package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface CandleRepository : JpaRepository<CandleEntity, Long> {
    /**
     * 날짜 역순 조회
     */
    @Query("select c from CandleEntity c where c.stock = :stock")
    fun list(@Param("stock") stock: StockEntity, pageable: Pageable): List<CandleEntity>

    /**
     * 해당 종목의 시세 데이터를 삭제
     */
    @Modifying
    @Query("delete from CandleEntity c where c.stock = :stock")
    fun deleteByStock(@Param("stock") stock: StockEntity): Int


    @Query(
        "select c from CandleEntity c " +
                " where c.stock = :stock and c.candleDateTime between :candleDateTimeStart and :candleDateTimeEnd" +
                " order by c.candleDateTime"
    )
    fun findByRange(
        @Param("stock") stock: StockEntity,
        @Param("candleDateTimeStart") candleDateTimeStart: LocalDateTime,
        @Param("candleDateTimeEnd") candleDateTimeEnd: LocalDateTime
    ): List<CandleEntity>

}