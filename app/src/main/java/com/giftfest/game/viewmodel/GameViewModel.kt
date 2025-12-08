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

    init {
        initializeGame()
        observeGameState()
        startEnergyRegeneration()
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
                        energy = state.energy,
                        maxEnergy = state.maxEnergy,
                        board = state.board,
                        unlockedGifts = state.unlockedGifts,
                        totalMerges = state.totalMerges,
                        highestGiftLevel = state.highestGiftLevel,
                        dailyStreak = state.dailyStreak,
                        lastEnergyRegenTime = state.lastEnergyRegenTime,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun startEnergyRegeneration() {
        energyRegenJob?.cancel()
        energyRegenJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Check every second
                val energyGained = gameUseCase.regenerateEnergy()
                if (energyGained > 0) {
                    _events.emit(GameEvent.EnergyRegenerated(energyGained))
                }
                updateEnergyTimer()
            }
        }
    }

    private fun updateEnergyTimer() {
        val state = _uiState.value
        if (state.energy < state.maxEnergy) {
            val now = System.currentTimeMillis()
            val timeSinceRegen = now - state.lastEnergyRegenTime
            val timeToNextEnergy = GameState.ENERGY_REGEN_TIME_MS - timeSinceRegen
            _uiState.update { it.copy(timeToNextEnergy = timeToNextEnergy.coerceAtLeast(0)) }
        } else {
            _uiState.update { it.copy(timeToNextEnergy = 0) }
        }
    }

    fun spawnGift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            when (val result = gameUseCase.spawnGift()) {
                is SpawnResult.Success -> {
                    _events.emit(GameEvent.GiftSpawned(result.cellIndex))
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

        // If cell is locked, don't do anything
        if (cell.isLocked) {
            viewModelScope.launch {
                val requirement = cell.unlockRequirement
                val message = when (requirement) {
                    is UnlockRequirement.Level -> "Reach level ${requirement.requiredLevel} to unlock"
                    is UnlockRequirement.InviteFriends -> "Invite ${requirement.count} friends to unlock"
                    is UnlockRequirement.Coins -> "Need ${requirement.amount} coins to unlock"
                    null -> "This cell is locked"
                }
                _events.emit(GameEvent.ShowMessage(message))
            }
            return
        }

        val selectedIndex = currentState.selectedCellIndex

        if (selectedIndex == null) {
            // No cell selected - select this one if it has a gift
            if (cell.gift != null) {
                _uiState.update { it.copy(selectedCellIndex = index) }
            }
        } else if (selectedIndex == index) {
            // Same cell - deselect
            _uiState.update { it.copy(selectedCellIndex = null) }
        } else {
            // Different cell selected - try to merge or move
            val selectedCell = board.getOrNull(selectedIndex)
            val selectedGift = selectedCell?.gift
            val targetGift = cell.gift

            if (selectedGift != null) {
                when {
                    // Target is empty - move the gift
                    targetGift == null -> {
                        viewModelScope.launch {
                            val success = gameUseCase.moveGift(selectedIndex, index)
                            if (success) {
                                _events.emit(GameEvent.GiftMoved(selectedIndex, index))
                            }
                            _uiState.update { it.copy(selectedCellIndex = null) }
                        }
                    }
                    // Same gift type and level - merge
                    targetGift.typeIndex == selectedGift.typeIndex &&
                            targetGift.level == selectedGift.level -> {
                        performMerge(selectedIndex, index)
                    }
                    // Different gifts - select the new one
                    else -> {
                        _uiState.update { it.copy(selectedCellIndex = index) }
                    }
                }
            } else {
                // Selected cell was empty, select new cell if it has a gift
                if (targetGift != null) {
                    _uiState.update { it.copy(selectedCellIndex = index) }
                } else {
                    _uiState.update { it.copy(selectedCellIndex = null) }
                }
            }
        }
    }

    private fun performMerge(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, selectedCellIndex = null) }

            when (val result = gameUseCase.mergeGifts(fromIndex, toIndex)) {
                is MergeResult.Success -> {
                    _events.emit(
                        GameEvent.MergeSuccess(
                            newLevel = result.newLevel,
                            expGained = result.expGained,
                            coinsGained = result.coinsGained
                        )
                    )
                    if (result.leveledUp) {
                        _events.emit(GameEvent.LevelUp(result.newPlayerLevel))
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

    fun claimDailyReward() {
        viewModelScope.launch {
            when (val result = gameUseCase.claimDailyReward()) {
                is DailyRewardResult.Success -> {
                    _events.emit(
                        GameEvent.DailyRewardClaimed(
                            streak = result.streak,
                            coins = result.coinsEarned,
                            energy = result.energyEarned
                        )
                    )
                }
                is DailyRewardResult.AlreadyClaimed -> {
                    _events.emit(GameEvent.ShowMessage("Daily reward already claimed!"))
                }
                is DailyRewardResult.Error -> {
                    _events.emit(GameEvent.ShowMessage("Error claiming reward"))
                }
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCellIndex = null) }
    }

    override fun onCleared() {
        super.onCleared()
        energyRegenJob?.cancel()
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

data class GameUiState(
    val playerLevel: Int = 1,
    val experience: Long = 0,
    val experienceToNextLevel: Long = 100,
    val coins: Long = 0,
    val energy: Int = 40,
    val maxEnergy: Int = 40,
    val timeToNextEnergy: Long = 0,
    val board: List<BoardCell> = emptyList(),
    val selectedCellIndex: Int? = null,
    val unlockedGifts: Set<Int> = emptySet(),
    val totalMerges: Int = 0,
    val highestGiftLevel: Int = 1,
    val dailyStreak: Int = 0,
    val lastEnergyRegenTime: Long = 0,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false
)

sealed class GameEvent {
    data class ShowMessage(val message: String) : GameEvent()
    data class GiftSpawned(val cellIndex: Int) : GameEvent()
    data class GiftMoved(val fromIndex: Int, val toIndex: Int) : GameEvent()
    data class MergeSuccess(val newLevel: Int, val expGained: Long, val coinsGained: Long) : GameEvent()
    data class LevelUp(val newLevel: Int) : GameEvent()
    data class DailyRewardClaimed(val streak: Int, val coins: Long, val energy: Int) : GameEvent()
    data class EnergyRegenerated(val amount: Int) : GameEvent()
}
