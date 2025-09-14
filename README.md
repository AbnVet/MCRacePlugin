# BOCRacePlugin

A comprehensive boat racing plugin for Minecraft 1.21.6 that supports both singleplayer time trials and multiplayer races with a full lobby system.

## Features

### 🏁 Race Types
- **Singleplayer**: Time trials with personal best tracking
- **Multiplayer**: Up to 10 players with finish order ranking

### 🎮 Interactive Course Setup
- Easy-to-use interactive setup mode
- Click-to-assign buttons and locations
- Real-time validation and completion checking
- Support for both singleplayer and multiplayer courses

### 🏆 Statistics & Leaderboards
- Personal best times
- Win/loss tracking
- Attempt counters
- Top 10 leaderboards
- Recent race history with timestamps

### 🎯 PlaceholderAPI Integration
- Rich placeholder support for external displays
- Compatible with DecentHolograms and other display plugins
- Real-time leaderboard data

### ⚙️ Configuration
- Fully configurable messages with MiniMessage formatting
- Customizable sounds and colors
- Adjustable race timeouts and cooldowns
- Performance optimization settings

## Installation

1. Download the latest release from the [GitHub repository](https://github.com/AbnVet/MCRacePlugin.git)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated `config.yml` and `messages.yml` files

### Dependencies
- **Paper 1.21.6** (required)
- **PlaceholderAPI** (optional, for placeholder support)

## Commands

### Admin Commands
- `/bocrace create <name> <type>` - Create a new race course
- `/bocrace edit <name>` - Edit an existing course
- `/bocrace delete <name>` - Delete a course
- `/bocrace list` - List all available courses
- `/bocrace tp <name>` - Teleport to a course
- `/bocrace info <name>` - Get course information
- `/bocrace reload` - Reload plugin configuration

### Player Commands
- `/bocrace join <name>` - Join a multiplayer race lobby
- `/bocrace leave` - Leave a race lobby
- `/bocrace start` - Start a race (lobby leader only)
- `/bocrace stats <name>` - View your race statistics
- `/bocrace recent <name>` - View your most recent race results

## Permissions

- `bocrace.*` - All permissions
- `bocrace.admin` - Admin permissions (create, edit, delete, etc.)
- `bocrace.use` - Basic permission to use the plugin (default: true)
- `bocrace.create` - Create courses (default: op)
- `bocrace.edit` - Edit courses (default: op)
- `bocrace.delete` - Delete courses (default: op)
- `bocrace.reload` - Reload configuration (default: op)
- `bocrace.tp` - Teleport to courses (default: op)
- `bocrace.info` - View course information (default: op)
- `bocrace.list` - List courses (default: op)

## Course Setup

### Singleplayer Courses
Required elements:
- Course Lobby spawn point
- Boat spawn location
- Start line cuboid
- Finish line cuboid
- Return to lobby button

### Multiplayer Courses
Required elements:
- Course Lobby spawn point
- 10 boat spawn locations
- Start line cuboid
- Finish line cuboid
- Create race button
- Join race button
- Start race button
- 10 boat spawn buttons
- Return to lobby button
- Return to main button

Optional elements:
- Main Lobby spawn point (global hub)

### Setup Process
1. Use `/bocrace create <name> <type>` to start setup
2. Follow the interactive prompts to assign each element
3. Click on blocks/buttons to assign them
4. Complete all required elements to finish setup

## PlaceholderAPI

The plugin provides extensive PlaceholderAPI support:

### Player Statistics
- `%bocrace_besttime_<course>%` - Player's best time
- `%bocrace_lasttime_<course>%` - Player's last race time
- `%bocrace_attempts_<course>%` - Total attempts
- `%bocrace_races_<course>%` - Completed races
- `%bocrace_wins_<course>%` - Total wins
- `%bocrace_winrate_<course>%` - Win percentage
- `%bocrace_averagetime_<course>%` - Average race time
- `%bocrace_position_<course>%` - Leaderboard position
- `%bocrace_recent_<course>%` - Most recent race time

### Leaderboards
- `%bocrace_leaderboard_<course>_<position>%` - Leaderboard entry

## Configuration

### Main Configuration (`config.yml`)
```yaml
general:
  max-players-per-race: 10
  race-timeout: 300
  debug: false

race:
  singleplayer-enabled: true
  multiplayer-enabled: true
  cooldown: 5
  boat-cleanup-delay: 30

storage:
  type: yaml
  yaml:
    courses-file: "courses.yml"
    stats-file: "stats.yml"
```

### Messages (`messages.yml`)
All messages support MiniMessage formatting and can be fully customized:
```yaml
general:
  prefix: "<gradient:#00ff00:#00ffff>BOCRace</gradient>"
  no-permission: "{prefix} <red>You don't have permission to use this command!"

race:
  singleplayer:
    started: "{prefix} <green>Singleplayer race started! Cross the start line to begin timing."
    finished: "{prefix} <green>Race finished! Time: <highlight>{time}</highlight>"
```

## Race Flow

### Singleplayer
1. Player clicks start button
2. Player is teleported to boat spawn
3. Player mounts boat
4. Timer starts when crossing start line
5. Timer stops when crossing finish line
6. Results are saved and displayed

### Multiplayer
1. Leader clicks "Create Race" button
2. Other players click "Join Race" button
3. Leader clicks "Start Race" button
4. All players are teleported to random spawn points
5. Race starts when crossing start line
6. Players are ranked by finish order
7. Results are saved and displayed

## Data Storage

The plugin uses YAML files for data storage:
- `courses.yml` - Course configurations
- `stats.yml` - Player statistics and race history

All data is automatically saved and loaded. The plugin is designed to support MySQL in future versions.

## Performance

The plugin is optimized for performance:
- Async operations where possible
- Configurable update tick rates
- Efficient data structures
- Minimal memory footprint

## Support

For support, bug reports, or feature requests, please visit the [GitHub repository](https://github.com/AbnVet/MCRacePlugin.git).

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Changelog

### Version 1.0.0
- Initial release
- Singleplayer and multiplayer race support
- Interactive course setup
- Statistics tracking
- PlaceholderAPI integration
- Full configuration system
