package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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
                " where c.stock = :stock and c.candleDateTime between :start and :end" +
                " order by c.candleDateTime"
    )
    fun findByRange(
        @Param("stock") stock: StockEntity,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<CandleEntity>


    /**
     * [stockList] 종목에서 [start] [end] 범위 안에 시세가 포함된 범위를 반환
     */
    @Query(
        "select new com.setvect.bokslstock2.util.DateRange(min(c.candleDateTime), max(c.candleDateTime)) " +
                " from CandleEntity c " +
                " where c.candleDateTime between :start and :end" +
                " and c.stock in (:stockList) "
    )
    fun findByCandleDateTimeBetween(
        @Param("stockList") stockList: List<StockEntity>,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): DateRange
}