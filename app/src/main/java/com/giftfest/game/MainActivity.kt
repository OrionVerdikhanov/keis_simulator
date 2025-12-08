package com.giftfest.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giftfest.game.ui.screen.GameScreen
import com.giftfest.game.ui.theme.BackgroundDark
import com.giftfest.game.ui.theme.GiftFestTheme
import com.giftfest.game.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GiftFestApplication

        setContent {
            GiftFestTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = BackgroundDark
                ) {
                    val viewModel: GameViewModel = viewModel(
                        factory = GameViewModel.Factory(app.database)
                    )
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}
