package com.setvect.bokslstock2.bokslstock2.repository

import com.setvect.bokslstock2.bokslstock2.backtest.entity.StockEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StockRepository : JpaRepository<StockEntity, Long> {
}