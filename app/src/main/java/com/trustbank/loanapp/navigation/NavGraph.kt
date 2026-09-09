package com.trustbank.loanapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trustbank.loanapp.ui.screens.applications.ApplicationDetailScreen
import com.trustbank.loanapp.ui.screens.apply.NewApplicationScreen
import com.trustbank.loanapp.ui.screens.auth.LoginScreen
import com.trustbank.loanapp.ui.screens.auth.RegisterScreen
import com.trustbank.loanapp.ui.screens.main.MainScreen
import com.trustbank.loanapp.ui.screens.notifications.NotificationsScreen
import com.trustbank.loanapp.ui.screens.products.ProductDetailScreen
import com.trustbank.loanapp.ui.screens.settings.SettingsScreen
import com.trustbank.loanapp.ui.screens.splash.SplashScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = { isLoggedIn ->
                    val destination = if (isLoggedIn) Routes.MAIN else Routes.LOGIN
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onProductClick = { productId -> navController.navigate(Routes.productDetail(productId)) },
                onApplicationClick = { applicationId -> navController.navigate(Routes.applicationDetail(applicationId)) },
                onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Routes.ARG_PRODUCT_ID) ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onApplyClick = { id -> navController.navigate(Routes.apply(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.APPLY,
            arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Routes.ARG_PRODUCT_ID) ?: return@composable
            NewApplicationScreen(
                productId = productId,
                onBack = { navController.popBackStack() },
                onSubmitted = { applicationId ->
                    navController.navigate(Routes.applicationDetail(applicationId)) {
                        popUpTo(Routes.MAIN)
                    }
                },
            )
        }

        composable(
            route = Routes.APPLICATION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_APPLICATION_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val applicationId = backStackEntry.arguments?.getString(Routes.ARG_APPLICATION_ID) ?: return@composable
            ApplicationDetailScreen(applicationId = applicationId, onBack = { navController.popBackStack() })
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
            )
        }
    }
}
