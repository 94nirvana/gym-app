package com.gymapp.minimalist.data

import androidx.room.withTransaction
import com.gymapp.minimalist.data.local.GymRoomDatabase
import com.gymapp.minimalist.data.local.entity.ExerciseEntity
import com.gymapp.minimalist.data.local.entity.WorkoutPlanEntity
import com.gymapp.minimalist.data.local.entity.WorkoutSessionEntity
import com.gymapp.minimalist.data.security.CryptoUtils
import com.gymapp.minimalist.domain.BackupPayload
import com.gymapp.minimalist.domain.Exercise
import com.gymapp.minimalist.domain.WorkoutPlan
import com.gymapp.minimalist.domain.WorkoutRepository
import com.gymapp.minimalist.domain.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RoomWorkoutRepository(
    private val database: GymRoomDatabase
) : WorkoutRepository {

    private val exerciseDao = database.exerciseDao()
    private val workoutPlanDao = database.workoutPlanDao()
    private val workoutSessionDao = database.workoutSessionDao()

    override suspend fun getExercisesForWorkout(workoutLetter: String): List<Exercise> =
        withContext(Dispatchers.IO) {
            exerciseDao.getExercisesForWorkout(workoutLetter).map { it.toDomain() }
        }

    override suspend fun insertExercise(exercise: Exercise): Long = withContext(Dispatchers.IO) {
        val maxOrder = exerciseDao.getMaxOrderIndex(exercise.workoutLetter) ?: -1
        val entity = ExerciseEntity.fromDomain(exercise.copy(orderIndex = maxOrder + 1))
        exerciseDao.insertExercise(entity)
    }

    override suspend fun updateExercise(exercise: Exercise): Boolean = withContext(Dispatchers.IO) {
        val count = exerciseDao.updateExercise(ExerciseEntity.fromDomain(exercise))
        count > 0
    }

    override suspend fun deleteExercise(exerciseId: Long): Boolean = withContext(Dispatchers.IO) {
        val count = exerciseDao.deleteExerciseById(exerciseId)
        count > 0
    }

    override suspend fun toggleCompleted(exerciseId: Long, isCompleted: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val count = exerciseDao.updateCompletedStatus(exerciseId, isCompleted)
            count > 0
        }

    override suspend fun incrementCompletedSets(exerciseId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val entity = exerciseDao.getExerciseById(exerciseId) ?: return@withContext false
            val newCompleted = entity.completedSets + 1
            val isNowCompleted = newCompleted >= entity.targetSets
            val count = exerciseDao.updateSetsAndCompletion(exerciseId, newCompleted, isNowCompleted)
            count > 0
        }

    override suspend fun decrementCompletedSets(exerciseId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val entity = exerciseDao.getExerciseById(exerciseId) ?: return@withContext false
            val newCompleted = (entity.completedSets - 1).coerceAtLeast(0)
            val isNowCompleted = newCompleted >= entity.targetSets
            val count = exerciseDao.updateSetsAndCompletion(exerciseId, newCompleted, isNowCompleted)
            count > 0
        }

    override suspend fun updateWeight(exerciseId: Long, newWeight: Double): Boolean =
        withContext(Dispatchers.IO) {
            val validWeight = newWeight.coerceAtLeast(0.0)
            val count = exerciseDao.updateWeight(exerciseId, validWeight)
            count > 0
        }

    override suspend fun reorderExercises(
        workoutLetter: String,
        exerciseIdsInOrder: List<Long>
    ): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            exerciseIdsInOrder.forEachIndexed { index, id ->
                exerciseDao.updateOrderIndex(id, index)
            }
        }
        true
    }

    override suspend fun getWorkoutPlan(letter: String): WorkoutPlan = withContext(Dispatchers.IO) {
        val entity = workoutPlanDao.getPlanByLetter(letter)
        entity?.toDomain() ?: WorkoutPlan(letter = letter, customName = "")
    }

    override suspend fun getAllWorkoutPlans(): List<WorkoutPlan> = withContext(Dispatchers.IO) {
        workoutPlanDao.getAllPlans().map { it.toDomain() }
    }

    override suspend fun updateWorkoutPlan(plan: WorkoutPlan): Boolean =
        withContext(Dispatchers.IO) {
            val sanitized = plan.copy(
                customName = CryptoUtils.sanitizeString(plan.customName, 50),
                updatedAt = System.currentTimeMillis()
            )
            workoutPlanDao.insertOrUpdate(WorkoutPlanEntity.fromDomain(sanitized))
            true
        }

    override suspend fun completeAndResetWorkout(workoutLetter: String): WorkoutSession? =
        withContext(Dispatchers.IO) {
            val exercises = exerciseDao.getExercisesForWorkout(workoutLetter)
            if (exercises.isEmpty()) return@withContext null

            val plan = workoutPlanDao.getPlanByLetter(workoutLetter)
            val planName = plan?.customName?.ifBlank { null } ?: "Treino $workoutLetter"

            var totalSets = 0
            var totalVolume = 0.0
            var maxEstimated1RM = 0.0

            for (e in exercises) {
                val setsToCount = if (e.completedSets > 0) e.completedSets else if (e.isCompleted) e.targetSets else 0
                totalSets += setsToCount
                totalVolume += (setsToCount * e.weightKg)

                // Estimativa de 1RM baseada na carga mais alta com média estimada de 8 reps
                if (e.weightKg > 0) {
                    val estimated = WorkoutSession.calculateEpleyOneRepMax(e.weightKg, 8)
                    if (estimated > maxEstimated1RM) {
                        maxEstimated1RM = estimated
                    }
                }
            }

            val session = WorkoutSession(
                workoutLetter = workoutLetter,
                workoutName = planName,
                completedAt = System.currentTimeMillis(),
                totalExercises = exercises.size,
                totalCompletedSets = totalSets,
                totalVolumeKg = totalVolume,
                estimatedOneRepMaxKg = maxEstimated1RM
            )

            database.withTransaction {
                workoutSessionDao.insertSession(WorkoutSessionEntity.fromDomain(session))
                exerciseDao.resetWorkoutCounters(workoutLetter)
            }

            session
        }

    override suspend fun resetWorkoutCounters(workoutLetter: String): Boolean =
        withContext(Dispatchers.IO) {
            exerciseDao.resetWorkoutCounters(workoutLetter)
            true
        }

    override suspend fun getAllWorkoutSessions(): List<WorkoutSession> =
        withContext(Dispatchers.IO) {
            workoutSessionDao.getAllSessions().map { it.toDomain() }
        }

    override suspend fun getSessionsForWorkout(workoutLetter: String): List<WorkoutSession> =
        withContext(Dispatchers.IO) {
            workoutSessionDao.getSessionsByWorkout(workoutLetter).map { it.toDomain() }
        }

    override suspend fun clearWorkoutSessions(): Boolean = withContext(Dispatchers.IO) {
        workoutSessionDao.clearAll()
        true
    }

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val plans = workoutPlanDao.getAllPlans().map { it.toDomain() }
        val exercises = exerciseDao.getAllExercises().map { it.toDomain() }
        val sessions = workoutSessionDao.getAllSessions().map { it.toDomain() }

        val rootJson = JSONObject()
        rootJson.put("schemaVersion", BackupPayload.CURRENT_SCHEMA_VERSION)
        rootJson.put("exportedAt", System.currentTimeMillis())

        val plansArray = JSONArray()
        for (p in plans) {
            val obj = JSONObject()
            obj.put("letter", p.letter)
            obj.put("customName", p.customName)
            obj.put("updatedAt", p.updatedAt)
            plansArray.put(obj)
        }
        rootJson.put("plans", plansArray)

        val exercisesArray = JSONArray()
        for (e in exercises) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("workoutLetter", e.workoutLetter)
            obj.put("name", e.name)
            obj.put("targetSets", e.targetSets)
            obj.put("completedSets", e.completedSets)
            obj.put("weightKg", e.weightKg)
            obj.put("isCompleted", e.isCompleted)
            obj.put("orderIndex", e.orderIndex)
            obj.put("createdAt", e.createdAt)
            exercisesArray.put(obj)
        }
        rootJson.put("exercises", exercisesArray)

        val sessionsArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("workoutLetter", s.workoutLetter)
            obj.put("workoutName", s.workoutName)
            obj.put("completedAt", s.completedAt)
            obj.put("totalExercises", s.totalExercises)
            obj.put("totalCompletedSets", s.totalCompletedSets)
            obj.put("totalVolumeKg", s.totalVolumeKg)
            obj.put("estimatedOneRepMaxKg", s.estimatedOneRepMaxKg)
            sessionsArray.put(obj)
        }
        rootJson.put("sessions", sessionsArray)

        // Calcula checksum SHA-256 do conteúdo dos dados para garantir integridade
        val contentPayload = "$plansArray$exercisesArray$sessionsArray"
        val checksum = CryptoUtils.computeSha256(contentPayload)
        rootJson.put("checksum", checksum)

        rootJson.toString(2)
    }

    override suspend fun importBackupJson(jsonString: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                if (jsonString.length > BackupPayload.MAX_JSON_SIZE_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Payload de backup excede o limite seguro de 2MB")
                    )
                }

                val rootJson = JSONObject(jsonString)
                val schemaVersion = rootJson.optInt("schemaVersion", 1)
                if (schemaVersion > BackupPayload.CURRENT_SCHEMA_VERSION) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Versão de schema de backup não suportada: $schemaVersion")
                    )
                }

                val expectedChecksum = rootJson.optString("checksum", "")
                val plansArray = rootJson.optJSONArray("plans") ?: JSONArray()
                val exercisesArray = rootJson.optJSONArray("exercises") ?: JSONArray()
                val sessionsArray = rootJson.optJSONArray("sessions") ?: JSONArray()

                // Validação de integridade caso o checksum tenha sido fornecido
                if (expectedChecksum.isNotBlank()) {
                    val contentPayload = "$plansArray$exercisesArray$sessionsArray"
                    val calculatedChecksum = CryptoUtils.computeSha256(contentPayload)
                    if (!expectedChecksum.equals(calculatedChecksum, ignoreCase = true)) {
                        return@withContext Result.failure(
                            SecurityException("Falha de integridade: Checksum SHA-256 do backup inválido ou adulterado")
                        )
                    }
                }

                val parsedPlans = mutableListOf<WorkoutPlanEntity>()
                for (i in 0 until plansArray.length()) {
                    val item = plansArray.getJSONObject(i)
                    val letter = item.getString("letter").trim().uppercase()
                    if (letter in listOf("A", "B", "C", "D", "E")) {
                        parsedPlans.add(
                            WorkoutPlanEntity(
                                letter = letter,
                                customName = CryptoUtils.sanitizeString(item.optString("customName", ""), 50),
                                updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val parsedExercises = mutableListOf<ExerciseEntity>()
                for (i in 0 until exercisesArray.length()) {
                    val item = exercisesArray.getJSONObject(i)
                    val letter = item.getString("workoutLetter").trim().uppercase()
                    val rawName = item.getString("name")
                    val sanitizedName = CryptoUtils.sanitizeString(rawName, 100)

                    if (letter in listOf("A", "B", "C", "D", "E") && sanitizedName.isNotBlank()) {
                        parsedExercises.add(
                            ExerciseEntity(
                                id = 0L, // autoincrement limpo
                                workoutLetter = letter,
                                name = sanitizedName,
                                targetSets = item.optInt("targetSets", 3).coerceIn(1, 20),
                                completedSets = item.optInt("completedSets", 0).coerceIn(0, 50),
                                weightKg = item.optDouble("weightKg", 0.0).coerceIn(0.0, 1000.0),
                                isCompleted = item.optBoolean("isCompleted", false),
                                orderIndex = item.optInt("orderIndex", i),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val parsedSessions = mutableListOf<WorkoutSessionEntity>()
                for (i in 0 until sessionsArray.length()) {
                    val item = sessionsArray.getJSONObject(i)
                    val letter = item.optString("workoutLetter", "A").trim().uppercase()
                    if (letter in listOf("A", "B", "C", "D", "E")) {
                        parsedSessions.add(
                            WorkoutSessionEntity(
                                id = 0L,
                                workoutLetter = letter,
                                workoutName = CryptoUtils.sanitizeString(item.optString("workoutName", "Treino $letter"), 50),
                                completedAt = item.optLong("completedAt", System.currentTimeMillis()),
                                totalExercises = item.optInt("totalExercises", 0).coerceAtLeast(0),
                                totalCompletedSets = item.optInt("totalCompletedSets", 0).coerceAtLeast(0),
                                totalVolumeKg = item.optDouble("totalVolumeKg", 0.0).coerceAtLeast(0.0),
                                estimatedOneRepMaxKg = item.optDouble("estimatedOneRepMaxKg", 0.0).coerceAtLeast(0.0)
                            )
                        )
                    }
                }

                // Importação atômica
                database.withTransaction {
                    if (parsedPlans.isNotEmpty()) {
                        workoutPlanDao.insertAll(parsedPlans)
                    }
                    if (parsedExercises.isNotEmpty()) {
                        exerciseDao.clearAll()
                        exerciseDao.insertAll(parsedExercises)
                    }
                    if (parsedSessions.isNotEmpty()) {
                        workoutSessionDao.clearAll()
                        workoutSessionDao.insertAll(parsedSessions)
                    }
                }

                val totalImported = parsedExercises.size + parsedSessions.size
                Result.success(totalImported)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
