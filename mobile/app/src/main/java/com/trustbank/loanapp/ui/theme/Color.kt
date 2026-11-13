package com.trustbank.loanapp.ui.theme

import androidx.compose.ui.graphics.Color

// A Ugandan-bank-inspired palette: a deep navy blue primary (close to
// Centenary Bank's brand blue, #0055C6) paired with a gold accent (their
// site uses a #FFE200 highlight strip) — evokes that trust/gold identity
// without reproducing any of their actual marks. Diverged from the web
// dashboard's tokens (web/src/index.css) on purpose for this reskin.

object AppColors {
    val Primary50 = Color(0xFFEAF2FC)
    val Primary100 = Color(0xFFD3E4F9)
    val Primary200 = Color(0xFFA7C9F3)
    val Primary300 = Color(0xFF79ADEC)
    val Primary400 = Color(0xFF3D86E0)
    val Primary500 = Color(0xFF0F66CB)
    val Primary600 = Color(0xFF0055B8)
    val Primary700 = Color(0xFF00448F)
    val Primary800 = Color(0xFF033670)
    val Primary900 = Color(0xFF052A56)

    // Gold accent — the "borrowed from Centenary Bank" highlight color, used
    // sparingly for accent bars, badges, and the strongest password state.
    val Gold50 = Color(0xFFFFF9E5)
    val Gold100 = Color(0xFFFFF0BF)
    val Gold200 = Color(0xFFFFE180)
    val Gold300 = Color(0xFFFFD24D)
    val Gold400 = Color(0xFFFFC61A)
    val Gold500 = Color(0xFFF2B705)
    val Gold600 = Color(0xFFD9A200)
    val Gold700 = Color(0xFFB38200)

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
