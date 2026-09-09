package com.trustbank.loanapp.ui.screens.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.DocumentType
import com.trustbank.loanapp.data.model.LoanProduct
import com.trustbank.loanapp.data.model.PaymentTerms
import com.trustbank.loanapp.data.model.Witness
import com.trustbank.loanapp.data.repository.ApplicationRepository
import com.trustbank.loanapp.data.repository.NewApplicationInput
import com.trustbank.loanapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

val REQUIRED_APPLICATION_DOCS = listOf(DocumentType.NATIONAL_ID, DocumentType.PAYSLIP, DocumentType.BANK_STATEMENT)

data class NewApplicationUiState(
    val product: LoanProduct? = null,
    val step: Int = 0,
    val attachedDocs: Set<DocumentType> = emptySet(),
    val docsError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submittedApplicationId: String? = null,
    // Loaded once from the session/profile so the applicant-details step
    // can start pre-filled instead of asking the user to retype what's
    // already on file.
    val prefillLoaded: Boolean = false,
    val prefillFullName: String = "",
    val prefillDob: LocalDate? = null,
    val prefillNin: String = "",
)

// Loan details (amount/term/purpose) live in the screen as FieldState, not
// here, so they can validate on every keystroke — this ViewModel only owns
// what needs to survive step navigation but isn't per-field UI state.
class NewApplicationViewModel(
    private val productRepository: ProductRepository = AppContainer.productRepository,
    private val applicationRepository: ApplicationRepository = AppContainer.applicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewApplicationUiState())
    val uiState: StateFlow<NewApplicationUiState> = _uiState

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            val product = productRepository.getProduct(productId)
            val user = AppContainer.session.currentUser.value
            val profile = runCatching { AppContainer.profileRepository.getProfile() }.getOrNull()
            _uiState.value = _uiState.value.copy(
                product = product,
                prefillLoaded = true,
                prefillFullName = user?.fullName ?: "",
                prefillDob = profile?.dateOfBirth,
                prefillNin = profile?.nationalId ?: "",
            )
        }
    }

    fun toggleDoc(docType: DocumentType) {
        val current = _uiState.value.attachedDocs
        _uiState.value = _uiState.value.copy(
            attachedDocs = if (docType in current) current - docType else current + docType,
            docsError = null,
        )
    }

    fun validateStepTwo(): Boolean {
        val state = _uiState.value
        val missing = REQUIRED_APPLICATION_DOCS.any { it !in state.attachedDocs }
        _uiState.value = state.copy(docsError = if (missing) "Attach all required documents to continue" else null)
        return !missing
    }

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(step = step)
    }

    fun submit(
        amount: Double,
        term: Int,
        purpose: String,
        applicantFullName: String,
        applicantDob: LocalDate,
        applicantNin: String,
        physicalLocation: String,
        businessName: String,
        businessLocation: String,
        collateralSecurity: String,
        witnesses: List<Witness>,
        paymentTerms: PaymentTerms,
    ) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            try {
                val application = applicationRepository.submitApplication(
                    NewApplicationInput(
                        productId = product.id,
                        amountRequested = amount,
                        termMonths = term,
                        purpose = purpose,
                        applicantFullName = applicantFullName,
                        applicantDob = applicantDob,
                        applicantNin = applicantNin,
                        physicalLocation = physicalLocation,
                        businessName = businessName,
                        businessLocation = businessLocation,
                        collateralSecurity = collateralSecurity,
                        witnesses = witnesses,
                        paymentTerms = paymentTerms,
                    ),
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, submittedApplicationId = application.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = e.message ?: "Unable to submit application")
            }
        }
    }
}
