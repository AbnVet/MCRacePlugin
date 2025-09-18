# 🏁 **BOCRacePlugin - PlaceholderAPI Integration Guide**

## 🎯 **Overview**

BOCRacePlugin now includes comprehensive PlaceholderAPI support with **25+ placeholders** for use with [DecentHolograms](https://wiki.decentholograms.eu/) and other plugins that support PlaceholderAPI.

All placeholders use the identifier `%bocrace_<placeholder>%`.

---

## 📊 **Available Placeholders**

### 🎮 **Player-Specific Placeholders**

These placeholders require a player context and show information about the specific player viewing the hologram.

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%bocrace_player_status%` | Player's current racing status | `"Racing (RUNNING)"`, `"Not Racing"` |
| `%bocrace_player_current_time%` | Live race timer for active races | `"1:23.456"`, `"N/A"` |
| `%bocrace_player_course%` | Current course name | `"SpeedRun1"`, `"None"` |
| `%bocrace_player_position%` | Position in multiplayer race | `"1st"`, `"3rd"`, `"N/A"` |
| `%bocrace_player_races_completed%` | Total races completed by player | `"47"`, `"0"` |
| `%bocrace_player_pb_<course>%` | Personal best on specific course | `"2:15.342"`, `"No PB"` |

#### **Personal Best Examples:**
```
%bocrace_player_pb_SpeedRun1%    # Personal best on SpeedRun1 course
%bocrace_player_pb_Circuit_A%    # Personal best on Circuit_A course
%bocrace_player_pb_Endurance%    # Personal best on Endurance course
```

---

### 🏆 **Course & Statistics Placeholders**

These placeholders work without a player context and show general course information.

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%bocrace_course_<name>_status%` | Course availability status | `"Available"`, `"In Use"`, `"Setup Mode"` |
| `%bocrace_course_<name>_record%` | Course record holder name | `"PlayerName"`, `"No Record"` |
| `%bocrace_course_<name>_record_time%` | Course record time | `"1:45.123"`, `"No Record"` |
| `%bocrace_course_<name>_usage%` | Times course has been used | `"156"`, `"0"` |

#### **Course Status Examples:**
```
%bocrace_course_SpeedRun1_status%        # "Available" / "In Use" / "Setup Mode"
%bocrace_course_Circuit_A_record%        # "AbnVet" / "No Record"  
%bocrace_course_Endurance_record_time%   # "3:22.145" / "No Record"
%bocrace_course_SpeedRun1_usage%         # "89"
```

---

### 🥇 **Leaderboard Placeholders**

Display top 10 leaderboard entries for any course.

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%bocrace_leaderboard_<course>_<position>%` | Leaderboard entry | `"AbnVet - 1:45.123"`, `"No Entry"` |

#### **Leaderboard Examples:**
```
%bocrace_leaderboard_SpeedRun1_1%    # "AbnVet - 1:45.123"     (1st place)
%bocrace_leaderboard_SpeedRun1_2%    # "PlayerTwo - 1:47.892"  (2nd place)
%bocrace_leaderboard_SpeedRun1_3%    # "SpeedRacer - 1:52.001" (3rd place)
%bocrace_leaderboard_Circuit_A_10%   # "SlowPoke - 4:32.111"   (10th place)
```

---

### 🎯 **Multiplayer Race Placeholders**

These show information about active multiplayer races (player context required).

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%bocrace_mp_players_joined%` | Players in current MP race | `"7"`, `"0"` |
| `%bocrace_mp_race_status%` | Multiplayer race status | `"LOBBY"`, `"RUNNING"`, `"Not in race"` |
| `%bocrace_mp_time_remaining%` | Time until race timeout | `"4:23"`, `"Expired"`, `"N/A"` |
| `%bocrace_mp_leader%` | Current race leader | `"PlayerName"`, `"N/A"` |
| `%bocrace_mp_leader_time%` | Leader's current time | `"2:15.445"`, `"N/A"` |

---

### 📈 **Global Statistics Placeholders**

Server-wide statistics (no player context needed).

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%bocrace_total_courses%` | Total courses on server | `"12"` |
| `%bocrace_active_races%` | Active singleplayer races | `"3"` |
| `%bocrace_active_mp_races%` | Active multiplayer races | `"1"` |

---

## 🎨 **DecentHolograms Integration Examples**

### **📊 Course Status Board**
```yaml
# Create a hologram showing course availability
/dh create CourseStatus
/dh addline CourseStatus &6&l🏁 SPEEDRUN1 STATUS
/dh addline CourseStatus &7Status: &e%bocrace_course_SpeedRun1_status%
/dh addline CourseStatus &7Record: &a%bocrace_course_SpeedRun1_record%
/dh addline CourseStatus &7Time: &b%bocrace_course_SpeedRun1_record_time%
/dh addline CourseStatus &7Total Races: &e%bocrace_course_SpeedRun1_usage%
```

### **🏆 Leaderboard Hologram**
```yaml
# Create a top 5 leaderboard hologram
/dh create Leaderboard_SpeedRun1
/dh addline Leaderboard_SpeedRun1 &6&l🏆 SPEEDRUN1 LEADERBOARD
/dh addline Leaderboard_SpeedRun1 &e&l1st: &a%bocrace_leaderboard_SpeedRun1_1%
/dh addline Leaderboard_SpeedRun1 &7&l2nd: &f%bocrace_leaderboard_SpeedRun1_2%
/dh addline Leaderboard_SpeedRun1 &c&l3rd: &f%bocrace_leaderboard_SpeedRun1_3%
/dh addline Leaderboard_SpeedRun1 &74th: %bocrace_leaderboard_SpeedRun1_4%
/dh addline Leaderboard_SpeedRun1 &75th: %bocrace_leaderboard_SpeedRun1_5%
```

### **⏱️ Live Race Timer**
```yaml
# Create a personal race timer hologram
/dh create PersonalTimer
/dh addline PersonalTimer &6&l⏱️ YOUR RACE STATUS
/dh addline PersonalTimer &7Status: &e%bocrace_player_status%
/dh addline PersonalTimer &7Course: &a%bocrace_player_course%
/dh addline PersonalTimer &7Time: &b%bocrace_player_current_time%
/dh addline PersonalTimer &7Position: &e%bocrace_player_position%
```

### **📈 Server Statistics**
```yaml
# Create a server stats hologram
/dh create ServerStats
/dh addline ServerStats &6&l📊 RACE SERVER STATS
/dh addline ServerStats &7Total Courses: &e%bocrace_total_courses%
/dh addline ServerStats &7Active Races: &a%bocrace_active_races%
/dh addline ServerStats &7MP Races: &b%bocrace_active_mp_races%
/dh addline ServerStats &7Your Races: &e%bocrace_player_races_completed%
```

### **🎯 Multiplayer Race Info**
```yaml
# Create a multiplayer race status hologram
/dh create MPRaceStatus
/dh addline MPRaceStatus &6&l🎯 MULTIPLAYER RACE
/dh addline MPRaceStatus &7Players: &e%bocrace_mp_players_joined%/10
/dh addline MPRaceStatus &7Status: &a%bocrace_mp_race_status%
/dh addline MPRaceStatus &7Time Left: &c%bocrace_mp_time_remaining%
/dh addline MPRaceStatus &7Leader: &6%bocrace_mp_leader%
/dh addline MPRaceStatus &7Leader Time: &b%bocrace_mp_leader_time%
```

---

## ⚙️ **Technical Implementation**

### **Installation Requirements**
1. **PlaceholderAPI** plugin installed and enabled
2. **BOCRacePlugin** with PlaceholderAPI integration
3. **DecentHolograms** (or any PlaceholderAPI-compatible plugin)

### **Automatic Registration**
The PlaceholderAPI expansion is automatically registered when BOCRacePlugin starts:
```
[BOCRacePlugin] PlaceholderAPI expansion registered successfully!
```

### **Testing Placeholders**
Use PlaceholderAPI's parse command to test placeholders:
```
/papi parse <player> %bocrace_player_status%
/papi parse <player> %bocrace_course_SpeedRun1_record%
```

---

## 🔧 **Placeholder Update Frequency**

| Type | Update Rate | Notes |
|------|-------------|-------|
| **Live Race Timers** | Real-time | Updates every time hologram refreshes |
| **Player Status** | Real-time | Instant updates when race state changes |
| **Course Records** | On completion | Updates when new records are set |
| **Leaderboards** | On completion | Updates when races finish |
| **MP Race Info** | Real-time | Updates as race progresses |

---

## 🎨 **Color Codes & Formatting**

All placeholders return plain text that can be formatted with Minecraft color codes:

```yaml
# Examples of color formatting
&a%bocrace_player_current_time%     # Green timer
&6&l%bocrace_course_SpeedRun1_record% # Gold bold record holder
&c&n%bocrace_mp_race_status%        # Red underlined status
```

---

## 🐛 **Troubleshooting**

### **Common Issues**

1. **Placeholder shows as text**: Ensure PlaceholderAPI is installed and BOCRacePlugin registered successfully
2. **"No Data" or "Error"**: Course name might be incorrect or no data exists
3. **"N/A" values**: Player not in race or feature not applicable

### **Debug Commands**
```
/papi parse <player> %bocrace_player_status%        # Test player status
/papi list                                          # Verify 'bocrace' expansion is loaded
/bocrace reload                                     # Reload plugin if needed
```

---

## 🚀 **Advanced Usage**

### **Dynamic Course Names**
For courses with spaces or special characters:
```
%bocrace_course_Speed_Run_1_status%      # Course named "Speed Run 1"
%bocrace_course_Circuit-A_record%        # Course named "Circuit-A"
%bocrace_leaderboard_Endurance_Race_1%   # Course named "Endurance Race"
```

### **Conditional Display**
Use DecentHolograms conditions for dynamic content:
```yaml
# Show different content based on race status
/dh addline RaceTimer &7{if}%bocrace_player_status% != Not Racing{endif}&bRacing: %bocrace_player_current_time%
/dh addline RaceTimer &7{if}%bocrace_player_status% == Not Racing{endif}&7Not currently racing
```

---

## 📝 **Notes**

- **Performance**: Placeholders are optimized for frequent updates
- **Persistence**: All data persists through server restarts
- **Compatibility**: Works with Paper 1.21.6+ and PlaceholderAPI 2.11.6+
- **Thread Safety**: All placeholder operations are thread-safe

---

**🎉 Your BOCRacePlugin now has full PlaceholderAPI support for amazing hologram displays!**

*For support, contact AbnVet or check the plugin documentation.*
