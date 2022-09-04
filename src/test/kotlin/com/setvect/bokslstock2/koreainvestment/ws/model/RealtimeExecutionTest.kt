package com.setvect.bokslstock2.koreainvestment.ws.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

internal class RealtimeExecutionTest {
    @Test
    fun parsing() {
        val rawText =
            "005930^151619^59700^2^900^1.53^59055.30^58200^59900^58200^59700^59600^34^10247065^605142384800^21595^33584^11989^155.15^3922631^6086067^1^0.60^105.75^090008^2^1500^150053^5^-200^090008^2^1500^20220831^20^N^33918^140697^663790^898987^0.17^9048877^113.24^0^^58200"

        val realtimeExecution = RealtimeExecution.parsing(rawText)
        Assertions.assertThat(realtimeExecution.code).isEqualTo("005930")
        Assertions.assertThat(realtimeExecution.stckCntgHour).isEqualTo(LocalTime.of(15, 16, 19))
        Assertions.assertThat(realtimeExecution.stckPrpr).isEqualTo(59700)
        Assertions.assertThat(realtimeExecution.stckOprc).isEqualTo(58200)
        Assertions.assertThat(realtimeExecution.stckHgpr).isEqualTo(59900)
        Assertions.assertThat(realtimeExecution.stckLwpr).isEqualTo(58200)
        Assertions.assertThat(realtimeExecution.bsopDate).isEqualTo(LocalDate.of(2022, 8, 31))
    }
}