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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.PaymentTerms
import com.trustbank.loanapp.data.model.Witness
import com.trustbank.loanapp.ui.common.FieldState
import com.trustbank.loanapp.ui.common.documentTypeLabel
import com.trustbank.loanapp.ui.common.paymentTermsLabel
import com.trustbank.loanapp.ui.common.rememberFieldState
import com.trustbank.loanapp.ui.components.AppDropdownField
import com.trustbank.loanapp.ui.components.AppTextField
import com.trustbank.loanapp.ui.components.BackRow
import com.trustbank.loanapp.ui.components.DateField
import com.trustbank.loanapp.ui.components.DetailRow
import com.trustbank.loanapp.ui.components.DigitsField
import com.trustbank.loanapp.ui.components.LoadingIndicator
import com.trustbank.loanapp.ui.components.NameField
import com.trustbank.loanapp.ui.components.NationalIdField
import com.trustbank.loanapp.ui.components.PhoneField
import com.trustbank.loanapp.ui.components.PrimaryButton
import com.trustbank.loanapp.ui.components.SecondaryButton
import com.trustbank.loanapp.ui.components.SectionCard
import com.trustbank.loanapp.ui.components.SetStatusBarAppearance
import com.trustbank.loanapp.ui.theme.AppColors
import com.trustbank.loanapp.util.Formatters
import com.trustbank.loanapp.util.Validators
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val STEP_TITLES = listOf("Applicant & Business", "Collateral & Witnesses", "Loan Terms", "Documents", "Review & Submit")
private const val MIN_WITNESSES = 2

