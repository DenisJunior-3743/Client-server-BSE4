package com.trustbank.loanapp.ui.common

import com.trustbank.loanapp.data.model.EmploymentType

fun employmentTypeLabel(type: EmploymentType): String = when (type) {
    EmploymentType.FORMAL -> "Formal Employment"
    EmploymentType.SELF_EMPLOYED -> "Self-Employed"
    EmploymentType.INFORMAL -> "Informal Employment"
}
