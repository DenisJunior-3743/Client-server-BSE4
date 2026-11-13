package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.model.LoanProduct

interface ProductRepository {
    suspend fun listProducts(): List<LoanProduct>
    suspend fun getProduct(id: String): LoanProduct?
}
