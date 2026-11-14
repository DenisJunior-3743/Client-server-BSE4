package com.trustbank.loanapp.util

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bound selected files and authenticated downloads, including unknown-length bodies. */
fun readDocumentBytes(input: InputStream, maxBytes: Int = 5_242_880): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(count <= maxBytes - output.size()) { "Files must be under 5MB" }
        output.write(buffer, 0, count)
    }
    require(output.size() > 0) { "Choose a non-empty file" }
    return output.toByteArray()
}
