# BOCRacePlugin PlaceholderAPI Reference

This document lists all available placeholders for the BOCRacePlugin PlaceholderAPI expansion.

## Table of Contents
1. [Course Status](#course-status)
2. [Course Records](#course-records)
3. [Course Usage](#course-usage)
4. [Leaderboards](#leaderboards)
5. [Period-Based Leaderboards](#period-based-leaderboards)
6. [Player Status](#player-status)
7. [Player Statistics](#player-statistics)
8. [Player Records](#player-records)
9. [Multiplayer Race Info](#multiplayer-race-info)
10. [DQ Information](#dq-information)
11. [Global Statistics](#global-statistics)

---

## Course Status

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_course_<coursename>_status%` | Course availability status | `<green>Open`, `<light_purple>In Use`, `<red>Setup` |

**Status Values:**
- `<green>Open` - Course is available for racing
- `<light_purple>In Use` - Someone is currently racing on this course
- `<red>Setup` - Course is in setup mode (maintenance)

---

## Course Records

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_course_<coursename>_record%` | Course record holder name | `Tarzan` |
| `%bocrace_course_<coursename>_record_time%` | Course record time | `12.80` or `1:35` |

**Examples:**
- `%bocrace_course_CoyoteSixTest1_record%` → `Tarzan`
- `%bocrace_course_CoyoteSixTest1_record_time%` → `12.80`

---

## Course Usage

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_course_<coursename>_usage%` | Total times course has been raced | `156` |

**Examples:**
- `%bocrace_course_CoyoteSixTest1_usage%` → `156`

---

## Leaderboards

### Standard Leaderboards (All-Time)
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_leaderboard_<coursename>_name_<position>%` | Player name at position | `Tarzan` |
| `%bocrace_leaderboard_<coursename>_time_<position>%` | Time at position | `12.80` or `1:35` |
| `%bocrace_leaderboard_<coursename>_<position>%` | Combined name and time (legacy) | `Tarzan - 12.80` |

**Examples:**
- `%bocrace_leaderboard_CoyoteSixTest1_name_1%` → `Tarzan`
- `%bocrace_leaderboard_CoyoteSixTest1_time_1%` → `12.80`
- `%bocrace_leaderboard_CoyoteSixTest1_1%` → `Tarzan - 12.80`

---

## Period-Based Leaderboards

### Daily Leaderboards (Today's Records)
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_leaderboard_<coursename>_daily_name_<position>%` | Today's best player name | `TestPlayer1` |
| `%bocrace_leaderboard_<coursename>_daily_time_<position>%` | Today's best time | `12.50` |

### Weekly Leaderboards (Last 7 Days)
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_leaderboard_<coursename>_weekly_name_<position>%` | Weekly best player name | `TestPlayer2` |
| `%bocrace_leaderboard_<coursename>_weekly_time_<position>%` | Weekly best time | `13.20` |

### Monthly Leaderboards (Current Month)
| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_leaderboard_<coursename>_monthly_name_<position>%` | Monthly best player name | `TestPlayer1` |
| `%bocrace_leaderboard_<coursename>_monthly_time_<position>%` | Monthly best time | `11.95` |

**Examples:**
- `%bocrace_leaderboard_CoyoteSixTest1_daily_name_1%` → `TestPlayer1`
- `%bocrace_leaderboard_CoyoteSixTest1_weekly_time_3%` → `14.80`
- `%bocrace_leaderboard_CoyoteSixTest1_monthly_name_5%` → `TestPlayer5`

---

## Player Status

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_player_status%` | Current player racing status | `Racing (RUNNING)`, `Not Racing` |
| `%bocrace_player_current_time%` | Current race time (if racing) | `15.30` or `N/A` |
| `%bocrace_player_course%` | Current course name (if racing) | `CoyoteSixTest1` or `None` |
| `%bocrace_player_position%` | Current position in MP race | `1st` or `N/A` |

**Examples:**
- `%bocrace_player_status%` → `Racing (RUNNING)`
- `%bocrace_player_current_time%` → `15.30`

---

## Player Statistics

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_player_races_completed%` | Total races completed | `45` |

**Examples:**
- `%bocrace_player_races_completed%` → `45`

---

## Player Records

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_player_pb_<coursename>%` | Personal best time for course | `12.80` or `No PB` |

**Examples:**
- `%bocrace_player_pb_CoyoteSixTest1%` → `12.80`
- `%bocrace_player_pb_AZ_Course_1%` → `No PB`

---

## Multiplayer Race Info

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_mp_players_joined%` | Players joined in MP race | `4` |
| `%bocrace_mp_race_status%` | Current MP race status | `WAITING`, `RUNNING`, `FINISHED` |
| `%bocrace_mp_time_remaining%` | Time until race timeout | `2:45` or `N/A` |
| `%bocrace_mp_leader%` | Current race leader name | `Tarzan` or `N/A` |
| `%bocrace_mp_leader_time%` | Leader's current time | `12.50` or `N/A` |

**Examples:**
- `%bocrace_mp_players_joined%` → `4`
- `%bocrace_mp_leader%` → `Tarzan`

---

## DQ Information

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_player_last_race_status%` | Last race completion status | `Completed - 12.80`, `DQ - Exited boat` |
| `%bocrace_player_dq_count%` | Total DQ count for player | `3` |
| `%bocrace_player_completion_rate%` | Completion rate percentage | `85%` |
| `%bocrace_player_last_dq_reason%` | Most recent DQ reason | `Exited boat during race` |
| `%bocrace_course_<coursename>_dq_rate%` | Course DQ rate percentage | `15%` |

**Examples:**
- `%bocrace_player_dq_count%` → `3`
- `%bocrace_course_CoyoteSixTest1_dq_rate%` → `15%`

---

## Global Statistics

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%bocrace_total_courses%` | Total number of courses | `9` |
| `%bocrace_active_races%` | Active singleplayer races | `2` |
| `%bocrace_active_mp_races%` | Active multiplayer races | `1` |

**Examples:**
- `%bocrace_total_courses%` → `9`
- `%bocrace_active_races%` → `2`

---

## Time Formatting

All time outputs follow this format:
- **Under 1 minute**: `12.80` (2 decimal places)
- **Over 1 minute**: `1:35` (MM:SS format, no decimals)
- **No data**: `N/A`

## Color Formatting

Status placeholders use MiniMessage colors:
- **Available**: `<green>Open`
- **In Use**: `<light_purple>In Use`
- **Setup**: `<red>Setup`

## Testing Placeholders

Use the built-in test command to verify placeholders:
```
/bocrace testpapi test %bocrace_leaderboard_CoyoteSixTest1_daily_name_1%
```

## Generating Test Data

To test period-based leaderboards:
```
/bocrace test-leaderboards CoyoteSixTest1
```

This generates 50+ test records across 30 days with 5 different players.

---

## Notes

- Replace `<coursename>` with actual course names (e.g., `CoyoteSixTest1`, `AZ_Course_1`)
- Replace `<position>` with numbers 1-10 for leaderboard positions
- All placeholders work in both singleplayer and multiplayer contexts
- Period-based leaderboards require recent race data to show meaningful results
- Use `/bocrace testpapi list` for a quick reference of available placeholders
