package com.gymapp.minimalist.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.gymapp.minimalist.domain.Exercise

class GymDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "gym_track.db"
        const val DATABASE_VERSION = 1

        const val TABLE_EXERCISES = "exercises"
        const val COLUMN_ID = "id"
        const val COLUMN_WORKOUT_LETTER = "workout_letter"
        const val COLUMN_NAME = "name"
        const val COLUMN_TARGET_SETS = "target_sets"
        const val COLUMN_COMPLETED_SETS = "completed_sets"
        const val COLUMN_WEIGHT_KG = "weight_kg"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_EXERCISES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_WORKOUT_LETTER TEXT NOT NULL,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_TARGET_SETS INTEGER NOT NULL DEFAULT 3,
                $COLUMN_COMPLETED_SETS INTEGER NOT NULL DEFAULT 0,
                $COLUMN_WEIGHT_KG REAL NOT NULL DEFAULT 0.0,
                $COLUMN_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            );
        """.trimIndent()

        val createIndexQuery = """
            CREATE INDEX idx_workout_letter ON $TABLE_EXERCISES ($COLUMN_WORKOUT_LETTER);
        """.trimIndent()

        db.execSQL(createTableQuery)
        db.execSQL(createIndexQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy: in version 1 no upgrade needed.
    }
}
