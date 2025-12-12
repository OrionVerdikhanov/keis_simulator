package com.giftfest.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.giftfest.game.data.GameDatabase
import com.giftfest.game.data.repository.GameRepository
import com.giftfest.game.domain.model.*
import com.giftfest.game.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameUseCase: GameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var energyRegenJob: Job? = null
    private var feverCheckJob: Job? = null
    private var comboCheckJob: Job? = null

    init {
        initializeGame()
        observeGameState()
        startBackgroundJobs()
        checkOfflineRewards()
    }

    private fun initializeGame() {
        viewModelScope.launch {
            gameUseCase.initializeGame()
        }
    }

    private fun observeGameState() {
        viewModelScope.launch {
            gameUseCase.getGameState().collect { state ->
                _uiState.update { currentState ->
                    currentState.copy(
                        playerLevel = state.playerLevel,
                        experience = state.experience,
                        experienceToNextLevel = state.experienceToNextLevel,
                        coins = state.coins,
                        gems = state.gems,
                        energy = state.energy,
                        maxEnergy = state.maxEnergy,
                        board = state.board,
                        unlockedGifts = state.unlockedGifts,
                        totalMerges = state.totalMerges,
                        highestGiftLevel = state.highestGiftLevel,
                        dailyStreak = state.dailyStreak,
                        lastEnergyRegenTime = state.lastEnergyRegenTime,
                        currentCombo = state.currentCombo,
                        comboMultiplier = state.comboMultiplier,
                        feverProgress = state.feverProgress,
                        isFeverActive = state.isFeverActive,
                        feverEndTime = state.feverEndTime,
                        activeBoosters = state.activeBoosters,
                        ownedBoosters = state.ownedBoosters,
                        activeQuests = state.activeQuests,
                        statistics = state.statistics,
                        freeSpinsAvailable = state.freeSpinsAvailable,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun startBackgroundJobs() {
        // Energy regeneration
        energyRegenJob?.cancel()
        energyRegenJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val energyGained = gameUseCase.regenerateEnergy()
                if (energyGained > 0) {
                    _events.emit(GameEvent.EnergyRegenerated(energyGained))
                }
                updateTimers()
            }
        }

        // Fever mode check
        feverCheckJob?.cancel()
        feverCheckJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val state = _uiState.value
                if (state.isFeverActive && System.currentTimeMillis() > state.feverEndTime) {
                    _events.emit(GameEvent.FeverEnded)
                }
            }
        }

        // Combo timeout check
        comboCheckJob?.cancel()
        comboCheckJob = viewModelScope.launch {
            while (true) {
                delay(500)
                updateComboTimer()
            }
        }
    }

    private fun checkOfflineRewards() {
        viewModelScope.launch {
            val result = gameUseCase.calculateOfflineRewards()
            if (result.minutesOffline >= 5) {
                _events.emit(
                    GameEvent.OfflineRewards(
                        coins = result.coins,
                        energy = result.energy,
                        minutes = result.minutesOffline
                    )
                )
            }
        }
    }

    private fun updateTimers() {
        val state = _uiState.value
        val now = System.currentTimeMillis()

        // Energy timer
        if (state.energy < state.maxEnergy) {
            val timeSinceRegen = now - state.lastEnergyRegenTime
            val timeToNextEnergy = GameState.ENERGY_REGEN_TIME_MS - timeSinceRegen
            _uiState.update { it.copy(timeToNextEnergy = timeToNextEnergy.coerceAtLeast(0)) }
        } else {
            _uiState.update { it.copy(timeToNextEnergy = 0) }
        }

        // Fever timer
        if (state.isFeverActive) {
            val feverTimeLeft = (state.feverEndTime - now).coerceAtLeast(0)
            _uiState.update { it.copy(feverTimeLeft = feverTimeLeft) }
        }

        // Booster timers
        val activeBoostersWithTime = state.activeBoosters.map { booster ->
            booster to (booster.expiresAt - now).coerceAtLeast(0)
        }
        _uiState.update { it.copy(boosterTimers = activeBoostersWithTime.toMap()) }
    }

    private fun updateComboTimer() {
        val state = _uiState.value
        if (state.currentCombo > 0) {
            val now = System.currentTimeMillis()
            val timeSinceLastMerge = now - state.lastMergeTime
            val comboTimeLeft = (GameState.COMBO_TIMEOUT_MS - timeSinceLastMerge).coerceAtLeast(0)
            _uiState.update { it.copy(comboTimeLeft = comboTimeLeft) }
        }
    }

    fun spawnGift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            when (val result = gameUseCase.spawnGift()) {
                is SpawnResult.Success -> {
                    _events.emit(
                        GameEvent.GiftSpawned(
                            cellIndex = result.cellIndex,
                            isSpecial = result.isSpecial,
                            specialType = result.specialType
                        )
                    )
                }
                is SpawnResult.NotEnoughEnergy -> {
                    _events.emit(GameEvent.ShowMessage("Not enough energy!"))
                }
                is SpawnResult.BoardFull -> {
                    _events.emit(GameEvent.ShowMessage("Board is full! Merge some gifts."))
                }
                is SpawnResult.Error -> {
                    _events.emit(GameEvent.ShowMessage(result.message))
                }
            }

            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun selectCell(index: Int) {
        val currentState = _uiState.value
        val board = currentState.board
        val cell = board.getOrNull(index) ?: return

        if (cell.isLocked) {
            viewModelScope.launch {
                val requirement = cell.unlockRequirement
                val message = when (requirement) {
                    is UnlockRequirement.Level -> "Reach level ${requirement.requiredLevel} to unlock"
                    is UnlockRequirement.InviteFriends -> "Invite ${requirement.count} friends to unlock"
                    is UnlockRequirement.Coins -> "Need ${requirement.amount} coins to unlock"
                    is UnlockRequirement.Gems -> "Need ${requirement.amount} gems to unlock"
                    is UnlockRequirement.Merges -> "Perform ${requirement.count} merges to unlock"
                    null -> "This cell is locked"
                }
                _events.emit(GameEvent.ShowMessage(message))
            }
            return
        }

        val selectedIndex = currentState.selectedCellIndex

        if (selectedIndex == null) {
            if (cell.gift != null) {
                _uiState.update { it.copy(selectedCellIndex = index) }
                // Vibrate on selection
                viewModelScope.launch { _events.emit(GameEvent.Haptic(HapticType.LIGHT)) }
            }
        } else if (selectedIndex == index) {
            // Double tap on special gift - use ability
            if (cell.gift?.isSpecial == true) {
                useSpecialGift(index)
            } else {
                _uiState.update { it.copy(selectedCellIndex = null) }
            }
        } else {
            val selectedCell = board.getOrNull(selectedIndex)
            val selectedGift = selectedCell?.gift
            val targetGift = cell.gift

            if (selectedGift != null) {
                when {
                    targetGift == null -> {
                        viewModelScope.launch {
                            val success = gameUseCase.moveGift(selectedIndex, index)
                            if (success) {
                                _events.emit(GameEvent.GiftMoved(selectedIndex, index))
                                _events.emit(GameEvent.Haptic(HapticType.LIGHT))
                            }
                            _uiState.update { it.copy(selectedCellIndex = null) }
                        }
                    }
                    canMerge(selectedGift, targetGift) -> {
                        performMerge(selectedIndex, index)
                    }
                    else -> {
                        _uiState.update { it.copy(selectedCellIndex = index) }
                    }
                }
            } else {
                if (targetGift != null) {
                    _uiState.update { it.copy(selectedCellIndex = index) }
                } else {
                    _uiState.update { it.copy(selectedCellIndex = null) }
                }
            }
        }
    }

    private fun canMerge(gift1: CellGift, gift2: CellGift): Boolean {
        // Rainbow can merge with anything
        if (gift1.specialType == SpecialGiftType.RAINBOW || gift2.specialType == SpecialGiftType.RAINBOW) {
            return true
        }
        return gift1.typeIndex == gift2.typeIndex && gift1.level == gift2.level
    }

    private fun performMerge(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedCellIndex = null) }

            when (val result = gameUseCase.mergeGifts(fromIndex, toIndex)) {
                is MergeResult.Success -> {
                    _events.emit(GameEvent.Haptic(HapticType.MEDIUM))
                    _events.emit(
                        GameEvent.MergeSuccess(
                            newLevel = result.newLevel,
                            expGained = result.expGained,
                            coinsGained = result.coinsGained,
                            gemsGained = result.gemsGained,
                            combo = result.combo
                        )
                    )

                    if (result.triggeredFever) {
                        _events.emit(GameEvent.FeverTriggered)
                        _events.emit(GameEvent.Haptic(HapticType.HEAVY))
                    }

                    if (result.leveledUp) {
                        _events.emit(GameEvent.LevelUp(result.newPlayerLevel))
                        _events.emit(GameEvent.Haptic(HapticType.HEAVY))
                    }

                    // High combo celebration
                    if (result.combo >= 5) {
                        _events.emit(GameEvent.ComboMilestone(result.combo))
                    }
                }
                is MergeResult.CannotMerge -> {
                    _events.emit(GameEvent.ShowMessage("Cannot merge these gifts"))
                }
                is MergeResult.MaxLevelReached -> {
                    _events.emit(GameEvent.ShowMessage("Maximum level reached!"))
                }
                is MergeResult.Error -> {
                    _events.emit(GameEvent.ShowMessage(result.message))
                }
            }

            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private fun useSpecialGift(cellIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedCellIndex = null) }

            when (val result = gameUseCase.useSpecialGift(cellIndex)) {
                is SpecialGiftResult.Success -> {
                    _events.emit(GameEvent.Haptic(HapticType.HEAVY))
                    _events.emit(
                        GameEvent.SpecialGiftUsed(
                            type = result.type,
                            coinsGained = result.coinsGained,
                            affectedCells = result.affectedCells
                        )
                    )
                }
                is SpecialGiftResult.NotSpecial -> {
                    _events.emit(GameEvent.ShowMessage("This is not a special gift"))
                }
                is SpecialGiftResult.Error -> {
                    _events.emit(GameEvent.ShowMessage(result.message))
                }
            }

            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun spinLuckyWheel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSpinning = true) }

            when (val result = gameUseCase.spinLuckyWheel()) {
                is WheelSpinResult.Success -> {
                    _events.emit(GameEvent.Haptic(HapticType.MEDIUM))
                    _events.emit(GameEvent.WheelPrizeWon(result.prize))
                }
                is WheelSpinResult.NotEnoughGems -> {
                    _events.emit(GameEvent.ShowMessage("Not enough gems! Need 10 gems."))
                }
                is WheelSpinResult.Error -> {
                    _events.emit(GameEvent.ShowMessage(result.message))
                }
            }

            delay(2000) // Animation time
            _uiState.update { it.copy(isSpinning = false) }
        }
    }

    fun purchaseBooster(type: BoosterType, useGems: Boolean) {
        viewModelScope.launch {
            when (val result = gameUseCase.purchaseBooster(type, useGems)) {
                is BoosterPurchaseResult.Success -> {
                    _events.emit(GameEvent.Haptic(HapticType.MEDIUM))
                    _events.emit(GameEvent.BoosterActivated(type))
                }
                is BoosterPurchaseResult.NotEnoughCoins -> {
                    _events.emit(GameEvent.ShowMessage("Not enough coins!"))
                }
                is BoosterPurchaseResult.NotEnoughGems -> {
                    _events.emit(GameEvent.ShowMessage("Not enough gems!"))
                }
                is BoosterPurchaseResult.Error -> {
                    _events.emit(GameEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            when (val result = gameUseCase.claimDailyReward()) {
                is DailyRewardResult.Success -> {
                    _events.emit(GameEvent.Haptic(HapticType.HEAVY))
                    _events.emit(
                        GameEvent.DailyRewardClaimed(
                            streak = result.streak,
                            coins = result.coinsEarned,
                            energy = result.energyEarned,
                            gems = result.gemsEarned
                        )
                    )
                }
                is DailyRewardResult.AlreadyClaimed -> {
                    _events.emit(GameEvent.ShowMessage("Come back tomorrow!"))
                }
                is DailyRewardResult.Error -> {
                    _events.emit(GameEvent.ShowMessage("Error claiming reward"))
                }
            }
        }
    }

    fun setCurrentScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCellIndex = null) }
    }

    override fun onCleared() {
        super.onCleared()
        energyRegenJob?.cancel()
        feverCheckJob?.cancel()
        comboCheckJob?.cancel()
    }

    class Factory(private val database: GameDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = GameRepository(database.playerDao(), database.boardDao())
            val useCase = GameUseCase(repository)
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(useCase) as T
        }
    }
}

