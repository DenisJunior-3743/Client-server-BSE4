package com.trustbank.loanapp.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class DocumentBytesTest {
    @Test fun acceptsExactLimitAndPreservesBytes() {
        val bytes = ByteArray(8193) { (it % 256).toByte() }
        assertArrayEquals(bytes, readDocumentBytes(ByteArrayInputStream(bytes), bytes.size))
    }

    @Test fun rejectsUnknownLengthStreamAsSoonAsItExceedsLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            readDocumentBytes(ByteArrayInputStream(ByteArray(8193)), 8192)
        }
    }

    @Test fun rejectsEmptyDocuments() {
        assertThrows(IllegalArgumentException::class.java) {
            readDocumentBytes(ByteArrayInputStream(byteArrayOf()))
        }
    }
}
