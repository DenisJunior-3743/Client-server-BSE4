package com.trustbank.loanapp.data.repository

import com.trustbank.loanapp.data.mock.MockData
import com.trustbank.loanapp.data.model.LoanProduct
import kotlinx.coroutines.delay

interface ProductRepository {
    suspend fun listProducts(): List<LoanProduct>
    suspend fun getProduct(id: String): LoanProduct?
}

class FakeProductRepository : ProductRepository {
    override suspend fun listProducts(): List<LoanProduct> {
        delay(250)
        return MockData.products
    }

    override suspend fun getProduct(id: String): LoanProduct? {
        delay(150)
        return MockData.productById(id)
    }
}
