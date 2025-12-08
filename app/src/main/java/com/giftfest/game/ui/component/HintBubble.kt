package com.giftfest.game.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giftfest.game.ui.theme.Primary
import com.giftfest.game.ui.theme.TextPrimary

@Composable
fun HintBubble(
    characterName: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Character avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎅",
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Speech bubble
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = Primary,
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = characterName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90EE90)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}
