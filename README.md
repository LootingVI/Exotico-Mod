# Exotico ✦

**Exotico** is a premium, high-performance Minecraft client-side mod built with Fabric for **Minecraft 1.21.11**. It is specifically designed for Hypixel SkyBlock enthusiasts to detect, highlight, and track rare "Exotic" armor colors, glitched items, and limited-edition dyed variants.

---

## ✨ Features

### 🔍 Advanced Scanning
- **Auto-Scanner:** Automatically scans players in your lobby and identifies exotic armor sets in real-time.
- **Level Detection:** Robustly extracts Hypixel Network Levels from the tab list to help identify potential "un-nicked" high-level players.
- **Dye Filtering:** Smart detection for official Hypixel Dyes (e.g., Chocolate Dye) to avoid false-positive exotic alerts.

### 🎨 Visuals & UI
- **Inventory Highlighting:** Exotic items are highlighted directly in your inventory, chests, and trading menus with color-coded borders (e.g., Purple for Glitched, Cyan for Crystal Dyed, Gold for true Exotics).
- **HUD Overlay:** A sleek, modern HUD displaying players with exotics currently present in your lobby.
- **Detailed Tooltips:** Enhanced item tooltips showing the exact Hex code, variant name (Fairy, Crystal, Glitched, etc.), and a brief history/description of the color.
- **Wavy Capes:** Includes a custom cape system with smooth animations.

### ⚙️ Automation & Integration
- **Auto-Message:** Automatically send customizable messages to players found with exotics. Includes configurable cooldowns and level requirements.
- **Discord Webhook Support:** Feed exotic detections directly to your private Discord channel for remote tracking.
- **Modern Settings UI:** A fully custom, flat-design settings screen (default key: `RSHIFT`) with masked API key input and clipboard support.

---

## 🚀 Getting Started

### Prerequisites
- Minecraft **1.21.11**
- **Fabric Loader**
- Your personal **API Key** (DM **linaaaaa_aaaaa** on Discord to request access)

### Installation
1. Download the latest `.jar` from the releases page (or build it yourself).
2. Drop the file into your Minecraft `%appdata%\.minecraft\mods` folder.
3. Launch the game using the Fabric profile.
4. Type `/exotico settings` in-game to open the settings and paste your API key.

---

## 🛠️ Development

### Building from Source
The project uses Gradle for dependency management.

```bash
# Clone the repository
git clone https://github.com/your-repo/exotico.git
cd exotico

# Build the project
./gradlew build
```
The compiled jar will be located in `build/libs/`.

### Architecture
- **Mixins:** Extensively uses SpongePowered Mixins to tap into Minecraft's rendering engine and inventory systems.
- **Fabric API:** Leverages the latest Fabric Client APIs for events and networking.
- **Custom UI:** Implements a standalone GUI system without external libraries for maximum performance and stability.

---

## 🛡️ License & Disclaimer
© 2026 Flori & Exotico Team.
This mod is an independent project and is not affiliated with Hypixel Inc. or Mojang AB. Use responsibly and adhere to server rules regarding automated messaging and player interaction.
