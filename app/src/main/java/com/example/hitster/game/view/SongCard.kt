package com.example.hitster.game.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SongCard(
    title: String,
    artist: String,
    year: String,
    color: Color,
    validation: SongCardValidation? = null
) {
    Card(
        modifier = Modifier.size(200.dp),
        colors = CardDefaults.cardColors().copy(containerColor = color),
        border = validation?.let { BorderStroke(2.dp, validation.color) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = artist,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = year,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class SongCardValidation(val color: Color) {
    VALID(Color(0xff4caf50)),
    INVALID(Color(0xffd32f2f));

    companion object {
        fun Boolean?.toSongCardValidation() =
            when (this) {
                true -> VALID
                false -> INVALID
                null -> null
            }
    }
}

@Preview
@Composable
fun SongCardPreview() {
    MaterialTheme {
        SongCard(
            title = "Alles nur geklaut",
            artist = "Die Prinzen",
            year = "1993",
            Color(0xffcfd8dc)
        )
    }
}