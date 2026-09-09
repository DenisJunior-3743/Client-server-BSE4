package com.trustbank.loanapp.ui.screens.apply

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.ui.common.FieldState
import com.trustbank.loanapp.ui.common.documentTypeLabel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DigitsField
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.util.Formatters
import com.trustbank.loanapp.util.Validators
import com.trustbank.loanapp.ui.theme.AppColors

private val STEP_TITLES = listOf("Loan Details", "Documents", "Review & Submit")

@Composable
fun NewApplicationScreen(
    productId: String,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    viewModel: NewApplicationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    LaunchedEffect(uiState.submittedApplicationId) {
        uiState.submittedApplicationId?.let(onSubmitted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackRow(title = "New Application", onBack = onBack)
        StepIndicator(currentStep = uiState.step)

        val product = uiState.product
        if (product == null) {
            LoadingIndicator(modifier = Modifier.height(200.dp))
        } else {
            ApplicationFormSteps(product = product, uiState = uiState, viewModel = viewModel)
        }
    }
}

// Only ever entered once `product` is loaded and stable for the rest of the
// screen's life, so it's safe for the FieldState validators below to close
// over it directly.
@Composable
private fun ApplicationFormSteps(
    product: LoanProduct,
    uiState: NewApplicationUiState,
    viewModel: NewApplicationViewModel,
) {
    val amount = rememberFieldState { text ->
        val value = Validators.parseAmount(text)
        when {
            value == null -> "Enter a valid amount"
            value < product.minAmount || value > product.maxAmount ->
                "Amount must be between ${product.minAmount.toInt()} and ${product.maxAmount.toInt()}"
            else -> null
        }
    }
    val term = rememberFieldState { text ->
        val value = text.toIntOrNull()
        when {
            value == null -> "Enter a valid term"
            value < product.minTermMonths || value > product.maxTermMonths ->
                "Term must be between ${product.minTermMonths} and ${product.maxTermMonths} months"
            else -> null
        }
    }
    val purpose = rememberFieldState {
        if (it.trim().length < 10) "Describe the purpose (at least 10 characters)" else null
    }

    when (uiState.step) {
        0 -> StepOne(
            product = product,
            amount = amount,
            term = term,
            purpose = purpose,
            onContinue = {
                val allValid = listOf(amount.validateForSubmit(), term.validateForSubmit(), purpose.validateForSubmit()).all { it }
                if (allValid) viewModel.goToStep(1)
            },
        )
        1 -> StepTwo(
            attachedDocs = uiState.attachedDocs,
            docsError = uiState.docsError,
            onToggleDoc = viewModel::toggleDoc,
            onBack = { viewModel.goToStep(0) },
            onContinue = { if (viewModel.validateStepTwo()) viewModel.goToStep(2) },
        )
        else -> StepThree(
            product = product,
            amountText = amount.value,
            termText = term.value,
            purposeText = purpose.value,
            attachedCount = uiState.attachedDocs.size,
            isSubmitting = uiState.isSubmitting,
            submitError = uiState.submitError,
            onBack = { viewModel.goToStep(1) },
            onSubmit = {
                val parsedAmount = Validators.parseAmount(amount.value) ?: 0.0
                val parsedTerm = term.value.toIntOrNull() ?: product.minTermMonths
                viewModel.submit(parsedAmount, parsedTerm, purpose.value.trim())
            },
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            STEP_TITLES.forEachIndexed { index, _ ->
                val active = index <= currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (active) AppColors.Primary600 else AppColors.Neutral200, RoundedCornerShape(2.dp)),
                )
            }
        }
        Text(
            "Step ${currentStep + 1} of ${STEP_TITLES.size} · ${STEP_TITLES[currentStep]}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500,
        )
    }
}

@Composable
private fun StepOne(
    product: LoanProduct,
    amount: FieldState,
    term: FieldState,
    purpose: FieldState,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard {
            Text(product.name, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
            DetailRow("Amount range", "${Formatters.ugx(product.minAmount)} – ${Formatters.ugx(product.maxAmount)}")
            DetailRow("Term range", "${product.minTermMonths}–${product.maxTermMonths} months")
        }

        DigitsField(
            label = "Amount requested (UGX)",
            value = amount.value,
            onValueChange = amount::onValueChange,
            onFocusLost = amount::onFocusLost,
            required = true,
            error = amount.error,
            placeholder = "e.g. 2000000",
        )
        DigitsField(
            label = "Repayment term (months)",
            value = term.value,
            onValueChange = term::onValueChange,
            onFocusLost = term::onFocusLost,
            required = true,
            error = term.error,
            placeholder = "e.g. 12",
            maxLength = 3,
        )
        AppTextField(
            label = "Purpose of loan",
            value = purpose.value,
            onValueChange = purpose::onValueChange,
            onFocusLost = purpose::onFocusLost,
            required = true,
            error = purpose.error,
            placeholder = "What will this loan be used for?",
            singleLine = false,
            minLines = 3,
        )

        PrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun StepTwo(
    attachedDocs: Set<DocumentType>,
    docsError: String?,
    onToggleDoc: (DocumentType) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Confirm you have these documents ready — you can upload the actual files from the applicant portal after submitting.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Neutral500,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            REQUIRED_APPLICATION_DOCS.forEach { docType ->
                val checked = docType in attachedDocs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleDoc(docType) }
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, if (checked) AppColors.Primary600 else AppColors.Neutral200, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleDoc(docType) },
                        colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary600),
                    )
                    Text(documentTypeLabel(docType), style = MaterialTheme.typography.bodyMedium, color = AppColors.Neutral800)
                }
            }
        }

        if (docsError != null) {
            Text(docsError, color = AppColors.Danger600, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepThree(
    product: LoanProduct,
    amountText: String,
    termText: String,
    purposeText: String,
    attachedCount: Int,
    isSubmitting: Boolean,
    submitError: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard(title = "Review your application") {
            DetailRow("Product", product.name)
            DetailRow("Amount", "UGX $amountText")
            DetailRow("Term", "$termText months")
            DetailRow("Purpose", purposeText)
            DetailRow("Documents ready", "$attachedCount of ${REQUIRED_APPLICATION_DOCS.size}")
        }

        if (submitError != null) {
            Text(submitError, color = AppColors.Danger600, style = MaterialTheme.typography.bodySmall)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Submit application", isLoading = isSubmitting, onClick = onSubmit, modifier = Modifier.weight(1f))
        }
    }
}