enum class Screen {
    GAME, SHOP, WHEEL, STATS, QUESTS
}

enum class HapticType {
    LIGHT, MEDIUM, HEAVY
}

data class GameUiState(
    val playerLevel: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 100,
    val coins: Long = 0,
    val gems: Int = 0,
    val energy: Int = 50,
    val maxEnergy: Int = 50,
    val timeToNextEnergy: Long = 0,
    val board: List<BoardCell> = emptyList(),
    val selectedCellIndex: Int? = null,
    val unlockedGifts: Set<Int> = emptySet(),
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastEnergyRegenTime: Long = 0,
    val lastMergeTime: Long = 0,

    // Combo
    val currentCombo: Int = 0,
    val comboMultiplier: Float = 1.0f,
    val comboTimeLeft: Long = 0,

    // Fever
    val feverProgress: Float = 0f,
    val isFeverActive: Boolean = false,
    val feverEndTime: Long = 0,
    val feverTimeLeft: Long = 0,

    // Boosters
    val activeBoosters: List<ActiveBooster> = emptyList(),
    val ownedBoosters: Map<BoosterType, Int> = emptyMap(),
    val boosterTimers: Map<ActiveBooster, Long> = emptyMap(),

    // Quests
    val activeQuests: List<Quest> = emptyList(),

    // Statistics
    val statistics: PlayerStatistics = PlayerStatistics(),

    // Lucky wheel
    val freeSpinsAvailable: Int = 1,
    val isSpinning: Boolean = false,

    // UI State
    val currentScreen: Screen = Screen.GAME,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false
)

