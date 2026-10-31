package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthScreen
import com.example.ui.CookTonightViewModel
import com.example.ui.HomeScreen
import com.example.ui.RecipeDetailScreen
import com.example.ui.performSignOut
import com.example.ui.theme.CookTonightTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with intent carrying debug token
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            CookTonightTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CookTonightRoot()
                }
            }
        }
    }
}

@Composable
fun CookTonightRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val viewModel: CookTonightViewModel = viewModel()
        val activeRecipe by viewModel.activeRecipe.collectAsState()

        if (activeRecipe != null) {
            RecipeDetailScreen(
                recipe = activeRecipe!!,
                viewModel = viewModel,
                onBack = { viewModel.closeRecipe() }
            )
        } else {
            HomeScreen(
                viewModel = viewModel,
                onSignOutRequested = {
                    performSignOut(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            currentUser = null
                        },
                        scope = scope
                    )
                }
            )
        }
    }
}
