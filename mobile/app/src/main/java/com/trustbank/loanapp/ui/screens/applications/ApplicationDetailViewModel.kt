package com.trustbank.loanapp.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trustbank.loanapp.data.AppContainer
import com.trustbank.loanapp.data.model.LoanApplication
import com.trustbank.loanapp.data.model.LoanDocument
import com.trustbank.loanapp.data.model.LoanStatus
import com.trustbank.loanapp.data.model.RepaymentInstallment
import com.trustbank.loanapp.data.repository.ApplicationRepository
import com.trustbank.loanapp.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ApplicationDetailUiState(
    val isLoading: Boolean = true,
    val application: LoanApplication? = null,
    val documents: List<LoanDocument> = emptyList(),
    val isResponding: Boolean = false,
    val responseSent: Boolean = false,
    val schedule: List<RepaymentInstallment> = emptyList(),
    val isActing: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null,
)

private val REPAYMENT_STATUSES = setOf(LoanStatus.ACTIVE, LoanStatus.DELINQUENT, LoanStatus.CLOSED)

class ApplicationDetailViewModel(
    private val applicationRepository: ApplicationRepository = AppContainer.applicationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApplicationDetailUiState())
    val uiState: StateFlow<ApplicationDetailUiState> = _uiState

    fun load(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val application = applicationRepository.getApplication(applicationId)
            val documents = applicationRepository.getDocuments(applicationId)
            val schedule = if (application != null && application.status in REPAYMENT_STATUSES) {
                applicationRepository.getSchedule(applicationId)
            } else {
                emptyList()
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                application = application,
                documents = documents,
                schedule = schedule,
            )
        }
    }

    // The screen already validated the response message live before calling this.
    fun respondToInfoRequest(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResponding = true)
            try {
                val updated = applicationRepository.respondToInfoRequest(applicationId)
                _uiState.value = _uiState.value.copy(isResponding = false, application = updated, responseSent = true)
            } catch (e: Exception) {
                // Network or server refusal: stay on the form so the borrower can try again.
                _uiState.value = _uiState.value.copy(isResponding = false)
            }
        }
    }

    // Idempotency keys for the two money-moving actions. A key is created once per action and
    // re-sent unchanged if the borrower taps again after a network error, so the server performs
    // the action at most once. It is replaced only after the action is known to have succeeded.
    private var acceptKey: String = UUID.randomUUID().toString()
    private var paymentKey: String = UUID.randomUUID().toString()
    private var paymentKeyAmount: Double? = null

    /** Firm deadline: the server refuses (410) once the offer's expiry has passed. */
    fun acceptOffer(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActing = true, actionMessage = null, actionError = null)
            try {
                val updated = applicationRepository.acceptOffer(applicationId, acceptKey)
                acceptKey = UUID.randomUUID().toString()
                _uiState.value = _uiState.value.copy(
                    isActing = false,
                    application = updated,
                    actionMessage = "Offer accepted. Your funds are being prepared.",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActing = false, actionError = e.message ?: "Could not accept the offer")
            }
        }
    }

    fun repay(applicationId: String, amount: Double) {
        // Same amount again = a retry of the same payment (same key). A different amount is a new payment.
        if (paymentKeyAmount != amount) {
            paymentKeyAmount = amount
            paymentKey = UUID.randomUUID().toString()
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActing = true, actionMessage = null, actionError = null)
            try {
                val receipt = applicationRepository.repay(applicationId, amount, paymentKey)
                paymentKeyAmount = null
                val application = applicationRepository.getApplication(applicationId) ?: _uiState.value.application
                val schedule = applicationRepository.getSchedule(applicationId)
                _uiState.value = _uiState.value.copy(
                    isActing = false,
                    application = application,
                    schedule = schedule,
                    actionMessage = "Payment received (${receipt.paymentReference}). " +
                        "Outstanding balance: ${Formatters.ugx(receipt.outstandingBalance)}",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActing = false, actionError = e.message ?: "Payment failed")
            }
        }
    }
}
