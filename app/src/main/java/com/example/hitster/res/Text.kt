package com.example.hitster.res

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed interface Text {
    data class Value(val value: String) : Text

    data class Resource(@StringRes val id: Int, val formatArgs: List<Any> = emptyList()) : Text {
        constructor(
            @StringRes id: Int,
            vararg  formatArgs: Any
        ) : this(id, formatArgs.toList())
    }

    fun getString(res: Resources): String = when (this) {
        is Value -> value
        is Resource -> when {
            formatArgs.isEmpty() -> res.getString(id)
            else -> res.getString(
                id,
                *formatArgs.map { (it as? Text)?.getString(res) ?: it }.toTypedArray()
            )
        }
    }

    @Composable
    fun getString() = getString(LocalContext.current.resources)
}