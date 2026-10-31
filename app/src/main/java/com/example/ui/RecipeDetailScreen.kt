package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroceryStore
import com.example.data.model.IngredientItemState
import com.example.data.model.Recipe
import com.example.ui.theme.BlinkitColor
import com.example.ui.theme.InstamartColor
import com.example.ui.theme.ZeptoColor
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    viewModel: CookTonightViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ingredientStates by viewModel.ingredientStates.collectAsState()
    val basketTotals by viewModel.basketTotals.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isFavorite by viewModel.isActiveRecipeFavorite.collectAsState()

    var showLocationMenu by remember { mutableStateOf(false) }

    // BackHandler to safely return to recipe list
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recipe.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("recipe_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to recipes"
                        )
                    }
                },
                actions = {
                    // Location dropdown button
                    Box {
                        OutlinedButton(
                            onClick = { showLocationMenu = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                                .testTag("location_dropdown_button"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedLocation.substringBefore("(").trim(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        DropdownMenu(
                            expanded = showLocationMenu,
                            onDismissRequest = { showLocationMenu = false }
                        ) {
                            viewModel.locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        viewModel.setLocation(loc)
                                        showLocationMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Favorite toggle button
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(recipe, isFavorite)
                            val msg = if (isFavorite) "Removed from favorites" else "Saved to favorites!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("recipe_favorite_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites",
                            tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Persistent Bottom Bar for "Order on <cheapest store>"
            if (basketTotals.missingItemsCount > 0) {
                OrderOnCheapestBar(
                    basketTotals = basketTotals,
                    onOrderClicked = { targetStore ->
                        val shoppingText = viewModel.getShoppingListText()
                        copyToClipboard(context, shoppingText)
                        Toast.makeText(context, "Shopping list copied to clipboard!", Toast.LENGTH_SHORT).show()
                        openStoreUrl(context, targetStore.baseUrl)
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Recipe Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipe_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = recipe.cuisine,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = recipe.time,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Serves ${recipe.serves}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Current Substitution Location: $selectedLocation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Ingredients Checklist
            Text(
                text = "1. Ingredients Checklist",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Check 'I have this' for items in your kitchen. Missing items will appear in the store comparison below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ingredientStates.forEachIndexed { index, itemState ->
                IngredientCardItem(
                    index = index,
                    state = itemState,
                    onToggleHave = { viewModel.toggleHaveIngredient(index) },
                    onRequestSubstitutes = { viewModel.requestSubstitutes(index) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Missing Items & Store Price Comparison Table
            MissingItemsPriceSection(
                ingredientStates = ingredientStates,
                basketTotals = basketTotals,
                onPriceChanged = { index, store, price ->
                    viewModel.updatePrice(index, store, price)
                },
                onOpenStoreSearch = { store, ingredientName ->
                    val encoded = URLEncoder.encode(ingredientName, "UTF-8")
                    val searchUrl = String.format(store.searchUrlPattern, encoded)
                    openStoreUrl(context, searchUrl)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Section 3: Preparation Steps
            Text(
                text = "3. Step-by-Step Directions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    recipe.steps.forEachIndexed { stepIdx, stepText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${stepIdx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stepText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (stepIdx < recipe.steps.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bottom padding for persistent bottom bar
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun IngredientCardItem(
    index: Int,
    state: IngredientItemState,
    onToggleHave: () -> Unit,
    onRequestSubstitutes: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ingredient_item_$index"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isHave) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (state.isHave) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isHave,
                    onCheckedChange = { onToggleHave() },
                    modifier = Modifier.testTag("checkbox_have_$index"),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.secondary
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleHave() }
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = state.ingredient.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (state.isHave) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (state.isHave) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = state.ingredient.quantity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.isHave) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "In Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // "Can't find it near me?" button
                    OutlinedButton(
                        onClick = onRequestSubstitutes,
                        enabled = !state.isLoadingSubstitutes,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("cant_find_btn_$index"),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        if (state.isLoadingSubstitutes) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Can't find it?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Substitute results if requested
            AnimatedVisibility(
                visible = !state.isHave && (state.substitutes.isNotEmpty() || state.substituteError != null),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Realistic Substitutes (for your location):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    state.substituteError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    state.substitutes.forEachIndexed { sIdx, sub ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = sub.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingItemsPriceSection(
    ingredientStates: List<IngredientItemState>,
    basketTotals: BasketTotals,
    onPriceChanged: (index: Int, store: GroceryStore, priceText: String) -> Unit,
    onOpenStoreSearch: (store: GroceryStore, ingredientName: String) -> Unit
) {
    val missingItemsWithIndices = ingredientStates
        .mapIndexed { index, state -> Pair(index, state) }
        .filter { !it.second.isHave }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("missing_items_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "2. Missing Items & Store Price Comparison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${missingItemsWithIndices.size} item(s) to buy. Tap store link to search and enter prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (missingItemsWithIndices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You have all ingredients! Ready to cook.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                // Table layout: horizontal scroll for store price inputs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item & Qty",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(140.dp)
                        )
                        StoreColumnHeader(
                            store = GroceryStore.BLINKIT,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.BLINKIT && basketTotals.blinkitTotal > 0
                        )
                        StoreColumnHeader(
                            store = GroceryStore.ZEPTO,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.ZEPTO && basketTotals.zeptoTotal > 0
                        )
                        StoreColumnHeader(
                            store = GroceryStore.INSTAMART,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.INSTAMART && basketTotals.instamartTotal > 0
                        )
                    }

                    HorizontalDivider()

                    // Ingredient Rows
                    missingItemsWithIndices.forEach { (origIndex, itemState) ->
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(140.dp)) {
                                Text(
                                    text = itemState.ingredient.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = itemState.ingredient.quantity,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Blinkit column
                            PriceCellWithLink(
                                store = GroceryStore.BLINKIT,
                                price = itemState.priceBlinkit,
                                onOpenLink = { onOpenStoreSearch(GroceryStore.BLINKIT, itemState.ingredient.name) },
                                onPriceChanged = { onPriceChanged(origIndex, GroceryStore.BLINKIT, it) },
                                isStoreCheapest = basketTotals.cheapestStore == GroceryStore.BLINKIT
                            )

                            // Zepto column
                            PriceCellWithLink(
                                store = GroceryStore.ZEPTO,
                                price = itemState.priceZepto,
                                onOpenLink = { onOpenStoreSearch(GroceryStore.ZEPTO, itemState.ingredient.name) },
                                onPriceChanged = { onPriceChanged(origIndex, GroceryStore.ZEPTO, it) },
                                isStoreCheapest = basketTotals.cheapestStore == GroceryStore.ZEPTO
                            )

                            // Instamart column
                            PriceCellWithLink(
                                store = GroceryStore.INSTAMART,
                                price = itemState.priceInstamart,
                                onOpenLink = { onOpenStoreSearch(GroceryStore.INSTAMART, itemState.ingredient.name) },
                                onPriceChanged = { onPriceChanged(origIndex, GroceryStore.INSTAMART, it) },
                                isStoreCheapest = basketTotals.cheapestStore == GroceryStore.INSTAMART
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }

                    // Total Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Basket Total",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(140.dp)
                        )

                        StoreTotalCell(
                            store = GroceryStore.BLINKIT,
                            total = basketTotals.blinkitTotal,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.BLINKIT && basketTotals.blinkitTotal > 0
                        )

                        StoreTotalCell(
                            store = GroceryStore.ZEPTO,
                            total = basketTotals.zeptoTotal,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.ZEPTO && basketTotals.zeptoTotal > 0
                        )

                        StoreTotalCell(
                            store = GroceryStore.INSTAMART,
                            total = basketTotals.instamartTotal,
                            isCheapest = basketTotals.cheapestStore == GroceryStore.INSTAMART && basketTotals.instamartTotal > 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreColumnHeader(
    store: GroceryStore,
    isCheapest: Boolean
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = store.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = when (store) {
                    GroceryStore.BLINKIT -> Color(0xFF8D6E00)
                    GroceryStore.ZEPTO -> ZeptoColor
                    GroceryStore.INSTAMART -> InstamartColor
                }
            )
            if (isCheapest) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = "CHEAPEST",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceCellWithLink(
    store: GroceryStore,
    price: Double?,
    onOpenLink: () -> Unit,
    onPriceChanged: (String) -> Unit,
    isStoreCheapest: Boolean
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick store search link
        OutlinedButton(
            onClick = onOpenLink,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            shape = RoundedCornerShape(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Search ${store.displayName}",
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Search",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Price input
        OutlinedTextField(
            value = if (price == null || price == 0.0) "" else price.toString().removeSuffix(".0"),
            onValueChange = { onPriceChanged(it) },
            placeholder = { Text("₹ price", style = MaterialTheme.typography.labelSmall) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isStoreCheapest) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun StoreTotalCell(
    store: GroceryStore,
    total: Double,
    isCheapest: Boolean
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCheapest) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                )
                .padding(vertical = 4.dp, horizontal = 6.dp)
        ) {
            Text(
                text = if (total > 0) "₹%.0f".format(total) else "—",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isCheapest) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
            if (isCheapest) {
                Text(
                    text = "Lowest Basket",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }
    }
}

@Composable
private fun OrderOnCheapestBar(
    basketTotals: BasketTotals,
    onOrderClicked: (targetStore: GroceryStore) -> Unit
) {
    val targetStore = basketTotals.cheapestStore ?: GroceryStore.BLINKIT
    val targetTotal = when (targetStore) {
        GroceryStore.BLINKIT -> basketTotals.blinkitTotal
        GroceryStore.ZEPTO -> basketTotals.zeptoTotal
        GroceryStore.INSTAMART -> basketTotals.instamartTotal
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${basketTotals.missingItemsCount} missing item(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (targetTotal > 0) {
                    Text(
                        text = "Lowest Basket: ₹%.0f".format(targetTotal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }

            Button(
                onClick = { onOrderClicked(targetStore) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("order_on_cheapest_store_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (targetStore) {
                        GroceryStore.BLINKIT -> BlinkitColor
                        GroceryStore.ZEPTO -> ZeptoColor
                        GroceryStore.INSTAMART -> InstamartColor
                    },
                    contentColor = if (targetStore == GroceryStore.BLINKIT) Color.Black else Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Order on ${targetStore.displayName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Cook Tonight Shopping List", text)
    clipboard.setPrimaryClip(clip)
}

private fun openStoreUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser for: $url", Toast.LENGTH_SHORT).show()
    }
}
