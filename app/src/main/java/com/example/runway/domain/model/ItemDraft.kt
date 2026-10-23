package com.example.runway.domain.model

// What the user has typed into the tag form so far, before it becomes an Item.
data class ItemDraft(
    val name: String = "",
    val category: String = "TOP",
    val colour: PaletteColour? = null,
    val brand: String = "",
    val size: String = "",
    // Kept as text so "12.5.3" can be flagged instead of quietly becoming no price.
    val priceText: String = "",
    val wearLimit: Int = RunwaySettings.DEFAULT_WEAR_LIMIT,
) {
    enum class Problem {
        NAME_MISSING,
        NAME_TOO_LONG,
        PRICE_NOT_A_NUMBER,
        PRICE_NEGATIVE,
        WEAR_LIMIT_OUT_OF_RANGE,
    }

    val problems: Set<Problem>
        get() = buildSet {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) add(Problem.NAME_MISSING)
            if (trimmed.length > MAX_NAME) add(Problem.NAME_TOO_LONG)

            val price = priceText.trim()
            if (price.isNotEmpty()) {
                val parsed = price.replace(',', '.').toDoubleOrNull()
                if (parsed == null) add(Problem.PRICE_NOT_A_NUMBER)
                else if (parsed < 0) add(Problem.PRICE_NEGATIVE)
            }

            if (wearLimit !in RunwaySettings.MIN_WEAR_LIMIT..RunwaySettings.MAX_WEAR_LIMIT) {
                add(Problem.WEAR_LIMIT_OUT_OF_RANGE)
            }
        }

    val isValid: Boolean get() = problems.isEmpty()

    // A comma is accepted too, since that's how many South African keyboards type decimals.
    val price: Double? get() = priceText.trim().replace(',', '.').toDoubleOrNull()

    // Only call this once isValid is true.
    fun toItem(imagePath: String?): Item = Item(
        name = name.trim(),
        category = category,
        colour = colour?.label,
        brand = brand.trim().ifEmpty { null },
        size = size.trim().ifEmpty { null },
        purchasePrice = price,
        imagePath = imagePath,
        wearLimit = wearLimit,
    )

    companion object {
        // Same limit the API enforces.
        const val MAX_NAME = 120
    }
}
