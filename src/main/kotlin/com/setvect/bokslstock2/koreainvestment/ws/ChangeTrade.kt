package com.setvect.bokslstock2.koreainvestment.ws

import org.springframework.context.ApplicationEvent

class ChangeTrade(tradeResult: TradeResult) : ApplicationEvent(tradeResult)