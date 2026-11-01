package com.gymapp.minimalist.domain

data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val plans: List<WorkoutPlan>,
    val exercises: List<Exercise>,
    val sessions: List<WorkoutSession>,
    val checksum: String = ""
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val MAX_JSON_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB limit to prevent DoS
    }
}
