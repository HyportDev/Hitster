package com.example.hitster.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Title(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.headlineSmall
    )
}
