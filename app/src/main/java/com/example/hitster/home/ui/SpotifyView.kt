package com.example.hitster.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitster.R
import com.example.hitster.home.model.SpotifyConnected
import com.example.hitster.home.model.SpotifyDisconnected
import com.example.hitster.home.model.SpotifyItem
import com.example.hitster.home.model.SpotifyLoading

@Composable
internal fun SpotifyView(spotifyItem: SpotifyItem) {
    Card(
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.spotify_icon),
                contentDescription = "Spotify"
            )
            Text(text = stringResource(spotifyItem.title), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    MaterialTheme {
        Column {
            SpotifyView(SpotifyLoading)
            SpotifyView(SpotifyDisconnected)
            SpotifyView(SpotifyConnected)
        }
    }
}