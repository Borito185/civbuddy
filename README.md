# CivBuddy

Fabric mod for CivMC. Originally forked from [veinbuddy](https://github.com/sbobicus/veinbuddy).

## Features

### Vein Mining System
Visual marking and tracking system for ore veins.

**Commands:**
- `/civbuddy digrange <x> <y> <z>` - Set mining radius for marked veins
- `/civbuddy digradius <radius>` - Set uniform radius for all axes
- `/civbuddy changedigrange <radius>` - Update radius for all existing markers
- `/civbuddy render` - Toggle vein overlay rendering
- `/civbuddy clear` - Clear all vein markers

**Controls:**
- Hold right-click with pickaxe to charge placement
- Release to place marker at targeted position
- Quick right-click to remove targeted marker
- Charge time determines placement distance 

**Rendering:**
- Selection markers (green) show vein centers
- Range boxes (red) show mining area per vein
- Highlight box (customizable) shows placement preview
- Persistent markers saved per world/server

### Vein Counter System
Automated ore tracking for coordinated mining operations.

**Commands:**
- `/civbuddy group <name>` - Set namelayer group for count broadcasts
- `/civbuddy name <key>` - Assign key identifier to current vein
- `/civbuddy reset` - Reset current vein count
- `/civbuddy listnames` - Display all tracked veins

**Functionality:**
- Automatically detects ore discovery messages from server
- Tracks count per vein using assigned keys
- Broadcasts updates to specified namelayer group
- Filters player chat to only process system messages
- Per-vein persistent storage

### Calculator
In-game expression evaluator with shortcuts.

**Command:**
- `/calc <expression>` - Evaluate mathematical expression
- Alias: `/civbuddy calc`

**Shortcuts:**
- `s`, `ci`, `cs` = 64 
- `cs` = 4096 
- `k` = 1000

**Features:**
- Implicit multiplication support
- Click result to copy to clipboard
- Supports standard operators and functions

### Command Bookmark Manager
GUI-based command organization system. Access with backslash (`\`).

**Features:**
- Category-based organization with color coding
- Global search across all categories
- Drag-and-drop reordering and copying
- Automatic history tracking (last 20 commands)
- Duplicate prevention
- Read-only History category
- Persistent storage per world/server

**Usage:**
- Click command to execute
- Click `⁝⁝⁝` to select for editing/drag
- `+ Add` creates categories or commands
- Categories show entry count
- Search works across all folders

### Commands
All commands use `civbuddy` or `cb` namespace.

**Aliases:**
- `/civbuddy` = `/cb`
- Individual commands can be called directly (e.g., `/calc`)

## Data Storage
- Vein markers: `data/veinbuddy/<world>.gson`
- Bookmarks: `config/civbuddy_bookmarks.json`
- Per-world/per-server persistence

## Requirements
- Minecraft 1.21.8
- Fabric Loader 0.17.2+
- Fabric API

## Development Setup
See [Fabric Wiki](https://fabricmc.net/wiki/tutorial:setup) for IDE-specific instructions.

## License
Available under CC0 license.
