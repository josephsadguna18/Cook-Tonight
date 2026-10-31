package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CachedRecipeGroupEntity
import com.example.data.model.Ingredient
import com.example.data.model.Recipe
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RecipeCacheRepo"

class RecipeCacheRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access Firestore.")
    }

    /**
     * Look up cached recipes for a specific query or cuisine key.
     * Returns list of recipes if found, or null if cache miss.
     */
    suspend fun getCachedRecipes(queryKey: String): List<Recipe>? {
        val uid = requireUserId()
        val sanitizedKey = sanitizeCacheKey(queryKey)
        val docRef = db.collection("users").document(uid).collection("cached_recipes").document(sanitizedKey)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val entity = snapshot.toObject(CachedRecipeGroupEntity::class.java)
                if (entity != null && entity.recipesJson.isNotBlank()) {
                    Log.d(TAG, "Cache HIT for '$queryKey'")
                    parseRecipesJson(entity.recipesJson)
                } else null
            } else {
                Log.d(TAG, "Cache MISS for '$queryKey'")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cache for '$queryKey'", e)
            null
        }
    }

    /**
     * Cache 6 generated recipes in Firestore for this query.
     */
    suspend fun saveCachedRecipes(query: String, cuisine: String, recipes: List<Recipe>, rawJson: String) {
        val uid = requireUserId()
        val sanitizedKey = sanitizeCacheKey(query)
        val docRef = db.collection("users").document(uid).collection("cached_recipes").document(sanitizedKey)

        try {
            val payload = mapOf(
                "query" to query.trim().lowercase(),
                "cuisine" to cuisine.ifBlank { "General" },
                "recipesJson" to rawJson,
                "userId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Log.d(TAG, "Successfully cached ${recipes.size} recipes for key '$sanitizedKey'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write cache for '$query'", e)
        }
    }

    fun observeCachedRecipes(): Flow<List<CachedRecipeGroupEntity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("cached_recipes")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CachedRecipeGroupEntity::class.java) }
                .catch { e ->
                    Log.e(TAG, "Error observing cached recipes", e)
                    throw e
                }
        )
    }

    companion object {
        fun sanitizeCacheKey(key: String): String {
            val normalized = key.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            return if (normalized.isBlank()) "default" else normalized.take(100)
        }

        fun parseRecipesJson(rawJson: String): List<Recipe> {
            val list = mutableListOf<Recipe>()
            try {
                val trimmed = rawJson.trim()
                val array: JSONArray = if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    when {
                        obj.has("recipes") -> obj.getJSONArray("recipes")
                        obj.has("data") -> obj.getJSONArray("data")
                        else -> JSONArray()
                    }
                } else if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    JSONArray()
                }

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val name = item.optString("name", "Delicious Recipe")
                    val cuisine = item.optString("cuisine", "")
                    val time = item.optString("time", "30 mins")
                    val serves = item.optString("serves", "2-4")

                    val stepsList = mutableListOf<String>()
                    val stepsArr = item.optJSONArray("steps")
                    if (stepsArr != null) {
                        for (s in 0 until stepsArr.length()) {
                            stepsList.add(stepsArr.optString(s))
                        }
                    }

                    val ingredientsList = mutableListOf<Ingredient>()
                    val ingArr = item.optJSONArray("ingredients")
                    if (ingArr != null) {
                        for (ingIdx in 0 until ingArr.length()) {
                            val ingObj = ingArr.getJSONObject(ingIdx)
                            ingredientsList.add(
                                Ingredient(
                                    name = ingObj.optString("name", ""),
                                    quantity = ingObj.optString("quantity", "")
                                )
                            )
                        }
                    }

                    list.add(
                        Recipe(
                            id = "rec_${System.currentTimeMillis()}_$i",
                            name = name,
                            cuisine = cuisine,
                            time = time,
                            serves = serves,
                            steps = stepsList,
                            ingredients = ingredientsList
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing recipe JSON", e)
            }
            return list
        }
    }
}
