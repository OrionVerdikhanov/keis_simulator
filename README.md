# Gift Fest - Merge Game for Android

A fun merge game where you collect and combine gifts to level them up!

## Features

- **Merge Mechanics**: Combine two identical gifts to create a higher-level gift
- **12 Gift Types**: Unlock new gifts as you progress
- **Energy System**: Energy regenerates over time (7 minutes per energy point)
- **Player Progression**: Earn XP and coins by merging gifts
- **Achievements**: Complete challenges to earn rewards
- **Collection**: Track all your unlocked gift types
- **Local Storage**: All data saved locally using Room database

## Gift Types by Rarity

| Rarity | Gifts |
|--------|-------|
| Common | Ice Castle, Ornament |
| Uncommon | Trophy, Carnival Mask |
| Rare | Gift Box, Rocket |
| Epic | Champagne, Diamond |
| Legendary | Flying Squirrel, Rose |
| Mythic | Teddy Bear, Mystery |

## How to Play

1. Tap **GET GIFT** to spawn a random gift (costs 5 energy)
2. Tap a gift to select it
3. Tap another gift of the same type and level to merge them
4. Higher-level gifts give more XP and coins!
5. Unlock new cells by leveling up

## Building the Project

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17 or later
- Android SDK 34

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

### Project Structure

```
app/
├── src/main/
│   ├── java/com/giftfest/game/
│   │   ├── data/           # Database, DAOs, Entities
│   │   ├── domain/         # Models, Use Cases
│   │   ├── ui/             # Compose UI Components
│   │   │   ├── component/  # Reusable components
│   │   │   ├── screen/     # Screen composables
│   │   │   └── theme/      # App theme
│   │   └── viewmodel/      # ViewModels
│   └── res/                # Resources
├── build.gradle.kts        # App module build config
└── proguard-rules.pro      # ProGuard rules
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room
- **Async**: Kotlin Coroutines & Flow
- **DI**: Manual dependency injection

## Game Mechanics

### Energy System
- Maximum energy: 40
- Spawn cost: 5 energy
- Regeneration: 1 energy every 7 minutes

### Experience & Leveling
- XP gained per merge depends on gift level and rarity
- Higher rarity gifts give more XP
- Leveling up unlocks new board cells

### Merge Rules
- Only same-type and same-level gifts can merge
- Maximum gift level: 12
- Each merge creates a gift one level higher

## License

This project is for educational purposes.
