package com.example.hitster.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Loud and tight where it counts, plain everywhere else. Only bundled families are used, so nothing
 * has to be downloaded at runtime.
 */
private val Headline = FontFamily.SansSerif

internal val HitsterTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp
        ),
        displayMedium = displayMedium.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp
        ),
        displaySmall = displaySmall.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        ),
        headlineLarge = headlineLarge.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp
        ),
        headlineMedium = headlineMedium.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp
        ),
        titleLarge = titleLarge.copy(
            fontFamily = Headline,
            fontWeight = FontWeight.ExtraBold
        ),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
}

/** The year on a song card, sized down by the card itself when the number does not fit. */
internal val SongYearStyle = TextStyle(
    fontFamily = Headline,
    fontSize = 48.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = (-2).sp
)
