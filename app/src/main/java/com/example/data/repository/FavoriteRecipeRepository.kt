package com.example.data.repository

import com.example.data.local.FavoriteRecipeDao
import com.example.data.local.FavoriteRecipeEntity
import com.example.data.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRecipeRepository(private val dao: FavoriteRecipeDao) {

    val allFavorites: Flow<List<Recipe>> = dao.getAllFavorites().map { entities ->
        entities.map { it.toRecipe() }
    }

    fun isFavorite(recipe: Recipe): Flow<Boolean> {
        val stableId = if (recipe.id.isNotBlank()) {
            recipe.id
        } else {
            "${recipe.cuisine}_${recipe.name}".lowercase().replace(Regex("[^a-z0-9]"), "_")
        }
        return dao.isFavoriteByName(recipe.name)
    }

    suspend fun toggleFavorite(recipe: Recipe, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            dao.deleteFavoriteByName(recipe.name)
        } else {
            val entity = FavoriteRecipeEntity.fromRecipe(recipe)
            dao.insertFavorite(entity)
        }
    }

    suspend fun removeFavoriteByName(name: String) {
        dao.deleteFavoriteByName(name)
    }
}
