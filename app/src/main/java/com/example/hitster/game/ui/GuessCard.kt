package com.example.hitster.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R

@Composable
fun GuessCard(
    modifier: Modifier = Modifier,
    onClickSubmit: () -> Unit,
    onClickLeft: (() -> Unit)?,
    onClickRight: (() -> Unit)?,
    isSubmitEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors().copy(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
            IconButton(
                modifier = Modifier.fillMaxSize(0.5f),
                enabled = isSubmitEnabled,
                onClick = onClickSubmit
            ) {
                Icon(
                    modifier = Modifier.fillMaxSize(0.75f).rotate(rotation),
                    painter = painterResource(R.drawable.ic_vinyl),
                    tint = if (isSubmitEnabled) Color.White else Color.DarkGray,
                    contentDescription = null
                )
            }
            onClickLeft?.let {
                IconButton(
                    modifier = Modifier.align(Alignment.BottomStart),
                    onClick = it
                ) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_back), tint = Color.LightGray, contentDescription = "Move left")
                }
            }
            onClickRight?.let {
                IconButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onClick = it
                ) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_forward), tint = Color.LightGray, contentDescription = "Move left")
                }
            }
        }
    }
}

@Preview
@Composable
fun GuessCardPreview() {
    MaterialTheme {
        GuessCard(Modifier, {}, {}, {})
    }
}