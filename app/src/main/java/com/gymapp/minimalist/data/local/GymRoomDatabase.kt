package com.gymapp.minimalist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymapp.minimalist.data.local.dao.ExerciseDao
import com.gymapp.minimalist.data.local.dao.WorkoutPlanDao
import com.gymapp.minimalist.data.local.dao.WorkoutSessionDao
import com.gymapp.minimalist.data.local.entity.ExerciseEntity
import com.gymapp.minimalist.data.local.entity.WorkoutPlanEntity
import com.gymapp.minimalist.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutPlanEntity::class,
        WorkoutSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GymRoomDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        const val DATABASE_NAME = "gym_room_workout.db"

        @Volatile
        private var INSTANCE: GymRoomDatabase? = null

        fun getInstance(context: Context): GymRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymRoomDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Popula os 5 planos padrões (A, B, C, D, E)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialPlans(database.workoutPlanDao())
                                    migrateLegacySqliteIfPresent(context, database)
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateInitialPlans(database.workoutPlanDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialPlans(dao: WorkoutPlanDao) {
            val existing = dao.getAllPlans()
            val existingLetters = existing.map { it.letter }.toSet()
            val missing = listOf("A", "B", "C", "D", "E").filter { it !in existingLetters }
            if (missing.isNotEmpty()) {
                val newPlans = missing.map { letter ->
                    WorkoutPlanEntity(
                        letter = letter,
                        customName = "",
                        updatedAt = System.currentTimeMillis()
                    )
                }
                dao.insertAll(newPlans)
            }
        }

        private suspend fun migrateLegacySqliteIfPresent(context: Context, database: GymRoomDatabase) {
            try {
                val legacyDbFile = context.getDatabasePath("gym_workout.db")
                if (legacyDbFile.exists()) {
                    val legacyDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                        legacyDbFile.path,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    )
                    val cursor = legacyDb.rawQuery(
                        "SELECT workout_letter, name, target_sets, completed_sets, weight_kg, is_completed, created_at FROM exercises",
                        null
                    )
                    val migratedList = mutableListOf<ExerciseEntity>()
                    var order = 0
                    while (cursor.moveToNext()) {
                        migratedList.add(
                            ExerciseEntity(
                                workoutLetter = cursor.getString(0),
                                name = cursor.getString(1),
                                targetSets = cursor.getInt(2),
                                completedSets = cursor.getInt(3),
                                weightKg = cursor.getDouble(4),
                                isCompleted = cursor.getInt(5) == 1,
                                orderIndex = order++,
                                createdAt = cursor.getLong(6)
                            )
                        )
                    }
                    cursor.close()
                    legacyDb.close()
                    if (migratedList.isNotEmpty()) {
                        database.exerciseDao().insertAll(migratedList)
                    }
                }
            } catch (_: Exception) {
                // Silencioso em caso de arquivo legado não compatível
            }
        }
    }
}
