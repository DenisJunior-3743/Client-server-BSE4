package com.trustbank.loanapp.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.common.hexToColor
import com.trustbank.loanapp.ui.components.AppHeader
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.screens.applications.ApplicationsScreen
import com.trustbank.loanapp.ui.screens.home.HomeScreen
import com.trustbank.loanapp.ui.screens.products.ProductsScreen
import com.trustbank.loanapp.ui.screens.profile.ProfileScreen
import com.trustbank.loanapp.ui.theme.AppColors

private data class BottomTab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    BottomTab("Home", Icons.Filled.Home),
    BottomTab("Products", Icons.Filled.LocalOffer),
    BottomTab("Applications", Icons.Filled.Description),
)
private const val PROFILE_TAB = 3

@Composable
fun MainScreen(
    onProductClick: (String) -> Unit,
    onApplicationClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    SetStatusBarAppearance(darkIcons = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    var unreadNotifications by remember { mutableStateOf(0) }
    val user by AppContainer.session.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        unreadNotifications = AppContainer.notificationRepository.listNotifications().count { !it.read }
    }

    Scaffold(
        topBar = {
            AppHeader(
                userName = user?.fullName ?: "",
                avatarColor = hexToColor(user?.avatarColorHex ?: "#2563EB"),
                unreadNotifications = unreadNotifications,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = { selectedTab = PROFILE_TAB },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp),
            ) {
                TABS.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Primary600,
                            selectedTextColor = AppColors.Primary600,
                            indicatorColor = AppColors.Primary50,
                            unselectedIconColor = AppColors.Neutral400,
                            unselectedTextColor = AppColors.Neutral400,
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HomeScreen(onApplyClick = { selectedTab = 1 }, onApplicationClick = onApplicationClick)
                1 -> ProductsScreen(onProductClick = onProductClick)
                2 -> ApplicationsScreen(onApplicationClick = onApplicationClick)
                else -> ProfileScreen(onSettingsClick = onSettingsClick)
            }
        }
    }
}
