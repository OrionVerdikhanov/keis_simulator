package com.giftfest.game.domain.model

/**
 * Represents a gift item in the game
 */
data class Gift(
    val id: Int,
    val type: GiftType,
    val level: Int,
    val isUnlocked: Boolean = false
) {
    companion object {
        const val MAX_LEVEL = 12
    }
}

/**
 * Different types of gifts available in the game
 */
enum class GiftType(
    val displayName: String,
    val baseEmoji: String,
    val rarity: GiftRarity
) {
    ICE_CASTLE("Ice Castle", "🏰", GiftRarity.COMMON),
    ORNAMENT("Ornament", "🎄", GiftRarity.COMMON),
    TROPHY("Trophy", "🏆", GiftRarity.UNCOMMON),
    CARNIVAL_MASK("Carnival Mask", "🎭", GiftRarity.UNCOMMON),
    GIFT_BOX("Gift Box", "🎁", GiftRarity.RARE),
    ROCKET("Rocket", "🚀", GiftRarity.RARE),
    CHAMPAGNE("Champagne", "🍾", GiftRarity.EPIC),
    DIAMOND("Diamond", "💎", GiftRarity.EPIC),
    FLYING_SQUIRREL("Flying Squirrel", "🐿️", GiftRarity.LEGENDARY),
    ROSE("Rose", "🌹", GiftRarity.LEGENDARY),
    TEDDY_BEAR("Teddy Bear", "🧸", GiftRarity.MYTHIC),
    MYSTERY("Mystery", "❓", GiftRarity.MYTHIC);

    companion object {
        fun fromLevel(level: Int): GiftType {
            return entries.getOrElse(level.coerceIn(0, entries.size - 1)) { ICE_CASTLE }
        }
    }
}

enum class GiftRarity(val color: Long, val expMultiplier: Float) {
    COMMON(0xFF9E9E9E, 1.0f),      // Gray
    UNCOMMON(0xFF4CAF50, 1.5f),    // Green
    RARE(0xFF2196F3, 2.0f),        // Blue
    EPIC(0xFF9C27B0, 3.0f),        // Purple
    LEGENDARY(0xFFFF9800, 5.0f),   // Orange
    MYTHIC(0xFFE91E63, 10.0f)      // Pink
}
