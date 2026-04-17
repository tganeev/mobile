package org.readium.r2.testapp.data.model

import androidx.annotation.DrawableRes

data class Module(
    val id: Int,
    val title: String,
    @DrawableRes val iconRes: Int,
    val isAvailable: Boolean = true
)