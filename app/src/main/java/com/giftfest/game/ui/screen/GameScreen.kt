package com.giftfest.game.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftfest.game.domain.model.GameState
import com.giftfest.game.ui.component.*
import com.giftfest.game.ui.theme.BackgroundDark
import com.giftfest.game.viewmodel.GameEvent
import com.giftfest.game.viewmodel.GameUiState
import com.giftfest.game.viewmodel.GameViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GameScreen(
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showMergePopup by remember { mutableStateOf(false) }
    var mergePopupData by remember { mutableStateOf(Triple(0L, 0L, 0)) }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GameEvent.ShowMessage -> {
                    snackbarMessage = event.message
                }
                is GameEvent.GiftSpawned -> {
                    // Animation could be added here
                }
                is GameEvent.MergeSuccess -> {
                    mergePopupData = Triple(event.expGained, event.coinsGained, event.newLevel)
                    showMergePopup = true
                }
                is GameEvent.LevelUp -> {
                    Toast.makeText(context, "Level Up! You are now level ${event.newLevel}!", Toast.LENGTH_LONG).show()
                }
                is GameEvent.DailyRewardClaimed -> {
                    Toast.makeText(
                        context,
                        "Day ${event.streak}! +${event.coins} coins, +${event.energy} energy",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is GameEvent.EnergyRegenerated -> {
                    // Silent regeneration
                }
                is GameEvent.GiftMoved -> {
                    // Animation could be added here
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            GameContent(
                uiState = uiState,
                onCellClick = viewModel::selectCell,
                onSpawnClick = viewModel::spawnGift
            )
        }

        // Snackbar overlay
        GameSnackbar(
            message = snackbarMessage,
            onDismiss = { snackbarMessage = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Merge success popup
        MergeSuccessPopup(
            expGained = mergePopupData.first,
            coinsGained = mergePopupData.second,
            newLevel = mergePopupData.third,
            visible = showMergePopup,
            onDismiss = { showMergePopup = false },
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onSpawnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // Title
        GameTitle()

        Spacer(modifier = Modifier.height(8.dp))

        // Player header with level, exp, coins
        PlayerHeader(
            level = uiState.playerLevel,
            experience = uiState.experience,
            experienceToNextLevel = uiState.experienceToNextLevel,
            coins = uiState.coins
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hint bubble
        HintBubble(
            characterName = "SANTA CLAUS",
            message = getHintMessage(uiState)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Game board
        GameBoard(
            board = uiState.board,
            selectedCellIndex = uiState.selectedCellIndex,
            onCellClick = onCellClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Energy bar
        EnergyBar(
            energy = uiState.energy,
            maxEnergy = uiState.maxEnergy,
            timeToNextEnergy = uiState.timeToNextEnergy
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Spawn button
        SpawnButton(
            enabled = uiState.energy >= GameState.SPAWN_ENERGY_COST,
            isProcessing = uiState.isProcessing,
            onClick = onSpawnClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats section
        StatsSection(
            totalMerges = uiState.totalMerges,
            highestGiftLevel = uiState.highestGiftLevel,
            unlockedGifts = uiState.unlockedGifts.size
        )
    }
}

@Composable
private fun StatsSection(
    totalMerges: Int,
    highestGiftLevel: Int,
    unlockedGifts: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Merges", value = "$totalMerges")
        StatItem(label = "Best Level", value = "$highestGiftLevel")
        StatItem(label = "Collection", value = "$unlockedGifts/12")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getHintMessage(state: GameUiState): String {
    return when {
        state.board.isEmpty() -> "Welcome! Tap GET GIFT to start collecting!"
        state.totalMerges == 0 -> "Tap a gift, then tap another same gift to merge them!"
        state.selectedCellIndex != null -> "Now tap another gift of the same type to merge!"
        state.energy < GameState.SPAWN_ENERGY_COST -> "Wait for energy to regenerate, or merge gifts!"
        state.board.count { it.gift != null } >= 10 -> "Board is getting full! Merge some gifts!"
        else -> "Keep merging gifts to unlock new types and level up!"
    }
}
