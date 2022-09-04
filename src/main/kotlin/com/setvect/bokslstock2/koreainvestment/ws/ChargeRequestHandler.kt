package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.koreainvestment.ws.model.Quotation
import lombok.extern.slf4j.Slf4j
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * 호가 이벤트
 */
@Slf4j
@Component
class ChargeRequestHandler : ApplicationListener<ChangeQuotation> {
    override fun onApplicationEvent(event: ChangeQuotation) {
        val response = event.response
        parsing(response)
    }

    private fun parsing(response: String) {
        val quotation = Quotation.parsing(response)
    }
}