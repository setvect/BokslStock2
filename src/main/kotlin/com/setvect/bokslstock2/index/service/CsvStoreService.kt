package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileReader
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

        val deleteCount = candleRepository.deleteByStock(stock)
        log.info("시세 데이터 삭제: ${stock.name}(${stock.code}) - ${String.format("%,d", deleteCount)}건")

        FileReader(csvStock).use { file ->
            val records: Iterable<CSVRecord> = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(file)
            val candleList = StreamSupport
                .stream(records.spliterator(), false)
                .map {
                    val close = it.get("Close").toDouble()
                    val adjClose = it.get("Adj Close").toDouble()
                    // 수정주가 비율를 구해 Open, High, Low 역산
                    val ratio = adjClose / close
                    CandleEntity(
                        stock = stock,
                        candleDateTime = DateUtil.getLocalDate(it.get("Date")).atTime(0, 0),
                        periodType = PeriodType.PERIOD_DAY,
                        openPrice = it.get("Open").toDouble() * ratio,
                        highPrice = it.get("High").toDouble() * ratio,
                        lowPrice = it.get("Low").toDouble() * ratio,
                        closePrice = adjClose
                    )
                }
                .collect(Collectors.toList())

            candleRepository.saveAll(candleList)
            log.info("시세 데이터 입력: ${stock.name}(${stock.code}) - ${String.format("%,d", candleList.size)}건")
        }
    }
}