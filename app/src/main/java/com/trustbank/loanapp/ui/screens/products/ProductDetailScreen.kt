package com.trustbank.loanapp.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.ui.common.productTypeLabel
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun ProductDetailScreen(productId: String, onApplyClick: (String) -> Unit, onBack: () -> Unit) {
    var product by remember { mutableStateOf<LoanProduct?>(null) }

    LaunchedEffect(productId) {
        product = AppContainer.productRepository.getProduct(productId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackRow(title = "Product Details", onBack = onBack)

        val current = product
        if (current == null) {
            LoadingIndicator(modifier = Modifier.height(200.dp))
        } else {
            SectionCard {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, color = AppColors.Neutral900)
                Text(productTypeLabel(current.type), style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
            }
            SectionCard(title = "Terms") {
                DetailRow("Interest rate", "${current.interestRateAnnual}% per year")
                DetailRow("Amount range", "${Formatters.ugx(current.minAmount)} – ${Formatters.ugx(current.maxAmount)}")
                DetailRow("Repayment term", "${current.minTermMonths}–${current.maxTermMonths} months")
            }
            PrimaryButton(text = "Apply for this loan", onClick = { onApplyClick(current.id) })
        }
    }
}
