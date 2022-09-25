package com.setvect.bokslstock2.util

import org.modelmapper.ModelMapper
import org.modelmapper.config.Configuration
import org.modelmapper.convention.MatchingStrategies

object ModalMapper {
    private val modelMapper: ModelMapper = ModelMapper()
    val mapper: ModelMapper
        get() {
            val configuration = modelMapper.configuration
            configuration.fieldAccessLevel = Configuration.AccessLevel.PRIVATE
            configuration.matchingStrategy = MatchingStrategies.STRICT
            return modelMapper
        }
}