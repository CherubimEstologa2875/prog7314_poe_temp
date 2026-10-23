package com.example.runway.domain.model

// The 25 colours an item can be tagged with alongside its ARGB value.
// Neutrals are tagged rather than calculated, else tones like beige read as yellows once converted

enum class ItemColour(val label: String, val argb: Int, val neutral: Boolean = false) {
    WHITE("White", 0xFFFFFFFF.toInt(), neutral = true),
    CREAM("Cream", 0xFFF5EFE0.toInt(), neutral = true),
    BEIGE("Beige", 0xFFE3D5B8.toInt(), neutral = true),
    TAUPE("Taupe", 0xFFB8A894.toInt(), neutral = true),
    TAN("Tan", 0xFFC08A4E.toInt(), neutral = true),
    CAMEL("Camel", 0xFFA9743B.toInt(), neutral = true),
    BROWN("Brown", 0xFF6B4423.toInt(), neutral = true),
    BLACK("Black", 0xFF101010.toInt(), neutral = true),
    CHARCOAL("Charcoal", 0xFF3A3E43.toInt(), neutral = true),
    GREY("Grey", 0xFF9AA0A6.toInt(), neutral = true),
    NAVY("Navy", 0xFF1B2A4A.toInt(), neutral = true),
    BLUE("Blue", 0xFF3D6DB5.toInt()),
    LIGHT_BLUE("Light blue", 0xFF8FB3DB.toInt()),
    TEAL("Teal", 0xFF1F7A72.toInt()),
    GREEN("Green", 0xFF2E7D46.toInt()),
    SAGE("Sage", 0xFFA8BFA0.toInt()),
    OLIVE("Olive", 0xFF4E5B31.toInt()),
    YELLOW("Yellow", 0xFFEDD26A.toInt()),
    MUSTARD("Mustard", 0xFFC9A227.toInt()),
    ORANGE("Orange", 0xFFD2762B.toInt()),
    RED("Red", 0xFFC62031.toInt()),
    BURGUNDY("Burgundy", 0xFF7B1E2B.toInt()),
    PINK("Pink", 0xFFE8A0B4.toInt()),
    PURPLE("Purple", 0xFF4A3A5C.toInt()),
    LILAC("Lilac", 0xFFB3A5D6.toInt());

    companion object {
        // Allow for inconsistent spacing and capitalisation
        fun parse(value: String?): ItemColour? {
            val cleaned = value?.trim()?.uppercase()?.replace(" ", "") ?: return null
            if (cleaned.isEmpty()) return null
            return entries.firstOrNull { it.label.uppercase().replace(" ", "") == cleaned }
        }
    }
}