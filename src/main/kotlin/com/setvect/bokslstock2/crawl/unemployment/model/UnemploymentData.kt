package com.setvect.bokslstock2.crawl.unemployment.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class UnemploymentData(
    @JsonProperty("realtime_start")
    val realtimeStart: LocalDate,

    @JsonProperty("realtime_end")
    val realtimeEnd: LocalDate,

    @JsonProperty("observation_start")
    val observationStart: LocalDate,

    @JsonProperty("observation_end")
    val observationEnd: LocalDate,

    val units: String,

    @JsonProperty("output_type")
    val outputType: Int,

    @JsonProperty("file_type")
    val fileType: String,

    @JsonProperty("order_by")
    val orderBy: String,

    @JsonProperty("sort_order")
    val sortOrder: String,

    val count: Int,
    val offset: Int,
    val limit: Int,

    val observations: List<Observation>
)