package com.trustbank.loanapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.ui.common.statusMeta
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun ApplicationRow(application: LoanApplication, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val product = MockData.productById(application.productId)
    val meta = statusMeta(application.status)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.Neutral200, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product?.name ?: application.productId, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
            Text(
                "${application.id} · ${Formatters.ugx(application.amountRequested)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral500,
            )
        }
        StatusChip(label = meta.label, tone = meta.tone)
    }
}
