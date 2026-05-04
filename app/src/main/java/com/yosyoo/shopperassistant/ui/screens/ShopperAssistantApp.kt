package com.yosyoo.shopperassistant.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yosyoo.shopperassistant.ui.ShopperDestination
import com.yosyoo.shopperassistant.ui.ShopperViewModel

@Composable
fun ShopperAssistantApp(
    viewModel: ShopperViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ShopperTopBar() },
        bottomBar = {
            ShopperNavigationBar(
                selectedDestination = uiState.selectedDestination,
                onDestinationSelected = viewModel::selectDestination,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            ScanGenerateRoute(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.destinationLayer(
                    visible = uiState.selectedDestination == ShopperDestination.Scan,
                ),
            )
            ExpiryCheckScreen(
                uiState = uiState,
                onProductionDateSelected = viewModel::onProductionDateSelected,
                onShelfLifeDaysChanged = viewModel::onShelfLifeDaysChanged,
                modifier = Modifier.destinationLayer(
                    visible = uiState.selectedDestination == ShopperDestination.Expiry,
                ),
            )
        }
    }
}

private fun Modifier.destinationLayer(visible: Boolean): Modifier {
    return fillMaxSize()
        .alpha(if (visible) 1f else 0f)
        .zIndex(if (visible) 1f else 0f)
}
