package com.example.hitster.ui.internal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

@Composable
internal fun ButtonContent(
    modifier: Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.secondary,
    shape: Shape,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        modifier = modifier,
        content = content,
        // buttonColors() only replaces what it is given: without a content color the label keeps
        // the default onPrimary and turns invisible on any other container.
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = contentColorFor(buttonColor).takeOrElse { LocalContentColor.current }
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        onClick = onClick,
        shape = shape
    )
}