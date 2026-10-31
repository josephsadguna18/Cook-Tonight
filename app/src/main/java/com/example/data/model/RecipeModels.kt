package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class Ingredient(
    val name: String = "",
    val quantity: String = ""
)

data class Recipe(
    val id: String = "",
    val name: String = "",
    val cuisine: String = "",
    val time: String = "",
    val serves: String = "",
    val steps: List<String> = emptyList(),
    val ingredients: List<Ingredient> = emptyList()
)

data class SubstituteOption(
    val name: String = "",
    val note: String = ""
)

data class IngredientItemState(
    val ingredient: Ingredient,
    val isHave: Boolean = false,
    val substitutes: List<SubstituteOption> = emptyList(),
    val isLoadingSubstitutes: Boolean = false,
    val substituteError: String? = null,
    val priceBlinkit: Double? = null,
    val priceZepto: Double? = null,
    val priceInstamart: Double? = null
)

enum class GroceryStore(
    val displayName: String,
    val baseUrl: String,
    val searchUrlPattern: String
) {
    BLINKIT(
        displayName = "Blinkit",
        baseUrl = "https://blinkit.com",
        searchUrlPattern = "https://blinkit.com/s/?q=%s"
    ),
    ZEPTO(
        displayName = "Zepto",
        baseUrl = "https://www.zepto.com",
        searchUrlPattern = "https://www.zepto.com/search?query=%s"
    ),
    INSTAMART(
        displayName = "Instamart",
        baseUrl = "https://www.swiggy.com/instamart",
        searchUrlPattern = "https://www.swiggy.com/instamart/search?custom_back=true&query=%s"
    )
}

@IgnoreExtraProperties
data class CachedRecipeGroupEntity(
    @DocumentId val id: String = "",
    val query: String = "",
    val cuisine: String = "",
    val recipesJson: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedRecipeEntity(
    @DocumentId val id: String = "",
    val recipeId: String = "",
    val name: String = "",
    val cuisine: String = "",
    val time: String = "",
    val serves: String = "",
    val steps: List<String> = emptyList(),
    val ingredients: List<Map<String, String>> = emptyList(),
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
