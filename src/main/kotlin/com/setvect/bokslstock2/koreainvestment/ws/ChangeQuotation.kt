package com.setvect.bokslstock2.koreainvestment.ws

import org.springframework.context.ApplicationEvent

class ChangeQuotation(val response: String) : ApplicationEvent(response) {
}