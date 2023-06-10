package com.setvect.bokslstock2.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object JsonUtil {
    val mapper: ObjectMapper = init()

    private fun init(): ObjectMapper {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        val customLocalDateTimeSerializer = LocalDateTimeSerializer(dateTimeFormatter)
        val javaTimeModule = JavaTimeModule()
        javaTimeModule.addSerializer(LocalDateTime::class.java, customLocalDateTimeSerializer)

        val objectMapper = ObjectMapper()
        objectMapper.registerModule(javaTimeModule)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return objectMapper
    }
}