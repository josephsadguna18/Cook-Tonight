package com.example.data.service

import android.util.Log
import com.example.data.model.Ingredient
import com.example.data.model.Recipe
import com.example.data.model.SubstituteOption
import com.example.data.repository.RecipeCacheRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiRecipeService"

class GeminiRecipeService {

    // 1. Strict JSON schema for 6 recipes
    private val recipeResponseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "recipes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(description = "Dish title"),
                            "cuisine" to Schema.string(description = "Cuisine name"),
                            "time" to Schema.string(description = "Total preparation & cooking time"),
                            "serves" to Schema.string(description = "Servings e.g. 2-4"),
                            "steps" to Schema.array(
                                items = Schema.string(),
                                description = "Numbered step by step cooking directions"
                            ),
                            "ingredients" to Schema.array(
                                items = Schema.obj(
                                    properties = mapOf(
                                        "name" to Schema.string(description = "Name of the ingredient"),
                                        "quantity" to Schema.string(description = "Quantity with unit (e.g., 250g, 2 tbsp)")
                                    )
                                ),
                                description = "List of ingredients with quantities"
                            )
                        )
                    ),
                    description = "List of exactly 6 curated delicious recipes"
                )
            )
        )
    }

    // 2. Strict JSON schema for ingredient substitution
    private val substituteResponseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "substitutes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(description = "Realistic substitute ingredient name"),
                            "note" to Schema.string(description = "One-line explanation of how the taste or texture changes in the dish")
                        )
                    ),
                    description = "Exactly 3 realistic substitutes"
                )
            )
        )
    }

    // Generative Model for Recipe generation
    private val recipeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recipeResponseSchema
            }
        )
    }

    // Generative Model for Substitutes
    private val substituteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = substituteResponseSchema
            }
        )
    }

    /**
     * Generate 6 recipes as structured JSON for a given cuisine or dish.
     * Returns Pair<List<Recipe>, rawJsonString>
     */
    suspend fun generateRecipes(query: String, cuisine: String?): Pair<List<Recipe>, String> {
        val prompt = buildString {
            append("You are a professional chef. ")
            if (!cuisine.isNullOrBlank()) {
                append("Generate exactly 6 authentic, delicious, and easy-to-cook recipes for '$cuisine' cuisine. ")
            } else {
                append("Generate exactly 6 distinct, delicious recipe ideas for '$query'. ")
            }
            append("Ensure each recipe has a descriptive name, accurate time, serving count, clear step-by-step cooking steps, and detailed ingredients with standard measurement quantities. Return JSON matching the schema.")
        }

        try {
            val response = withTimeout(60_000L) {
                recipeModel.generateContent(prompt)
            }
            val text = response.text ?: throw IllegalStateException("Gemini returned empty response text.")
            Log.d(TAG, "Received raw recipe JSON: ${text.take(200)}...")

            val recipes = RecipeCacheRepository.parseRecipesJson(text)
            if (recipes.isEmpty()) {
                throw IllegalStateException("Could not parse recipes from model output.")
            }
            return Pair(recipes, text)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Recipe generation timed out after 60s", e)
            throw IllegalStateException("Recipe generation timed out. Please check your network and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini recipe generation failed", e)
            throw e
        }
    }

    /**
     * Generate 3 realistic substitutes for an ingredient in a recipe for a user in location.
     */
    suspend fun getSubstitutes(
        ingredientName: String,
        recipeName: String,
        cuisine: String,
        userLocation: String
    ): List<SubstituteOption> {
        val prompt = buildString {
            append("The user wants to cook '$recipeName' ($cuisine cuisine). ")
            append("They do NOT have the ingredient '$ingredientName'. ")
            append("The cook's location is: '$userLocation'. ")
            append("Suggest exactly 3 realistic, easily accessible substitute ingredients available locally in $userLocation. ")
            append("For each substitute, provide a concise one-line note on how the taste or texture of the dish changes. Return JSON matching the schema.")
        }

        try {
            val response = withTimeout(45_000L) {
                substituteModel.generateContent(prompt)
            }
            val text = response.text ?: throw IllegalStateException("Model returned empty text for substitutes.")
            val obj = JSONObject(text)
            val arr = obj.optJSONArray("substitutes") ?: return emptyList()

            val list = mutableListOf<SubstituteOption>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    SubstituteOption(
                        name = item.optString("name", "Alternative"),
                        note = item.optString("note", "Alters taste or texture slightly.")
                    )
                )
            }
            return list
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Substitute generation timed out", e)
            throw IllegalStateException("Substitute search timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini substitute generation failed", e)
            throw e
        }
    }
}
