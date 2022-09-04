package com.setvect.bokslstock2.koreainvestment.ws.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class QuotationTest {
    @Test
    fun test1() {
        val rawText =
            "233740^152809^A^8960^8965^8970^0^0^0^0^0^0^0^8955^8950^8945^0^0^0^0^0^0^0^29^636^684^0^0^0^0^0^0^0^1070^14889^10942^0^0^0^0^0^0^0^0^0^0^0^8955^229477^229477^180^2^2.05^12263346^0^0^0^0^0"
        val quotation = Quotation.parsing(rawText)
        Assertions.assertThat(quotation.code).isEqualTo("233740")
    }
}