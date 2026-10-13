package com.example.irblast

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.irblast.ui.screens.browse.BrowseScreen
import com.example.irblast.ui.screens.codefinder.CodeFinderScreen
import com.example.irblast.ui.screens.custom.CustomSignalScreen
import com.example.irblast.ui.screens.home.HomeScreen
import com.example.irblast.ui.screens.remote.RemoteScreen
import com.example.irblast.ui.screens.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as IrBlastApplication
    val container = app.container

    val backStack = rememberNavBackStack(Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = Modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    container = container,
                    onNavigate = { navKey -> backStack.add(navKey) }
                )
            }
            entry<RemoteControl> { key ->
                RemoteScreen(
                    remoteId = key.remoteId,
                    container = container,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToBrowse = { backStack.add(Browse()) }
                )
            }
            entry<Browse> { key ->
                BrowseScreen(
                    container = container,
                    initialCategory = key.initialCategory,
                    onBack = { backStack.removeLastOrNull() },
                    onRemoteImported = { newId ->
                        backStack.add(RemoteControl(newId))
                    }
                )
            }
            entry<CodeFinder> { key ->
                CodeFinderScreen(
                    container = container,
                    initialCategory = key.initialCategory,
                    initialBrand = key.initialBrand,
                    onBack = { backStack.removeLastOrNull() },
                    onRemoteImported = { newId ->
                        backStack.add(RemoteControl(newId))
                    }
                )
            }
            entry<CustomSignal> {
                CustomSignalScreen(
                    container = container,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToRemote = { newId ->
                        backStack.add(RemoteControl(newId))
                    }
                )
            }
            entry<Settings> {
                SettingsScreen(
                    container = container,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
