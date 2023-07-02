package com.setvect.bokslstock2.crawl.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Observation(
    @JsonProperty("realtime_start")
    val realtimeStart: LocalDate,

    @JsonProperty("realtime_end")
    val realtimeEnd: LocalDate,

    val date: LocalDate,
    val value: String
)