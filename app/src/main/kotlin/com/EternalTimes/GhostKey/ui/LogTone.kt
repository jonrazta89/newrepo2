package com.EternalTimes.GhostKey.ui

/**
 * Log semantic tone. Shared between main terminal and log detail page for consistent coloring.
 *
 * Priority order: explicit error marker > error/fail text > warn > system (<s>) > flow (<b>) > plain output.
 * Note: warn is a separate tier (yellow in main terminal), not merged with error.
 */
enum class LogTone { SYSTEM, FLOW, ERROR, WARN, PLAIN }

/** Determine tone from log line content (aligned with native log English tags). */
fun logToneOf(line: String): LogTone = when {
    line.startsWith("<->") -> LogTone.ERROR
    line.contains("error", ignoreCase = true) ||
        line.contains("fail", ignoreCase = true) -> LogTone.ERROR
    line.contains("warn", ignoreCase = true) -> LogTone.WARN
    line.startsWith("<s>") -> LogTone.SYSTEM
    line.startsWith("<b>") -> LogTone.FLOW
    else -> LogTone.PLAIN
}
