package com.example.hitster.game.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitster.R

@Composable
fun GuessCard(onClickLeft: () -> Unit, onClickRight: () -> Unit) {
    Card(
        modifier = Modifier.size(200.dp),
        colors = CardDefaults.cardColors().copy(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "?",
                fontSize = 72.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            IconButton(
                modifier = Modifier.align(Alignment.BottomStart),
                onClick = onClickLeft
            ) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back), tint = Color.LightGray, contentDescription = "Move left")
            }
            IconButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = onClickRight
            ) {
                Icon(painter = painterResource(R.drawable.ic_arrow_forward), tint = Color.LightGray, contentDescription = "Move left")
            }
        }
    }
}

@Preview
@Composable
fun GuessCardPreview() {
    MaterialTheme {
        GuessCard({}, {})
    }
}