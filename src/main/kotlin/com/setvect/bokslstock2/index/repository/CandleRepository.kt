package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
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

    /**
     * 해당 종목의 시세 데이터를 삭제
     */
    @Modifying
    @Query("delete from CandleEntity c where c.stock = :stock and c.periodType = :periodType")
    fun deleteByStockPeriodType(@Param("stock") stock: StockEntity, @Param("periodType") periodType: PeriodType): Int


    @Query(
        "select c from CandleEntity c " +
                " where c.stock = :stock and c.candleDateTime between :start and :end" +
                " and c.periodType = :periodType" +
                " order by c.candleDateTime"
    )
    fun findByRange(
        @Param("stock") stock: StockEntity,
        @Param("periodType") periodType: PeriodType,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<CandleEntity>


    /**
     * [base] 기준으로 이전 캔들 중 가장 마지막 캔들
     */
    @Query(
        "select c from CandleEntity c " +
                " where c.stock.code = :stockCode and c.candleDateTime <= :base and c.periodType = :periodType" +
                " order by c.candleDateTime desc"
    )
    fun findByBeforeLastCandle(
        @Param("stockCode") stockCode: String,
        @Param("base") base: LocalDateTime,
        @Param("periodType") periodType: PeriodType,
        page: Pageable,
    ): List<CandleEntity>

    /**
     * 가지고 있는 시작날짜, 마지막날짜
     */
    @Query(
        "select new com.setvect.bokslstock2.util.DateRange(min(c.candleDateTime), max(c.candleDateTime)) from CandleEntity c " +
                " where c.stock.code = :stockCode and c.periodType = :periodType"
    )
    fun findByMaxMin(@Param("stockCode") stockCode: String, @Param("periodType") periodType: PeriodType): DateRange

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