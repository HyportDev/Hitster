package com.example.hitster.game.view

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R

@Composable
fun GuessCard(onClickSubmit: () -> Unit, onClickLeft: (() -> Unit)?, onClickRight: (() -> Unit)?) {
    Card(
        modifier = Modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors().copy(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            IconButton(
                modifier = Modifier.fillMaxSize(0.5f),
                onClick = onClickSubmit
            ) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_question_mark),
                    tint = Color.White,
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
        GuessCard({}, {}, {})
    }
}