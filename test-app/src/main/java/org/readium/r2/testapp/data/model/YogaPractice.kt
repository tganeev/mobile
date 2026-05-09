package org.readium.r2.testapp.data.model

import androidx.annotation.DrawableRes
import org.readium.r2.testapp.R


data class YogaPractice(
    val id: String,
    val name: String,
    val defaultDurationMinutes: Int,
    @DrawableRes val iconRes: Int,
    val color: String,
    val description: String
)

object YogaPractices {
    val practices = listOf(
        YogaPractice(
            id = "vipassana",
            name = "Випашьяна",
            defaultDurationMinutes = 20,
            iconRes = R.drawable.ic_yoga_default,
            color = "#9C27B0",
            description = "Практика осознанного наблюдения за дыханием и телесными ощущениями"
        ),
        YogaPractice(
            id = "kumbhaka",
            name = "Кумбхака",
            defaultDurationMinutes = 2,
            iconRes = R.drawable.ic_yoga_default,
            color = "#FF9800",
            description = "Задержка дыхания после вдоха или выдоха"
        ),
        YogaPractice(
            id = "visualization",
            name = "Визуализация",
            defaultDurationMinutes = 5,
            iconRes = R.drawable.ic_yoga_default,
            color = "#2196F3",
            description = "Практика создания мысленных образов"
        ),
        YogaPractice(
            id = "shamatha",
            name = "Шаматха",
            defaultDurationMinutes = 31,
            iconRes = R.drawable.ic_yoga_default,
            color = "#4CAF50",
            description = "Практика устойчивого внимания на одном объекте"
        ),
        YogaPractice(
            id = "concentration",
            name = "Концентрация",
            defaultDurationMinutes = 15,
            iconRes = R.drawable.ic_yoga_default,
            color = "#F44336",
            description = "Сосредоточение на точке или объекте"
        ),
        YogaPractice(
            id = "pranayama",
            name = "Пранаяма",
            defaultDurationMinutes = 10,
            iconRes = R.drawable.ic_yoga_default,
            color = "#00BCD4",
            description = "Управление дыханием"
        ),
        YogaPractice(
            id = "mantra",
            name = "Мантра",
            defaultDurationMinutes = 26,
            iconRes = R.drawable.ic_yoga_default,
            color = "#E91E63",
            description = "Повторение священных звуков"
        ),
        YogaPractice(
            id = "ekadash",
            name = "Экадаш",
            defaultDurationMinutes = 26,
            iconRes = R.drawable.ic_yoga_default,
            color = "#795548",
            description = "Одиннадцатидневный цикл практик"
        )
    )
}