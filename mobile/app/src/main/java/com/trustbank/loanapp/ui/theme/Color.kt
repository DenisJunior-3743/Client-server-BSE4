package com.trustbank.loanapp.ui.theme

import androidx.compose.ui.graphics.Color

// Same hex values as the web dashboard's design tokens (web/src/index.css).
// Keep these two files in sync by hand — mobile has no build step that reads
// the web app's CSS, so this is the one place the palette is repeated.

object AppColors {
    val Primary50 = Color(0xFFEFF6FF)
    val Primary100 = Color(0xFFDBEAFE)
    val Primary200 = Color(0xFFBFDBFE)
    val Primary300 = Color(0xFF93C5FD)
    val Primary400 = Color(0xFF60A5FA)
    val Primary500 = Color(0xFF3B82F6)
    val Primary600 = Color(0xFF2563EB)
    val Primary700 = Color(0xFF1D4ED8)
    val Primary800 = Color(0xFF1E40AF)
    val Primary900 = Color(0xFF1E3A8A)

    val Success50 = Color(0xFFECFDF5)
    val Success100 = Color(0xFFD1FAE5)
    val Success500 = Color(0xFF10B981)
    val Success600 = Color(0xFF059669)
    val Success700 = Color(0xFF047857)

    val Warning50 = Color(0xFFFFFBEB)
    val Warning100 = Color(0xFFFEF3C7)
    val Warning500 = Color(0xFFF59E0B)
    val Warning600 = Color(0xFFD97706)
    val Warning700 = Color(0xFFB45309)

    val Danger50 = Color(0xFFFEF2F2)
    val Danger100 = Color(0xFFFEE2E2)
    val Danger500 = Color(0xFFEF4444)
    val Danger600 = Color(0xFFDC2626)
    val Danger700 = Color(0xFFB91C1C)

    val Neutral50 = Color(0xFFF8FAFC)
    val Neutral100 = Color(0xFFF1F5F9)
    val Neutral200 = Color(0xFFE2E8F0)
    val Neutral300 = Color(0xFFCBD5E1)
    val Neutral400 = Color(0xFF94A3B8)
    val Neutral500 = Color(0xFF64748B)
    val Neutral600 = Color(0xFF475569)
    val Neutral700 = Color(0xFF334155)
    val Neutral800 = Color(0xFF1E293B)
    val Neutral900 = Color(0xFF0F172A)

    // Decorative avatar variety — same list as web/src/lib/avatarPalette.ts
    val AvatarPalette = listOf(
        Color(0xFF2563EB),
        Color(0xFF059669),
        Color(0xFFD97706),
        Color(0xFFDC2626),
        Color(0xFF7C3AED),
        Color(0xFF0891B2),
    )
}