sealed class GameEvent {
    data class ShowMessage(val message: String) : GameEvent()
    data class GiftSpawned(val cellIndex: Int, val isSpecial: Boolean = false, val specialType: SpecialGiftType? = null) : GameEvent()
    data class GiftMoved(val fromIndex: Int, val toIndex: Int) : GameEvent()
    data class MergeSuccess(val newLevel: Int, val expGained: Long, val coinsGained: Long, val gemsGained: Int = 0, val combo: Int = 1) : GameEvent()
    data class LevelUp(val newLevel: Int) : GameEvent()
    data class DailyRewardClaimed(val streak: Int, val coins: Long, val energy: Int, val gems: Int = 0) : GameEvent()
    data class EnergyRegenerated(val amount: Int) : GameEvent()
    data class SpecialGiftUsed(val type: SpecialGiftType, val coinsGained: Long, val affectedCells: List<Int>) : GameEvent()
    data class ComboMilestone(val combo: Int) : GameEvent()
    data object FeverTriggered : GameEvent()
    data object FeverEnded : GameEvent()
    data class WheelPrizeWon(val prize: WheelPrize) : GameEvent()
    data class BoosterActivated(val type: BoosterType) : GameEvent()
    data class OfflineRewards(val coins: Long, val energy: Int, val minutes: Int) : GameEvent()
    data class Haptic(val type: HapticType) : GameEvent()
}
