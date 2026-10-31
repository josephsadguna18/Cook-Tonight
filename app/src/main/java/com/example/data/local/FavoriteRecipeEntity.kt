package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Ingredient
import com.example.data.model.Recipe
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val cuisine: String,
    val time: String,
    val serves: String,
    val stepsJson: String,
    val ingredientsJson: String,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toRecipe(): Recipe {
        val stepsList = mutableListOf<String>()
        try {
            val arr = JSONArray(stepsJson)
            for (i in 0 until arr.length()) {
                stepsList.add(arr.getString(i))
            }
        } catch (_: Exception) {}

        val ingredientsList = mutableListOf<Ingredient>()
        try {
            val arr = JSONArray(ingredientsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                ingredientsList.add(
                    Ingredient(
                        name = obj.optString("name", ""),
                        quantity = obj.optString("quantity", "")
                    )
                )
            }
        } catch (_: Exception) {}

        return Recipe(
            id = id,
            name = name,
            cuisine = cuisine,
            time = time,
            serves = serves,
            steps = stepsList,
            ingredients = ingredientsList
        )
    }

    companion object {
        fun fromRecipe(recipe: Recipe): FavoriteRecipeEntity {
            val stepsArr = JSONArray()
            recipe.steps.forEach { stepsArr.put(it) }

            val ingArr = JSONArray()
            recipe.ingredients.forEach { ing ->
                val obj = JSONObject()
                obj.put("name", ing.name)
                obj.put("quantity", ing.quantity)
                ingArr.put(obj)
            }

            // Stable key based on recipe name and cuisine
            val stableId = if (recipe.id.isNotBlank()) {
                recipe.id
            } else {
                "${recipe.cuisine}_${recipe.name}".lowercase().replace(Regex("[^a-z0-9]"), "_")
            }

            return FavoriteRecipeEntity(
                id = stableId,
                name = recipe.name,
                cuisine = recipe.cuisine,
                time = recipe.time,
                serves = recipe.serves,
                stepsJson = stepsArr.toString(),
                ingredientsJson = ingArr.toString(),
                savedAt = System.currentTimeMillis()
            )
        }
    }
}
