package com.trustbank.loanapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.trustbank.loanapp.ui.common.StatusTone
import com.trustbank.loanapp.ui.common.toColor
import com.trustbank.loanapp.ui.common.toContainerColor
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.Neutral200, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
        }
        content()
    }
}

@Composable
fun StatusChip(label: String, tone: StatusTone, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(tone.toContainerColor(), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = tone.toColor(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun AvatarInitials(name: String, color: Color, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Formatters.initials(name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(AppColors.Neutral100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.Neutral400)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral800)
        if (description != null) {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Neutral500,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.Primary600)
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.Neutral200, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(tone.toContainerColor(), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tone.toColor(), modifier = Modifier.size(18.dp))
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, color = AppColors.Neutral900)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.Neutral500)
    }
}
