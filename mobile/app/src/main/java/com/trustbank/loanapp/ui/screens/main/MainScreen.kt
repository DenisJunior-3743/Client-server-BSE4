package com.trustbank.loanapp.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.ui.common.hexToColor
import com.trustbank.loanapp.ui.components.AppHeader
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.components.UpdateDialog
import com.trustbank.loanapp.ui.screens.applications.ApplicationsScreen
import com.trustbank.loanapp.ui.screens.home.HomeScreen
import com.trustbank.loanapp.ui.screens.products.ProductsScreen
import com.trustbank.loanapp.ui.screens.profile.ProfileScreen
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.update.UpdateStatus
import com.trustbank.loanapp.update.UpdateViewModel
import kotlinx.coroutines.launch

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
    SetStatusBarAppearance(darkIcons = false)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val updateViewModel: UpdateViewModel = viewModel()
    val updateState by updateViewModel.uiState.collectAsState()
    val amountsVisible by AppContainer.visibility.amountsVisible.collectAsState()

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate(context)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                amountsVisible = amountsVisible,
                onAmountsVisibleChange = { AppContainer.visibility.setVisible(it) },
                updateStatus = updateState.status,
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onSettingsClick()
                },
                onCheckUpdatesClick = {
                    scope.launch { drawerState.close() }
                    updateViewModel.checkForUpdate(context)
                },
            )
        },
    ) {
        MainScaffold(
            onProductClick = onProductClick,
            onApplicationClick = onApplicationClick,
            onNotificationsClick = onNotificationsClick,
            onSettingsClick = onSettingsClick,
            onMenuClick = { scope.launch { drawerState.open() } },
        )
    }

    UpdateDialog(
        uiState = updateState,
        onUpdateClick = { updateViewModel.downloadAndInstall(context) },
        onDismiss = { updateViewModel.dismiss() },
    )
}

@Composable
private fun MainScaffold(
    onProductClick: (String) -> Unit,
    onApplicationClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
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
                onMenuClick = onMenuClick,
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

@Composable
private fun AppDrawer(
    amountsVisible: Boolean,
    onAmountsVisibleChange: (Boolean) -> Unit,
    updateStatus: UpdateStatus,
    onSettingsClick: () -> Unit,
    onCheckUpdatesClick: () -> Unit,
) {
    val maxDrawerWidth = (LocalConfiguration.current.screenWidthDp * 0.75f).dp
    ModalDrawerSheet(modifier = Modifier.width(maxDrawerWidth)) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AppColors.Primary600, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "TrustBank",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Neutral900,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(16.dp))

            NavigationDrawerItem(
                label = { Text("Settings", maxLines = 1) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = false,
                onClick = onSettingsClick,
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Sensitive amounts", style = MaterialTheme.typography.labelMedium, color = AppColors.Neutral500, maxLines = 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (amountsVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = AppColors.Neutral700,
                        modifier = Modifier.size(20.dp),
                    )
                    Switch(checked = amountsVisible, onCheckedChange = onAmountsVisibleChange)
                }
            }

            NavigationDrawerItem(
                label = {
                    Text(
                        text = when (updateStatus) {
                            UpdateStatus.CHECKING -> "Checking…"
                            UpdateStatus.UP_TO_DATE -> "Up to date"
                            UpdateStatus.AVAILABLE -> "Update ready"
                            else -> "Updates"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                selected = false,
                onClick = onCheckUpdatesClick,
                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
