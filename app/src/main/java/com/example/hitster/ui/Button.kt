package com.example.hitster.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.hitster.ui.internal.ButtonContent

@Composable
fun Button(modifier: Modifier = Modifier, title: String, primary: Boolean, onClick: () -> Unit) {
    ButtonContent(
        modifier = modifier.fillMaxWidth(),
        buttonColor =
            if (primary) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary,
        shape = if (primary) RoundedCornerShape(percent = 25) else ButtonDefaults.shape,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = title,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal
        )
    }
}