package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CandleRepository : JpaRepository<CandleEntity, Long> {
    /**
     * 날짜 역순 조회
     */
    @Query("select c from CandleEntity c where c.stock = :stock")
    fun list(@Param("stock") stock: StockEntity, pageable: Pageable): List<CandleEntity>
}