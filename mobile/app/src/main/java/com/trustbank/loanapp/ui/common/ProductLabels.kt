package com.trustbank.loanapp.ui.common

import com.trustbank.loanapp.data.model.ProductType

fun productTypeLabel(type: ProductType): String = when (type) {
    ProductType.PERSONAL -> "Personal"
    ProductType.BUSINESS -> "Business"
    ProductType.AGRICULTURE -> "Agriculture"
    ProductType.ASSET_FINANCE -> "Asset Finance"
    ProductType.EMERGENCY -> "Emergency"
}
