package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TradeRepository : JpaRepository<TradeEntity, Long>