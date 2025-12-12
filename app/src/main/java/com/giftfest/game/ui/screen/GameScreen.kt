package com.giftfest.game.ui.screen

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftfest.game.domain.model.GameState
import com.giftfest.game.ui.component.*
import com.giftfest.game.ui.theme.*
import com.giftfest.game.viewmodel.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vibrator = context.getSystemService<Vibrator>()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showMergePopup by remember { mutableStateOf(false) }
    var mergePopupData by remember { mutableStateOf(MergePopupData()) }
    var showOfflineRewardsDialog by remember { mutableStateOf(false) }
    var offlineRewardsData by remember { mutableStateOf(Triple(0L, 0, 0)) }
    var showSellDialog by remember { mutableStateOf(false) }
    var showSoldPopup by remember { mutableStateOf(false) }
    var soldPopupData by remember { mutableStateOf(SoldPopupData()) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GameEvent.ShowMessage -> snackbarMessage = event.message
                is GameEvent.GiftSpawned -> { }
                is GameEvent.MergeSuccess -> {
                    mergePopupData = MergePopupData(
                        expGained = event.expGained,
                        coinsGained = event.coinsGained,
                        gemsGained = event.gemsGained,
                        newLevel = event.newLevel,
                        combo = event.combo
                    )
                    showMergePopup = true
                }
                is GameEvent.LevelUp -> {
                    Toast.makeText(context, "🎉 Новый уровень! Теперь вы ${event.newLevel} уровня!", Toast.LENGTH_LONG).show()
                }
                is GameEvent.DailyRewardClaimed -> {
                    Toast.makeText(context, "День ${event.streak}! +${event.coins} монет, +${event.energy} энергии", Toast.LENGTH_LONG).show()
                }
                is GameEvent.FeverTriggered -> {
                    Toast.makeText(context, "🔥 РЕЖИМ ФУРИИ АКТИВИРОВАН!", Toast.LENGTH_SHORT).show()
                }
                is GameEvent.FeverEnded -> {
                    Toast.makeText(context, "Режим фурии завершён!", Toast.LENGTH_SHORT).show()
                }
                is GameEvent.ComboMilestone -> {
                    Toast.makeText(context, "🔥 ${event.combo}x КОМБО!", Toast.LENGTH_SHORT).show()
                }
                is GameEvent.OfflineRewards -> {
                    offlineRewardsData = Triple(event.coins, event.energy, event.minutes)
                    showOfflineRewardsDialog = true
                }
                is GameEvent.BoardFull -> {
                    showSellDialog = true
                }
                is GameEvent.GiftSold -> {
                    soldPopupData = SoldPopupData(event.coinsGained, event.giftLevel, event.spawnedNew)
                    showSoldPopup = true
                }
                is GameEvent.PrestigeComplete -> {
                    Toast.makeText(context, "♻️ Престиж ${event.newLevel}! +${event.pointsEarned} очков!", Toast.LENGTH_LONG).show()
                }
                is GameEvent.Haptic -> {
                    vibrator?.let { v ->
                        val duration = when (event.type) {
                            HapticType.LIGHT -> 20L
                            HapticType.MEDIUM -> 50L
                            HapticType.HEAVY -> 100L
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(duration)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, Color(0xFF051208))
                )
            )
    ) {
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.currentScreen == Screen.GAME -> MainGameContent(
                uiState = uiState,
                onCellClick = viewModel::selectCell,
                onCellLongClick = { index ->
                    val cell = uiState.board.getOrNull(index)
                    if (cell?.gift != null) {
                        showSellDialog = true
                    }
                },
                onSpawnClick = viewModel::spawnGift,
                onSellClick = { index ->
                    viewModel.sellGift(index)
                    showSellDialog = false
                },
                onNavigate = viewModel::setCurrentScreen,
                getSellPrice = viewModel::getSellPrice
            )
            uiState.currentScreen == Screen.SHOP -> ShopScreen(
                coins = uiState.coins,
                gems = uiState.gems,
                ownedBoosters = uiState.ownedBoosters,
                onPurchaseBooster = viewModel::purchaseBooster,
                onBack = { viewModel.setCurrentScreen(Screen.GAME) }
            )
            uiState.currentScreen == Screen.WHEEL -> WheelScreen(
                gems = uiState.gems,
                freeSpins = uiState.freeSpinsAvailable,
                isSpinning = uiState.isSpinning,
                onSpin = viewModel::spinLuckyWheel,
                onBack = { viewModel.setCurrentScreen(Screen.GAME) }
            )
            uiState.currentScreen == Screen.STATS -> StatsScreen(
                statistics = uiState.statistics,
                level = uiState.playerLevel,
                totalMerges = uiState.totalMerges,
                highestGiftLevel = uiState.highestGiftLevel,
                onBack = { viewModel.setCurrentScreen(Screen.GAME) }
            )
            uiState.currentScreen == Screen.PRESTIGE -> PrestigeScreen(
                playerLevel = uiState.playerLevel,
                prestigeLevel = uiState.prestigeLevel,
                prestigePoints = uiState.prestigePoints,
                bonuses = uiState.permanentBonuses,
                pointsPreview = viewModel.getPrestigePointsPreview(),
                canPrestige = viewModel.canPrestige(),
                onPrestige = viewModel::performPrestige,
                onPurchaseUpgrade = viewModel::purchasePrestigeUpgrade,
                onBack = { viewModel.setCurrentScreen(Screen.GAME) }
            )
        }

        GameSnackbar(
            message = snackbarMessage,
            onDismiss = { snackbarMessage = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = showMergePopup,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            MergeSuccessPopup(
                data = mergePopupData,
                onDismiss = { showMergePopup = false }
            )
        }

        AnimatedVisibility(
            visible = showSoldPopup,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SoldSuccessPopup(
                data = soldPopupData,
                onDismiss = { showSoldPopup = false }
            )
        }

        if (showOfflineRewardsDialog) {
            OfflineRewardsDialog(
                coins = offlineRewardsData.first,
                energy = offlineRewardsData.second,
                minutes = offlineRewardsData.third,
                onDismiss = { showOfflineRewardsDialog = false }
            )
        }

        if (showSellDialog && uiState.selectedCellIndex != null) {
            val selectedCell = uiState.board.getOrNull(uiState.selectedCellIndex!!)
            val gift = selectedCell?.gift
            if (gift != null) {
                SellGiftDialog(
                    giftLevel = gift.displayLevel,
                    sellPrice = viewModel.getSellPrice(uiState.selectedCellIndex!!),
                    onSell = {
                        viewModel.sellGift(uiState.selectedCellIndex!!)
                        showSellDialog = false
                    },
                    onDismiss = { showSellDialog = false }
                )
            }
        }

        if (showSellDialog && uiState.isBoardFull && uiState.selectedCellIndex == null) {
            BoardFullDialog(
                onDismiss = { showSellDialog = false }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentGold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка...", color = TextSecondary)
        }
    }
}

