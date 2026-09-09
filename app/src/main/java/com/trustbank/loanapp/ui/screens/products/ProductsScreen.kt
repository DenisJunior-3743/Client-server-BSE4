package com.trustbank.loanapp.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun ProductsScreen(onProductClick: (String) -> Unit) {
    var products by remember { mutableStateOf<List<LoanProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        products = AppContainer.productRepository.listProducts()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Loan Products", style = MaterialTheme.typography.headlineSmall, color = AppColors.Neutral900)
        Text("Pick a product to see rates and apply.", style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral500)

        if (isLoading) {
            LoadingIndicator(modifier = Modifier.height(200.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(products, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: LoanProduct, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.Neutral200, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
            Text("${product.interestRateAnnual}% p.a.", style = MaterialTheme.typography.labelLarge, color = AppColors.Primary600)
        }
        Text(
            "${Formatters.ugx(product.minAmount)} – ${Formatters.ugx(product.maxAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Neutral700,
        )
        Text(
            "${product.minTermMonths}–${product.maxTermMonths} months",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500,
        )
    }
}
