package com.setvect.bokslstock2.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

object JsonUtil {
    val mapper: ObjectMapper = ObjectMapper().registerModules(JavaTimeModule())
}