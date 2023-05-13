package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileReader
import java.time.LocalDateTime
import java.util.stream.Collectors
import java.util.stream.StreamSupport


@Service
class CsvStoreService(
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * [code] 종목 코드(티커)
     */
    @Transactional
    fun store(code: String, csvStock: File) {
        val stock = stockRepository.findByCode(code).get()
        val periodType = PeriodType.PERIOD_DAY

        deleteStock(stock, periodType)
        val candleList: MutableList<CandleEntity> = loadCandle(csvStock, stock, periodType)
        candleRepository.saveAll(candleList)
        log.info("시세 데이터 입력: ${stock.name}(${stock.code} - ${periodType}) - ${String.format("%,d", candleList.size)}건")
    }

    /**
     * 크래온PLUS로 수집한 데이터 저장
     * [code] 종목 코드(티커)
     * [csvStock] 수정 주가가 들어 왔다고 가정하고 진행
     * [append] false면 기존거 지우고 입력
     */
    @Transactional
    fun storeFromCron(code: String, csvStock: File, append: Boolean) {
        val stock = stockRepository.findByCode(code).get()
        val periodType = PeriodType.PERIOD_MINUTE_5

        if (!append) {
            deleteStock(stock, periodType)
        }
        val lastCandle = candleRepository.findByBeforeLastCandle(code, LocalDateTime.now(), periodType, Pageable.ofSize(1))
        val lastCandleDate = if (lastCandle.isEmpty()) {
            LocalDateTime.MIN
        } else {
            lastCandle[0].candleDateTime
        }

        val candleList: MutableList<CandleEntity> = loadCandleCron(csvStock, stock, periodType, lastCandleDate)
        if (candleList.isEmpty()) {
            log.info("시세 데이터 입력: ${stock.name}(${stock.code} - ${periodType}), 데이터 없음")
            return
        }
        candleRepository.saveAll(candleList)
        log.info("시세 데이터 입력: ${stock.name}(${stock.code} - ${periodType}), ${candleList.first().candleDateTime} ~ ${candleList.last().candleDateTime}, ${String.format("%,d", candleList.size)}건")
    }

    private fun loadCandle(
        csvStock: File,
        stock: StockEntity,
        periodType: PeriodType
    ): MutableList<CandleEntity> {
        FileReader(csvStock).use { file ->
            val records: Iterable<CSVRecord> = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(file)
            return StreamSupport
                .stream(records.spliterator(), false)
                .map {
                    val close = it.get("Close").toDouble()
                    val adjClose = it.get("Adj Close").toDouble()
                    // 수정주가 비율를 구해 Open, High, Low 역산
                    val ratio = adjClose / close
                    CandleEntity(
                        stock = stock,
                        candleDateTime = DateUtil.getLocalDate(it.get("Date")).atTime(0, 0),
                        periodType = periodType,
                        openPrice = it.get("Open").toDouble() * ratio,
                        highPrice = it.get("High").toDouble() * ratio,
                        lowPrice = it.get("Low").toDouble() * ratio,
                        closePrice = adjClose
                    )
                }
                .collect(Collectors.toList())
        }
    }

    /**
     * [lastCandleDate] 이후 데이터만 필터링
     */
    private fun loadCandleCron(
        csvStock: File,
        stock: StockEntity,
        periodType: PeriodType,
        lastCandleDate: LocalDateTime
    ): MutableList<CandleEntity> {
        FileReader(csvStock).use { file ->
            val records: Iterable<CSVRecord> = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(file)
            return StreamSupport
                .stream(records.spliterator(), false)
                .map {

                    // header:  date,time,open,high,low,close,volume
                    val dateTime = it.get("date") + it.get("time").padStart(4, '0')
                    CandleEntity(
                        stock = stock,
                        candleDateTime = DateUtil.getLocalDateTime(dateTime, "yyyyMMddHHmm"),
                        periodType = periodType,
                        openPrice = it.get("open").toDouble(),
                        highPrice = it.get("high").toDouble(),
                        lowPrice = it.get("low").toDouble(),
                        closePrice = it.get("close").toDouble(),
                    )
                }
                .filter { it.candleDateTime.isAfter(lastCandleDate) }
                .collect(Collectors.toList())
        }
    }

    private fun deleteStock(stock: StockEntity, periodType: PeriodType) {
        val deleteCount = candleRepository.deleteByStockPeriodType(stock, periodType)
        log.info("시세 데이터 삭제: ${stock.name}(${stock.code} - ${periodType}) - ${String.format("%,d", deleteCount)}건")
    }

//    /**
//     * 크래온PLUS로 수집한 데이터 저장
//     * [code] 종목 코드(티커)
//     */
//    @Transactional
//    fun storeFromCron(code: String, csvStock: File) {
//        val stock = stockRepository.findByCode(code).get()
//
//        val deleteCount = candleRepository.deleteByStock(stock)
//        log.info("시세 데이터 삭제: ${stock.name}(${stock.code}) - ${String.format("%,d", deleteCount)}건")
//
//        FileReader(csvStock).use { file ->
//            val records: Iterable<CSVRecord> = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(file)
//            val candleList = StreamSupport
//                .stream(records.spliterator(), false)
//                .map {
//                    val close = it.get("Close").toDouble()
//                    val adjClose = it.get("Adj Close").toDouble()
//                    // 수정주가 비율를 구해 Open, High, Low 역산
//                    val ratio = adjClose / close
//                    CandleEntity(
//                        stock = stock,
//                        candleDateTime = DateUtil.getLocalDate(it.get("Date")).atTime(0, 0),
//                        periodType = PeriodType.PERIOD_DAY,
//                        openPrice = it.get("Open").toDouble() ,
//                        highPrice = it.get("High").toDouble() * ratio,
//                        lowPrice = it.get("Low").toDouble() * ratio,
//                        closePrice = adjClose
//                    )
//                }
//                .collect(Collectors.toList())
//
//            candleRepository.saveAll(candleList)
//            log.info("시세 데이터 입력: ${stock.name}(${stock.code}) - ${String.format("%,d", candleList.size)}건")
//        }
//    }
}