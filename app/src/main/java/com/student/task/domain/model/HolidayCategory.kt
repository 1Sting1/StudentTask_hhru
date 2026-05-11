package com.student.task.domain.model

enum class HolidayCategory(val displayName: String, val emoji: String) {
    OFFICIAL("Государственный", "🇷🇺"),
    PROFESSIONAL("Профессиональный", "💼"),
    INTERNATIONAL("Международный", "🌍"),
    FOLK("Народный", "🎭"),

    DEMONSTRATION ("Несуществующий", "👻")
}
