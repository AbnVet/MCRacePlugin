package com.bocrace.model;

/**
 * Represents different time periods for leaderboard filtering
 */
public enum Period {
    DAILY,   // Current day (midnight to midnight)
    WEEKLY,  // Last 7 days (rolling)
    MONTHLY  // Current calendar month (1st to last day)
}
