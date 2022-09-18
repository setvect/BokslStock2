package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.entity.AssetHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AssetHistoryRepository : JpaRepository<AssetHistoryEntity, Long>