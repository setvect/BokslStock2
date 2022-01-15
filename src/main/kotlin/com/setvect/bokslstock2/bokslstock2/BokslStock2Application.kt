package com.setvect.bokslstock2.bokslstock2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BokslStock2Application

fun main(args: Array<String>) {
	runApplication<BokslStock2Application>(*args)
	println()
}
