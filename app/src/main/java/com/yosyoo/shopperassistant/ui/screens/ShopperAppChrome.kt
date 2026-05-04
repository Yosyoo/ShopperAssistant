package com.yosyoo.shopperassistant.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.yosyoo.shopperassistant.R
import com.yosyoo.shopperassistant.ui.ShopperDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShopperTopBar() {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
internal fun ShopperNavigationBar(
    selectedDestination: ShopperDestination,
    onDestinationSelected: (ShopperDestination) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedDestination == ShopperDestination.Scan,
            onClick = { onDestinationSelected(ShopperDestination.Scan) },
            icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
            label = { Text("扫码生成") },
        )
        NavigationBarItem(
            selected = selectedDestination == ShopperDestination.Expiry,
            onClick = { onDestinationSelected(ShopperDestination.Expiry) },
            icon = { Icon(Icons.Outlined.Event, contentDescription = null) },
            label = { Text("保质期检查") },
        )
    }
}
