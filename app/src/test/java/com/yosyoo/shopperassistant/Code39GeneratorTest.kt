package com.yosyoo.shopperassistant

import com.yosyoo.shopperassistant.barcode.Code39Generator
import com.yosyoo.shopperassistant.barcode.InvalidCode39ContentException
import org.junit.Test

class Code39GeneratorTest {
    @Test
    fun normalizeOrThrow_acceptsCode39CharactersAndUppercases() {
        val normalized = Code39Generator.normalizeOrThrow(" ab-12.$/+% ")

        expectEquals("AB-12.$/+%", normalized)
    }

    @Test
    fun normalizeOrThrow_rejectsUnsupportedCharacters() {
        expectThrows<InvalidCode39ContentException> {
            Code39Generator.normalizeOrThrow("苹果-123")
        }
    }

    @Test
    fun normalizeOrThrow_rejectsEmptyContent() {
        val exception = expectThrows<InvalidCode39ContentException> {
            Code39Generator.normalizeOrThrow("   ")
        }

        expectEquals("请输入条码内容", exception.message)
    }

    @Test
    fun normalizeOrThrow_rejectsOverMaxLengthContent() {
        val content = "A".repeat(81)

        expectThrows<InvalidCode39ContentException> {
            Code39Generator.normalizeOrThrow(content)
        }
    }
}
