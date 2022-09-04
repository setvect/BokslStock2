package com.setvect.bokslstock2

import com.setvect.bokslstock2.koreainvestment.ws.TradingWebsocket
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
class BokslStock2Application

fun main(args: Array<String>) {
    val context = runApplication<BokslStock2Application>(*args)
    val tradingWebsocket: TradingWebsocket = context.getBean(TradingWebsocket::class.java)
    tradingWebsocket.onApplicationEvent()
}