@Composable
fun NewApplicationScreen(
    productId: String,
    onBack: () -> Unit,
    onSubmitted: (String) -> Unit,
    viewModel: NewApplicationViewModel = viewModel(),
) {
    SetStatusBarAppearance(darkIcons = true)

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    LaunchedEffect(uiState.submittedApplicationId) {
        uiState.submittedApplicationId?.let(onSubmitted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackRow(title = "New Application", onBack = onBack)
        StepIndicator(currentStep = uiState.step)

        val product = uiState.product
        if (product == null || !uiState.prefillLoaded) {
            LoadingIndicator(modifier = Modifier.height(200.dp))
        } else {
            ApplicationFormSteps(product = product, uiState = uiState, viewModel = viewModel)
        }
    }
}

/** A single witness's Name + Contact fields, wired into the shared FieldState/validation runtime. */
private class WitnessFields {
    val name = FieldState("") {
        when {
            !Validators.isRequired(it) -> "Witness name is required"
            !Validators.isValidFullName(it) -> "Enter a valid name"
            else -> null
        }
    }
    val contact = FieldState("") {
        when {
            !Validators.isRequired(it) -> "Contact is required"
            !Validators.isValidUgandaPhone(it) -> Validators.PHONE_ERROR_MESSAGE
            else -> null
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
    // Step 0 — applicant & business details
    val fullName = rememberFieldState(uiState.prefillFullName) {
        when {
            !Validators.isRequired(it) -> "Full name is required"
            !Validators.isValidFullName(it) -> "Enter a valid full name"
            else -> null
        }
    }
    val dobText = rememberFieldState(uiState.prefillDob?.toString() ?: "") { text ->
        if (text.isBlank()) return@rememberFieldState "Date of birth is required"
        val parsed = try {
            LocalDate.parse(text)
        } catch (e: DateTimeParseException) {
            null
        }
        when {
            parsed == null -> "Use format YYYY-MM-DD"
            !Validators.isAdult(parsed) -> "You must be at least 18 years old"
            else -> null
        }
    }
    val nin = rememberFieldState(uiState.prefillNin) {
        when {
            !Validators.isRequired(it) -> "National ID is required"
            !Validators.isValidNationalId(it) -> Validators.NATIONAL_ID_ERROR_MESSAGE
            else -> null
        }
    }
    val physicalLocation = rememberFieldState { if (!Validators.isRequired(it)) "Physical location is required" else null }
    val businessName = rememberFieldState { if (!Validators.isRequired(it)) "Business name is required" else null }
    val businessLocation = rememberFieldState { if (!Validators.isRequired(it)) "Business location is required" else null }

    // Step 1 — collateral & witnesses
    val collateral = rememberFieldState {
        if (it.trim().length < 10) "Describe the collateral security (at least 10 characters)" else null
    }
    val witnesses = remember { mutableStateListOf(WitnessFields(), WitnessFields()) }

    // Step 2 — loan terms
    val amount = rememberFieldState { text ->
        val value = Validators.parseAmount(text)
        when {
            value == null -> "Enter a valid amount"
            value < product.minAmount || value > product.maxAmount ->
                "Amount must be between ${product.minAmount.toInt()} and ${product.maxAmount.toInt()}"
            else -> null
        }
    }
    val period = rememberFieldState { text ->
        val value = text.toIntOrNull()
        when {
            value == null -> "Enter a valid period"
            value < product.minTermMonths || value > product.maxTermMonths ->
                "Period must be between ${product.minTermMonths} and ${product.maxTermMonths} months"
            else -> null
        }
    }
    val purpose = rememberFieldState {
        if (it.trim().length < 10) "Describe the purpose (at least 10 characters)" else null
    }
    var paymentTerms by remember { mutableStateOf<PaymentTerms?>(null) }
    var paymentTermsTouched by remember { mutableStateOf(false) }
    val paymentTermsError = if (paymentTermsTouched && paymentTerms == null) "Select the terms of payment" else null

    when (uiState.step) {
        0 -> StepApplicantDetails(
            fullName = fullName,
            dobText = dobText,
            nin = nin,
            physicalLocation = physicalLocation,
            businessName = businessName,
            businessLocation = businessLocation,
            onContinue = {
                val allValid = listOf(
                    fullName.validateForSubmit(),
                    dobText.validateForSubmit(),
                    nin.validateForSubmit(),
                    physicalLocation.validateForSubmit(),
                    businessName.validateForSubmit(),
                    businessLocation.validateForSubmit(),
                ).all { it }
                if (allValid) viewModel.goToStep(1)
            },
        )
        1 -> StepCollateralAndWitnesses(
            collateral = collateral,
            witnesses = witnesses,
            onBack = { viewModel.goToStep(0) },
            onContinue = {
                val witnessesValid = witnesses.map { it.name.validateForSubmit() and it.contact.validateForSubmit() }.all { it }
                if (collateral.validateForSubmit() && witnessesValid) viewModel.goToStep(2)
            },
        )
        2 -> StepLoanTerms(
            product = product,
            amount = amount,
            period = period,
            purpose = purpose,
            paymentTerms = paymentTerms,
            paymentTermsError = paymentTermsError,
            onPaymentTermsSelect = { paymentTerms = it },
            onBack = { viewModel.goToStep(1) },
            onContinue = {
                paymentTermsTouched = true
                val allValid = listOf(
                    amount.validateForSubmit(),
                    period.validateForSubmit(),
                    purpose.validateForSubmit(),
                ).all { it } && paymentTerms != null
                if (allValid) viewModel.goToStep(3)
            },
        )
        3 -> StepDocuments(
            attachedDocs = uiState.attachedDocs,
            docsError = uiState.docsError,
            onToggleDoc = viewModel::toggleDoc,
            onBack = { viewModel.goToStep(2) },
            onContinue = { if (viewModel.validateStepTwo()) viewModel.goToStep(4) },
        )
        else -> StepReview(
            product = product,
            fullName = fullName.value,
            dobText = dobText.value,
            nin = nin.value,
            physicalLocation = physicalLocation.value,
            businessName = businessName.value,
            businessLocation = businessLocation.value,
            collateral = collateral.value,
            witnesses = witnesses,
            amountText = amount.value,
            periodText = period.value,
            purposeText = purpose.value,
            paymentTerms = paymentTerms,
            attachedCount = uiState.attachedDocs.size,
            isSubmitting = uiState.isSubmitting,
            submitError = uiState.submitError,
            onBack = { viewModel.goToStep(3) },
            onSubmit = {
                val terms = paymentTerms
                if (terms != null) {
                    viewModel.submit(
                        amount = Validators.parseAmount(amount.value) ?: 0.0,
                        term = period.value.toIntOrNull() ?: product.minTermMonths,
                        purpose = purpose.value.trim(),
                        applicantFullName = fullName.value.trim(),
                        applicantDob = LocalDate.parse(dobText.value),
                        applicantNin = nin.value.trim(),
                        physicalLocation = physicalLocation.value.trim(),
                        businessName = businessName.value.trim(),
                        businessLocation = businessLocation.value.trim(),
                        collateralSecurity = collateral.value.trim(),
                        witnesses = witnesses.map { Witness(name = it.name.value.trim(), contact = it.contact.value) },
                        paymentTerms = terms,
                    )
                }
            },
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
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
private fun StepApplicantDetails(
    fullName: FieldState,
    dobText: FieldState,
    nin: FieldState,
    physicalLocation: FieldState,
    businessName: FieldState,
    businessLocation: FieldState,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Applicant details", style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
        NameField(
            label = "Full name",
            value = fullName.value,
            onValueChange = fullName::onValueChange,
            onFocusLost = fullName::onFocusLost,
            required = true,
            error = fullName.error,
        )
        DateField(
            label = "Date of birth",
            value = dobText.value,
            onValueChange = dobText::onValueChange,
            required = true,
            error = dobText.error,
        )
        NationalIdField(
            label = "NIN (National ID Number)",
            value = nin.value,
            onValueChange = nin::onValueChange,
            onFocusLost = nin::onFocusLost,
            required = true,
            error = nin.error,
        )
        AppTextField(
            label = "Physical location",
            value = physicalLocation.value,
            onValueChange = physicalLocation::onValueChange,
            onFocusLost = physicalLocation::onFocusLost,
            required = true,
            error = physicalLocation.error,
            placeholder = "e.g. Plot 12, Ntinda Road, Kampala",
        )

        Text("Business details", style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
        AppTextField(
            label = "Business name",
            value = businessName.value,
            onValueChange = businessName::onValueChange,
            onFocusLost = businessName::onFocusLost,
            required = true,
            error = businessName.error,
        )
        AppTextField(
            label = "Business location",
            value = businessLocation.value,
            onValueChange = businessLocation::onValueChange,
            onFocusLost = businessLocation::onFocusLost,
            required = true,
            error = businessLocation.error,
        )

        PrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun StepCollateralAndWitnesses(
    collateral: FieldState,
    witnesses: MutableList<WitnessFields>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Collateral security", style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
        AppTextField(
            label = "Collateral security",
            value = collateral.value,
            onValueChange = collateral::onValueChange,
            onFocusLost = collateral::onFocusLost,
            required = true,
            error = collateral.error,
            placeholder = "What are you offering as collateral?",
            singleLine = false,
            minLines = 3,
        )

        Text("Witnesses (minimum $MIN_WITNESSES)", style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
        witnesses.forEachIndexed { index, witness ->
            SectionCard {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Witness ${index + 1}", style = MaterialTheme.typography.labelLarge, color = AppColors.Neutral700)
                    if (witnesses.size > MIN_WITNESSES) {
                        IconButton(onClick = { witnesses.removeAt(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove witness ${index + 1}", tint = AppColors.Danger600)
                        }
                    }
                }
                NameField(
                    label = "Name",
                    value = witness.name.value,
                    onValueChange = witness.name::onValueChange,
                    onFocusLost = witness.name::onFocusLost,
                    required = true,
                    error = witness.name.error,
                )
                PhoneField(
                    label = "Contact",
                    value = witness.contact.value,
                    onValueChange = witness.contact::onValueChange,
                    onFocusLost = witness.contact::onFocusLost,
                    error = witness.contact.error,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { witnesses.add(WitnessFields()) }
                .padding(vertical = 6.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = AppColors.Primary600, modifier = Modifier.size(18.dp))
            Text(
                "Add another witness",
                color = AppColors.Primary600,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepLoanTerms(
    product: LoanProduct,
    amount: FieldState,
    period: FieldState,
    purpose: FieldState,
    paymentTerms: PaymentTerms?,
    paymentTermsError: String?,
    onPaymentTermsSelect: (PaymentTerms) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard {
            Text(product.name, style = MaterialTheme.typography.titleMedium, color = AppColors.Neutral900)
            DetailRow("Amount range", "${Formatters.ugx(product.minAmount)} – ${Formatters.ugx(product.maxAmount)}")
            DetailRow("Period range", "${product.minTermMonths}–${product.maxTermMonths} months")
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
            label = "Period (months)",
            value = period.value,
            onValueChange = period::onValueChange,
            onFocusLost = period::onFocusLost,
            required = true,
            error = period.error,
            placeholder = "e.g. 12",
            maxLength = 3,
        )
        AppDropdownField(
            label = "Terms of payment",
            options = PaymentTerms.entries,
            selected = paymentTerms,
            optionLabel = ::paymentTermsLabel,
            onSelect = onPaymentTermsSelect,
            required = true,
            error = paymentTermsError,
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Back", onClick = onBack, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepDocuments(
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
private fun StepReview(
    product: LoanProduct,
    fullName: String,
    dobText: String,
    nin: String,
    physicalLocation: String,
    businessName: String,
    businessLocation: String,
    collateral: String,
    witnesses: List<WitnessFields>,
    amountText: String,
    periodText: String,
    purposeText: String,
    paymentTerms: PaymentTerms?,
    attachedCount: Int,
    isSubmitting: Boolean,
    submitError: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard(title = "Applicant & Business") {
            DetailRow("Full name", fullName)
            DetailRow("Date of birth", dobText)
            DetailRow("NIN", nin)
            DetailRow("Physical location", physicalLocation)
            DetailRow("Business name", businessName)
            DetailRow("Business location", businessLocation)
        }
        SectionCard(title = "Collateral & Witnesses") {
            DetailRow("Collateral security", collateral)
            witnesses.forEachIndexed { index, witness ->
                DetailRow("Witness ${index + 1}", "${witness.name.value} · ${witness.contact.value}")
            }
        }
        SectionCard(title = "Loan Terms") {
            DetailRow("Product", product.name)
            DetailRow("Amount", "UGX $amountText")
            DetailRow("Period", "$periodText months")
            DetailRow("Terms of payment", paymentTerms?.let(::paymentTermsLabel) ?: "—")
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
