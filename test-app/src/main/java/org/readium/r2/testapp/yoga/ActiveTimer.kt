package org.readium.r2.testapp.yoga

import org.readium.r2.testapp.data.model.YogaPractice
import java.util.UUID

data class ActiveTimer(
    val id: String = UUID.randomUUID().toString(),
    val practice: YogaPractice,
    val remainingSeconds: Long,
    val isRunning: Boolean = true,
    val startTime: Long = System.currentTimeMillis()
) {
    val elapsedSeconds: Long
        get() = practice.defaultDurationMinutes * 60L - remainingSeconds

    val progressPercent: Float
        get() = if (practice.defaultDurationMinutes * 60L > 0) {
            elapsedSeconds.toFloat() / (practice.defaultDurationMinutes * 60) * 100
        } else 0f
}