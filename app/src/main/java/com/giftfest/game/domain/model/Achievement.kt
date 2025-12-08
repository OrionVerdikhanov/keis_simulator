package com.giftfest.game.domain.model

/**
 * Predefined achievements for the game
 */
object Achievements {
    val ALL = listOf(
        Achievement(
            id = "first_merge",
            name = "First Steps",
            description = "Perform your first merge",
            target = 1,
            reward = AchievementReward.Coins(100)
        ),
        Achievement(
            id = "merge_10",
            name = "Getting Started",
            description = "Perform 10 merges",
            target = 10,
            reward = AchievementReward.Coins(200)
        ),
        Achievement(
            id = "merge_50",
            name = "Merge Master",
            description = "Perform 50 merges",
            target = 50,
            reward = AchievementReward.Coins(500)
        ),
        Achievement(
            id = "merge_100",
            name = "Merge Legend",
            description = "Perform 100 merges",
            target = 100,
            reward = AchievementReward.Energy(20)
        ),
        Achievement(
            id = "level_5",
            name = "Rising Star",
            description = "Reach player level 5",
            target = 5,
            reward = AchievementReward.Coins(300)
        ),
        Achievement(
            id = "level_10",
            name = "Experienced",
            description = "Reach player level 10",
            target = 10,
            reward = AchievementReward.Coins(500)
        ),
        Achievement(
            id = "level_25",
            name = "Veteran",
            description = "Reach player level 25",
            target = 25,
            reward = AchievementReward.Coins(1000)
        ),
        Achievement(
            id = "gift_level_3",
            name = "Upgrader",
            description = "Create a level 3 gift",
            target = 3,
            reward = AchievementReward.Coins(150)
        ),
        Achievement(
            id = "gift_level_5",
            name = "Expert Crafter",
            description = "Create a level 5 gift",
            target = 5,
            reward = AchievementReward.Coins(300)
        ),
        Achievement(
            id = "gift_level_8",
            name = "Gift Artisan",
            description = "Create a level 8 gift",
            target = 8,
            reward = AchievementReward.Energy(30)
        ),
        Achievement(
            id = "gift_level_10",
            name = "Legendary Crafter",
            description = "Create a level 10 gift",
            target = 10,
            reward = AchievementReward.Coins(2000)
        ),
        Achievement(
            id = "collect_5",
            name = "Collector",
            description = "Unlock 5 gift types",
            target = 5,
            reward = AchievementReward.Coins(400)
        ),
        Achievement(
            id = "collect_10",
            name = "Treasure Hunter",
            description = "Unlock 10 gift types",
            target = 10,
            reward = AchievementReward.Coins(1000)
        ),
        Achievement(
            id = "collect_all",
            name = "Complete Collection",
            description = "Unlock all 12 gift types",
            target = 12,
            reward = AchievementReward.Coins(5000)
        ),
        Achievement(
            id = "daily_3",
            name = "Dedicated",
            description = "Login 3 days in a row",
            target = 3,
            reward = AchievementReward.Energy(15)
        ),
        Achievement(
            id = "daily_7",
            name = "Loyal Player",
            description = "Login 7 days in a row",
            target = 7,
            reward = AchievementReward.Coins(1000)
        )
    )

    fun getById(id: String): Achievement? = ALL.find { it.id == id }
}
