package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.CandleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CandleRepository : JpaRepository<CandleEntity, Long> {
}