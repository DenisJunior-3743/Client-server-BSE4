package com.trustbank.loanapp.data.model

/** File bytes live only in memory until the authenticated API upload completes. */
data class DocumentUpload(val fileName: String, val contentType: String, val bytes: ByteArray)
