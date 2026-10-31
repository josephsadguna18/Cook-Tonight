package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CookTonightDatabase
import com.example.data.local.FavoriteRecipeEntity
import com.example.data.model.GroceryStore
import com.example.data.model.Ingredient
import com.example.data.model.Recipe
import com.example.data.repository.FavoriteRecipeRepository
import com.example.data.repository.RecipeCacheRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URLEncoder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CookTonightUnitTest {

    private lateinit var db: CookTonightDatabase
    private lateinit var favoriteRepository: FavoriteRecipeRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CookTonightDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        favoriteRepository = FavoriteRecipeRepository(db.favoriteRecipeDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun sanitizeCacheKey_normalizesCorrectly() {
        assertEquals("indian", RecipeCacheRepository.sanitizeCacheKey("Indian"))
        assertEquals("butter_chicken", RecipeCacheRepository.sanitizeCacheKey("Butter Chicken!"))
        assertEquals("pad_thai", RecipeCacheRepository.sanitizeCacheKey("Pad Thai"))
        assertEquals("default", RecipeCacheRepository.sanitizeCacheKey("   "))
    }

    @Test
    fun parseRecipesJson_parsesStandardRecipeFormat() {
        val sampleJson = """
            {
              "recipes": [
                {
                  "name": "Paneer Tikka",
                  "cuisine": "Indian",
                  "time": "25 mins",
                  "serves": "2-3",
                  "steps": ["Cube paneer", "Marinate in spices", "Grill till golden"],
                  "ingredients": [
                    {"name": "Paneer", "quantity": "250g"},
                    {"name": "Yogurt", "quantity": "3 tbsp"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = RecipeCacheRepository.parseRecipesJson(sampleJson)
        assertEquals(1, parsed.size)
        val recipe = parsed[0]
        assertEquals("Paneer Tikka", recipe.name)
        assertEquals("Indian", recipe.cuisine)
        assertEquals(3, recipe.steps.size)
        assertEquals(2, recipe.ingredients.size)
        assertEquals("Paneer", recipe.ingredients[0].name)
    }

    @Test
    fun groceryStore_generatesValidSearchUrls() {
        val ingredient = "Greek Yogurt"
        val encoded = URLEncoder.encode(ingredient, "UTF-8")

        val blinkitUrl = String.format(GroceryStore.BLINKIT.searchUrlPattern, encoded)
        assertTrue(blinkitUrl.contains("blinkit.com/s/?q=Greek+Yogurt"))

        val zeptoUrl = String.format(GroceryStore.ZEPTO.searchUrlPattern, encoded)
        assertTrue(zeptoUrl.contains("zepto.com/search?query=Greek+Yogurt"))

        val instamartUrl = String.format(GroceryStore.INSTAMART.searchUrlPattern, encoded)
        assertTrue(instamartUrl.contains("swiggy.com/instamart/search"))
    }

    @Test
    fun roomDatabase_favoriteRecipesTogglingAndRetrieval() = runBlocking {
        val recipe = Recipe(
            id = "fav_1",
            name = "Classic Margherita Pizza",
            cuisine = "Italian",
            time = "20 mins",
            serves = "2",
            steps = listOf("Roll dough", "Add tomato sauce", "Bake at 250C"),
            ingredients = listOf(
                Ingredient("Flour", "200g"),
                Ingredient("Mozzarella", "150g")
            )
        )

        // Initially not favorite
        val initialIsFav = favoriteRepository.isFavorite(recipe).first()
        assertFalse(initialIsFav)

        // Toggle on: save to Room
        favoriteRepository.toggleFavorite(recipe, isCurrentlyFavorite = false)

        val updatedIsFav = favoriteRepository.isFavorite(recipe).first()
        assertTrue(updatedIsFav)

        val allFavorites = favoriteRepository.allFavorites.first()
        assertEquals(1, allFavorites.size)
        assertEquals("Classic Margherita Pizza", allFavorites[0].name)
        assertEquals(2, allFavorites[0].ingredients.size)
        assertEquals("Flour", allFavorites[0].ingredients[0].name)

        // Toggle off: remove from Room
        favoriteRepository.toggleFavorite(recipe, isCurrentlyFavorite = true)

        val finalIsFav = favoriteRepository.isFavorite(recipe).first()
        assertFalse(finalIsFav)
        val emptyFavorites = favoriteRepository.allFavorites.first()
        assertEquals(0, emptyFavorites.size)
    }
}