@Composable
private fun MainGameContent(
    uiState: GameUiState,
    onCellClick: (Int) -> Unit,
    onCellLongClick: (Int) -> Unit,
    onSpawnClick: () -> Unit,
    onSellClick: (Int) -> Unit,
    onNavigate: (Screen) -> Unit,
    getSellPrice: (Int) -> Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopBar(
            coins = uiState.coins,
            gems = uiState.gems,
            level = uiState.playerLevel,
            experience = uiState.experience,
            experienceToNextLevel = uiState.experienceToNextLevel,
            prestigeLevel = uiState.prestigeLevel,
            onShopClick = { onNavigate(Screen.SHOP) },
            onStatsClick = { onNavigate(Screen.STATS) },
            onPrestigeClick = { onNavigate(Screen.PRESTIGE) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = uiState.currentCombo > 0,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            ComboIndicator(
                combo = uiState.currentCombo,
                multiplier = uiState.comboMultiplier,
                timeLeft = uiState.comboTimeLeft,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FeverIndicator(
            progress = uiState.feverProgress,
            isActive = uiState.isFeverActive,
            timeLeft = uiState.feverTimeLeft
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = uiState.boosterTimers.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ActiveBoostersBar(boosters = uiState.boosterTimers)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            GameBoard(
                board = uiState.board,
                selectedCellIndex = uiState.selectedCellIndex,
                onCellClick = onCellClick,
                isFeverActive = uiState.isFeverActive,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Sell button when cell is selected
        AnimatedVisibility(
            visible = uiState.selectedCellIndex != null &&
                    uiState.board.getOrNull(uiState.selectedCellIndex!!)?.gift != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            uiState.selectedCellIndex?.let { index ->
                val sellPrice = getSellPrice(index)
                SellButton(
                    sellPrice = sellPrice,
                    onClick = { onSellClick(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        EnergyBar(
            energy = uiState.energy,
            maxEnergy = uiState.maxEnergy,
            timeToNextEnergy = uiState.timeToNextEnergy
        )

        Spacer(modifier = Modifier.height(12.dp))

        BottomButtons(
            canSpawn = uiState.energy >= GameState.SPAWN_ENERGY_COST && !uiState.isBoardFull,
            isBoardFull = uiState.isBoardFull,
            isProcessing = uiState.isProcessing,
            onSpawnClick = onSpawnClick,
            onWheelClick = { onNavigate(Screen.WHEEL) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TopBar(
    coins: Long,
    gems: Int,
    level: Int,
    experience: Long,
    experienceToNextLevel: Long,
    prestigeLevel: Int,
    onShopClick: () -> Unit,
    onStatsClick: () -> Unit,
    onPrestigeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevelBadge(level = level, prestigeLevel = prestigeLevel, onClick = onPrestigeClick)

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CurrencyBadge(
                    icon = "💰",
                    value = formatNumber(coins),
                    color = AccentGold,
                    onClick = onShopClick
                )
                CurrencyBadge(
                    icon = "💎",
                    value = "$gems",
                    color = AccentBlue,
                    onClick = onShopClick
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            ExperienceBar(
                experience = experience,
                experienceToNextLevel = experienceToNextLevel
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onStatsClick,
            modifier = Modifier
                .size(40.dp)
                .background(BackgroundCard, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "Статистика",
                tint = TextPrimary
            )
        }
    }
}

@Composable
private fun LevelBadge(level: Int, prestigeLevel: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "level")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (prestigeLevel > 0) AccentPurple.copy(alpha = glowAlpha * 0.4f)
                        else AccentGold.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (prestigeLevel > 0) {
                        Modifier.border(2.dp, AccentPurple, CircleShape)
                    } else Modifier
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = if (prestigeLevel > 0) {
                            listOf(AccentPurple, AccentGold)
                        } else {
                            listOf(AccentGold, AccentOrange)
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$level",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundDark
                )
                if (prestigeLevel > 0) {
                    Text(
                        text = "P$prestigeLevel",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "LVL",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = BackgroundDark.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyBadge(
    icon: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = BackgroundCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            tint = color,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ExperienceBar(
    experience: Long,
    experienceToNextLevel: Long
) {
    val progress = (experience.toFloat() / experienceToNextLevel.toFloat()).coerceIn(0f, 1f)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BackgroundCell)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ExpBar, Color(0xFF81C784))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$experience / $experienceToNextLevel XP",
            fontSize = 9.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun SellButton(
    sellPrice: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sell,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ПРОДАТЬ +$sellPrice",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " 💰",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BottomButtons(
    canSpawn: Boolean,
    isBoardFull: Boolean,
    isProcessing: Boolean,
    onSpawnClick: () -> Unit,
    onWheelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WheelButton(
            onClick = onWheelClick,
            modifier = Modifier.weight(0.3f)
        )

        SpawnButton(
            enabled = canSpawn,
            isBoardFull = isBoardFull,
            isProcessing = isProcessing,
            onClick = onSpawnClick,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun WheelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPurple
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "🎡",
            fontSize = 24.sp
        )
    }
}

@Composable
private fun SpawnButton(
    enabled: Boolean,
    isBoardFull: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isProcessing) 1f else 0.95f,
        label = "scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isProcessing,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isBoardFull) AccentOrange else ButtonPrimary,
            disabledContainerColor = ButtonDisabled
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else if (isBoardFull) {
            Text(
                text = "📦 ПОЛЕ ПОЛНОЕ - ПРОДАЙТЕ ПОДАРОК",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "🎁 ПОЛУЧИТЬ ПОДАРОК",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${GameState.SPAWN_ENERGY_COST}⚡)",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun MergeSuccessPopup(
    data: MergePopupData,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onDismiss()
    }

    Card(
        modifier = Modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (data.combo > 1) "🔥 ${data.combo}x КОМБО!" else "✨ СОЕДИНЕНО!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (data.combo > 1) AccentOrange else AccentGold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (data.expGained > 0) {
                    RewardItem(icon = "📚", value = "+${data.expGained}", color = ExpBar)
                }
                if (data.coinsGained > 0) {
                    RewardItem(icon = "💰", value = "+${data.coinsGained}", color = AccentGold)
                }
                if (data.gemsGained > 0) {
                    RewardItem(icon = "💎", value = "+${data.gemsGained}", color = AccentBlue)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Подарок ${data.newLevel} уровня!",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SoldSuccessPopup(
    data: SoldPopupData,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        onDismiss()
    }

    Card(
        modifier = Modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💸 ПРОДАНО!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AccentOrange
            )
            Spacer(modifier = Modifier.height(12.dp))
            RewardItem(icon = "💰", value = "+${data.coinsGained}", color = AccentGold)
            Spacer(modifier = Modifier.height(8.dp))
            if (data.spawnedNew) {
                Text(
                    text = "Появился новый подарок!",
                    fontSize = 13.sp,
                    color = AccentGreen
                )
            }
        }
    }
}

@Composable
private fun RewardItem(icon: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun OfflineRewardsDialog(
    coins: Long,
    energy: Int,
    minutes: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        title = {
            Text(
                text = "🌙 С возвращением!",
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
        },
        text = {
            Column {
                Text(
                    text = "Вы отсутствовали ${minutes / 60}ч ${minutes % 60}м",
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Собрано наград:", color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RewardItem(icon = "💰", value = "+$coins", color = AccentGold)
                    RewardItem(icon = "⚡", value = "+$energy", color = EnergyBar)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text("Забрать!")
            }
        }
    )
}

@Composable
private fun SellGiftDialog(
    giftLevel: Int,
    sellPrice: Long,
    onSell: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        title = {
            Text(
                text = "💰 Продать подарок?",
                fontWeight = FontWeight.Bold,
                color = AccentOrange
            )
        },
        text = {
            Column {
                Text(
                    text = "Продать подарок $giftLevel уровня?",
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Вы получите +$sellPrice монет",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "На его месте появится подарок 1 уровня",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSell,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Продать!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun BoardFullDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundCard,
        title = {
            Text(
                text = "📦 Поле заполнено!",
                fontWeight = FontWeight.Bold,
                color = AccentOrange
            )
        },
        text = {
            Column {
                Text(
                    text = "Ваше поле полностью заполнено!",
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Нажмите на подарок и используйте кнопку ПРОДАТЬ, чтобы освободить место.",
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text("Понятно!")
            }
        }
    )
}

data class MergePopupData(
    val expGained: Long = 0,
    val coinsGained: Long = 0,
    val gemsGained: Int = 0,
    val newLevel: Int = 0,
    val combo: Int = 1
)

data class SoldPopupData(
    val coinsGained: Long = 0,
    val giftLevel: Int = 0,
    val spawnedNew: Boolean = false
)

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
