package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CookTonightDatabase
import com.example.data.model.GroceryStore
import com.example.data.model.IngredientItemState
import com.example.data.model.Recipe
import com.example.data.repository.FavoriteRecipeRepository
import com.example.data.repository.RecipeCacheRepository
import com.example.data.service.GeminiRecipeService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "CookTonightViewModel"

sealed interface RecipeListUiState {
    data object Idle : RecipeListUiState
    data class Loading(val queryText: String) : RecipeListUiState
    data class Success(val query: String, val recipes: List<Recipe>, val fromCache: Boolean) : RecipeListUiState
    data class Error(val message: String) : RecipeListUiState
}

data class BasketTotals(
    val blinkitTotal: Double = 0.0,
    val zeptoTotal: Double = 0.0,
    val instamartTotal: Double = 0.0,
    val cheapestStore: GroceryStore? = null,
    val missingItemsCount: Int = 0
)

enum class HomeTab {
    DISCOVER,
    FAVORITES
}

@OptIn(ExperimentalCoroutinesApi::class)
class CookTonightViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheRepository = RecipeCacheRepository(application.applicationContext)
    private val favoriteRepository = FavoriteRecipeRepository(
        CookTonightDatabase.getDatabase(application).favoriteRecipeDao()
    )
    private val geminiService = GeminiRecipeService()

    // Room Database favorites reactive stream
    val favoriteRecipes: StateFlow<List<Recipe>> = favoriteRepository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(HomeTab.DISCOVER)
    val selectedTab: StateFlow<HomeTab> = _selectedTab.asStateFlow()

    fun setSelectedTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    // 1. Cuisines predefined list
    val cuisines = listOf(
        "Indian", "Italian", "Chinese", "Japanese", "Mexican",
        "Thai", "French", "Korean", "Middle Eastern"
    )

    // 2. Locations dropdown
    val locations = listOf(
        "India (Bengaluru)",
        "India (Mumbai)",
        "India (Delhi NCR)",
        "India (Hyderabad)",
        "India (Chennai)",
        "India (Kolkata)",
        "India (Pune)",
        "United States (New York)",
        "United Kingdom (London)",
        "Canada (Toronto)",
        "Australia (Sydney)",
        "UAE (Dubai)"
    )

    private val _selectedLocation = MutableStateFlow("India (Bengaluru)")
    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _recipeListState = MutableStateFlow<RecipeListUiState>(RecipeListUiState.Idle)
    val recipeListState: StateFlow<RecipeListUiState> = _recipeListState.asStateFlow()

    private val _activeRecipe = MutableStateFlow<Recipe?>(null)
    val activeRecipe: StateFlow<Recipe?> = _activeRecipe.asStateFlow()

    // Observe whether active recipe is locally favorited in Room
    val isActiveRecipeFavorite: StateFlow<Boolean> = _activeRecipe
        .flatMapLatest { recipe ->
            if (recipe != null) favoriteRepository.isFavorite(recipe)
            else flowOf(false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _ingredientStates = MutableStateFlow<List<IngredientItemState>>(emptyList())
    val ingredientStates: StateFlow<List<IngredientItemState>> = _ingredientStates.asStateFlow()

    private val _basketTotals = MutableStateFlow(BasketTotals())
    val basketTotals: StateFlow<BasketTotals> = _basketTotals.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCuisine = MutableStateFlow<String?>(null)
    val selectedCuisine: StateFlow<String?> = _selectedCuisine.asStateFlow()

    init {
        // Automatically fetch default suggestions for Indian cuisine on first load
        selectCuisine("Indian")
    }

    fun setLocation(loc: String) {
        _selectedLocation.value = loc
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCuisine(cuisine: String) {
        _selectedCuisine.value = cuisine
        _searchQuery.value = ""
        fetchRecipes(query = cuisine, cuisine = cuisine, forceRefresh = false)
    }

    fun searchDish(dishQuery: String) {
        if (dishQuery.isBlank()) return
        _selectedCuisine.value = null
        fetchRecipes(query = dishQuery.trim(), cuisine = null, forceRefresh = false)
    }

    fun refreshCurrentSearch() {
        val currentCuisine = _selectedCuisine.value
        val currentQuery = _searchQuery.value
        val query = currentCuisine ?: currentQuery.ifBlank { "Indian" }
        fetchRecipes(query = query, cuisine = currentCuisine, forceRefresh = true)
    }

    fun toggleFavorite(recipe: Recipe, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(recipe, currentlyFavorite)
        }
    }

    fun isRecipeFavorite(recipe: Recipe): Flow<Boolean> {
        return favoriteRepository.isFavorite(recipe)
    }

    private fun fetchRecipes(query: String, cuisine: String?, forceRefresh: Boolean) {
        viewModelScope.launch {
            _recipeListState.value = RecipeListUiState.Loading(query)
            try {
                // Step 1: Check Firestore cache first if not forced refresh
                if (!forceRefresh) {
                    val cached = cacheRepository.getCachedRecipes(query)
                    if (!cached.isNullOrEmpty()) {
                        Log.d(TAG, "Loaded ${cached.size} recipes from Firestore cache for '$query'")
                        _recipeListState.value = RecipeListUiState.Success(
                            query = query,
                            recipes = cached,
                            fromCache = true
                        )
                        return@launch
                    }
                }

                // Step 2: Cache miss or force refresh: Call Gemini API
                Log.d(TAG, "Calling Gemini AI for '$query' (cuisine: $cuisine)...")
                val (recipes, rawJson) = geminiService.generateRecipes(query, cuisine)

                // Step 3: Cache in Firestore for instant future retrieval
                cacheRepository.saveCachedRecipes(
                    query = query,
                    cuisine = cuisine ?: "Various",
                    recipes = recipes,
                    rawJson = rawJson
                )

                _recipeListState.value = RecipeListUiState.Success(
                    query = query,
                    recipes = recipes,
                    fromCache = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load recipes for '$query'", e)
                _recipeListState.value = RecipeListUiState.Error(
                    e.localizedMessage ?: "Failed to generate recipes. Please try again."
                )
            }
        }
    }

    fun openRecipe(recipe: Recipe) {
        _activeRecipe.value = recipe
        // Initialize ingredients checklist: default "I have this" = false
        _ingredientStates.value = recipe.ingredients.map { ingredient ->
            IngredientItemState(
                ingredient = ingredient,
                isHave = false,
                substitutes = emptyList(),
                isLoadingSubstitutes = false,
                substituteError = null,
                priceBlinkit = null,
                priceZepto = null,
                priceInstamart = null
            )
        }
        recalculateTotals()
    }

    fun closeRecipe() {
        _activeRecipe.value = null
        _ingredientStates.value = emptyList()
        _basketTotals.value = BasketTotals()
    }

    fun toggleHaveIngredient(index: Int) {
        val currentList = _ingredientStates.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            currentList[index] = item.copy(isHave = !item.isHave)
            _ingredientStates.value = currentList
            recalculateTotals()
        }
    }

    fun requestSubstitutes(index: Int) {
        val currentRecipe = _activeRecipe.value ?: return
        val currentList = _ingredientStates.value.toMutableList()
        if (index !in currentList.indices) return

        val targetItem = currentList[index]
        currentList[index] = targetItem.copy(isLoadingSubstitutes = true, substituteError = null)
        _ingredientStates.value = currentList

        viewModelScope.launch {
            try {
                val substitutes = geminiService.getSubstitutes(
                    ingredientName = targetItem.ingredient.name,
                    recipeName = currentRecipe.name,
                    cuisine = currentRecipe.cuisine,
                    userLocation = _selectedLocation.value
                )
                val updated = _ingredientStates.value.toMutableList()
                if (index in updated.indices) {
                    updated[index] = updated[index].copy(
                        substitutes = substitutes,
                        isLoadingSubstitutes = false,
                        substituteError = null
                    )
                    _ingredientStates.value = updated
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch substitutes", e)
                val updated = _ingredientStates.value.toMutableList()
                if (index in updated.indices) {
                    updated[index] = updated[index].copy(
                        isLoadingSubstitutes = false,
                        substituteError = e.localizedMessage ?: "Could not find substitutes."
                    )
                    _ingredientStates.value = updated
                }
            }
        }
    }

    fun updatePrice(index: Int, store: GroceryStore, priceText: String) {
        val currentList = _ingredientStates.value.toMutableList()
        if (index !in currentList.indices) return

        val parsedPrice = priceText.toDoubleOrNull()
        val item = currentList[index]
        val updatedItem = when (store) {
            GroceryStore.BLINKIT -> item.copy(priceBlinkit = parsedPrice)
            GroceryStore.ZEPTO -> item.copy(priceZepto = parsedPrice)
            GroceryStore.INSTAMART -> item.copy(priceInstamart = parsedPrice)
        }
        currentList[index] = updatedItem
        _ingredientStates.value = currentList
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val missingItems = _ingredientStates.value.filter { !it.isHave }

        var blinkitSum = 0.0
        var zeptoSum = 0.0
        var instamartSum = 0.0

        var hasBlinkitPrice = false
        var hasZeptoPrice = false
        var hasInstamartPrice = false

        for (item in missingItems) {
            item.priceBlinkit?.let {
                blinkitSum += it
                hasBlinkitPrice = true
            }
            item.priceZepto?.let {
                zeptoSum += it
                hasZeptoPrice = true
            }
            item.priceInstamart?.let {
                instamartSum += it
                hasInstamartPrice = true
            }
        }

        // Determine cheapest store among stores that have prices entered
        val validStores = mutableListOf<Pair<GroceryStore, Double>>()
        if (hasBlinkitPrice && blinkitSum > 0) validStores.add(GroceryStore.BLINKIT to blinkitSum)
        if (hasZeptoPrice && zeptoSum > 0) validStores.add(GroceryStore.ZEPTO to zeptoSum)
        if (hasInstamartPrice && instamartSum > 0) validStores.add(GroceryStore.INSTAMART to instamartSum)

        val cheapest = validStores.minByOrNull { it.second }?.first ?: if (missingItems.isNotEmpty()) GroceryStore.BLINKIT else null

        _basketTotals.value = BasketTotals(
            blinkitTotal = blinkitSum,
            zeptoTotal = zeptoSum,
            instamartTotal = instamartSum,
            cheapestStore = cheapest,
            missingItemsCount = missingItems.size
        )
    }

    fun getShoppingListText(): String {
        val recipe = _activeRecipe.value
        val missing = _ingredientStates.value.filter { !it.isHave }
        if (missing.isEmpty()) return "All ingredients in stock for ${recipe?.name ?: "recipe"}!"

        return buildString {
            append("Cook Tonight Shopping List")
            if (recipe != null) {
                append(" for ${recipe.name} (${recipe.cuisine})")
            }
            append(":\n\n")
            missing.forEachIndexed { i, item ->
                append("${i + 1}. ${item.ingredient.name} - ${item.ingredient.quantity}\n")
            }
            val totals = _basketTotals.value
            if (totals.cheapestStore != null && (totals.blinkitTotal > 0 || totals.zeptoTotal > 0 || totals.instamartTotal > 0)) {
                append("\nTarget store: ${totals.cheapestStore.displayName}")
            }
        }
    }
}
