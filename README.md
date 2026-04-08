# CivBuddy

QOL features for the CivMC server. It contains the following features:

- **Vein Marking:** Highlights blocks by holding right-click with a pickaxe in hand. Once marked, a 5×5×5 area will appear around the marked block.
- **Multi-Vein Tracking:** Keep track of multiple separate veins by assigning a name for each one.
- **Command Bookmarks:** Organize and quickly execute commands with a GUI system (press `\`).
- **Calculator:** In-game math evaluator with custom shortcuts for quick calculations.
- **Help System:** `/civbuddy help` for in-game feature guide.

CivBuddy is an optimized and improved version of Veinbuddy.

## Features

### Vein Marking System
Visual marking and tracking system for ore veins.

**Commands:**
- `/civbuddy veins digrange <x> <y> <z>` - Set mining radius for marked veins
- `/civbuddy veins digradius <radius>` - Set uniform radius for all axes
- `/civbuddy veins changeall digradius <radius>` - Update radius for all existing markers
- `/civbuddy veins togglerenderer` - Toggle vein overlay rendering
- `/civbuddy veins clear` - Clear all vein markers
- `/civbuddy veins set <name>` - Swap to a different named vein
- `/civbuddy veins info` - Display information about the current vein, including diamonds found by the player.
- `/civbuddy veins share with <nl>` - Live share the current vein with a namelayer.
- `/civbuddy veins share all` - Share the current vein fully, including previous markings.

**Controls:**
- Hold right-click with pickaxe to charge placement
- Release to place marker at targeted position
- Quick right-click to remove targeted marker
- Charge time determines placement distance 

**Rendering:**
- Selection markers (green) show vein centers
- Range boxes (red) show mining area per vein
- Highlight box shows placement preview when charging
- Persistent markers saved per world/server

### Calculator
In-game expression evaluator with CivMC-specific shortcuts.

**Command:**
- `/calc <expression>` - Evaluate mathematical expression
- `/civbuddy calc <expression>` - Alternative syntax

**Shortcuts:**
- `b` = 9 (block)
- `s` or `ci` = 64 (stack/compacted item)
- `cs` = 4,096 (compacted stack)
- `k` = 1,000

**Examples:**
- `2cs + 1s` = 8,256
- `1k*2s` = 128,000
- `(2k + 3s) / s` = 34.25

**Features:**
- Implicit multiplication support (e.g., `2s` works)
- Click result to copy to clipboard
- Supports standard operators: +, -, *, /, (), etc.

### Command Bookmarks
GUI-based command organization system.

**Access:**
- Press `\` (backslash) to open the bookmark GUI

**Features:**
- Category-based organization with custom color coding
- Global search across all categories (works from any category view)
- Drag-and-drop command reordering and copying between categories
- Automatic history tracking (last 20 commands)
- Duplicate prevention within categories
- Read-only History category
- Prebuilt destinations available on first launch
- Persistent storage per world/server

**Usage:**
- Click command to execute immediately
- Click `⁝⁝⁝` marker to select command for editing or dragging
- Click `+ Add` button to create new categories or commands
- Categories display entry count on the right side
- Search filters work across all bookmarks in current view
- Drag selected commands between categories to copy/move

### Help System
In-game command reference guide.

**Command:**
- `/civbuddy help` or `/cb help`

**Displays:**
- Quick overview of all mod features
- Command syntax and examples
- Keybind information
- Available shortcuts and aliases

## Commands Reference

All commands use the `civbuddy` or `cb` namespace.

**Root Commands:**
- `/civbuddy` or `/cb` - Access all features
- `/calc` - Quick calculator access (standalone)

**Command Structure:**
- Vein commands: `/civbuddy veins <subcommand>`
- All other commands work as documented in their sections above

## Data Storage
- Vein markers and selections: `data/veinbuddy/<world>.db`
- Vein counter data: Stored in same file per world/server
- Command bookmarks: `config/civbuddy_bookmarks.json` (per world/server)
- Prebuilt commands: `config/civbuddy_prebuilt_commands.json` (loaded on first launch)

## Internationalization
CivBuddy supports 21 languages with full keybinding translations:
- English, Spanish, French, German, Portuguese (BR), Russian
- Chinese (Simplified), Japanese, Korean, Italian, Dutch, Polish
- Swedish, Danish, Finnish, Norwegian, Czech, Turkish
- Ukrainian, Greek, Hungarian

## Requirements
- Minecraft 1.21.8
- Fabric Loader 0.17.2+
- Fabric API 0.131.0+
- Java 21+

## Development
Originally forked from [veinbuddy](https://github.com/sbobicus/veinbuddy).

**Setup:**
See [Fabric Wiki](https://fabricmc.net/wiki/tutorial:setup) for IDE-specific instructions.

**Building:**
```bash
./gradlew build
```

Output jars will be in `build/libs/`

## License
Available under CC0 license.
