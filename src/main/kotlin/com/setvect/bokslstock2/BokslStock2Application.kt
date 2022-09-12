package com.setvect.bokslstock2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
class BokslStock2Application

fun main(args: Array<String>) {
    runApplication<BokslStock2Application>(*args)
}
