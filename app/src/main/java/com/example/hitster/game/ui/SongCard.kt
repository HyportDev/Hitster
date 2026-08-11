package com.example.hitster.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitster.ui.HitsterTheme
import com.example.hitster.ui.SongYearStyle

/** Used by cards that never got a random color, i.e. the starting card of a timeline. */
internal val DefaultSongCardColor = Color(0xFFE3DDEA)

@Composable
internal fun SongCard(
    modifier: Modifier = Modifier,
    title: String,
    artist: String,
    year: String,
    color: Color,
    validation: SongCardValidation? = null
) {
    val newModifier: Modifier = validation?.let {
        Modifier.background(radiantShimmer(it.color))
    } ?: Modifier

    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors().copy(containerColor = color),
        border = validation?.let { BorderStroke(3.dp, validation.color) }
    ) {
        Column(
            modifier = newModifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = artist,
                color = Color.Black,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            BasicText(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                text = year,
                style = SongYearStyle.copy(textAlign = TextAlign.Center),
                autoSize = TextAutoSize.StepBased(),
                maxLines = 1,
                softWrap = true,
                color = { Color.Black },
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                color = Color.Black,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class SongCardValidation(val color: Color) {
    VALID(Color(0xFF4ADE80)),
    INVALID(Color(0xFFFF5A5A));

    companion object {
        fun Boolean?.toSongCardValidation() =
            when (this) {
                true -> VALID
                false -> INVALID
                null -> null
            }
    }
}

@Composable
private fun radiantShimmer(color: Color): Brush {
    val shimmerColors = listOf(
        color.copy(alpha = 0.3f),
        color.copy(alpha = 0.5f),
        color.copy(alpha = 1.0f),
        color.copy(alpha = 0.5f),
        color.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "")

    val translateAnimation = transition.animateFloat(
        initialValue = 10f,
        targetValue = (1000 + 500).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Shimmer loading animation",
    )
    return Brush.radialGradient(
        colors = shimmerColors,
        radius = translateAnimation.value
    )
}

@Preview
@Composable
fun SongCardPreview() {
    HitsterTheme {
        SongCard(
            title = "Alles nur geklaut",
            artist = "Die Prinzen",
            year = "1993",
            color = DefaultSongCardColor
        )
    }
}
