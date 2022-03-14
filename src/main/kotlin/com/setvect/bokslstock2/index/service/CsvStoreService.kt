package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateUtil
import java.io.File
import java.io.FileReader
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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
                    var time = it.get("time") ?: ""
                    if (time.length == 3) {
                        time = "0$time"
                    }
                    CandleEntity(
                        stock = stock,
                        candleDateTime = DateUtil.getLocalDateTime(it.get("date") + time, "yyyyMMddHHmm"),
                        periodType = PeriodType.PERIOD_5_MINUTES,
                        openPrice = it.get("open").toDouble(),
                        highPrice = it.get("high").toDouble(),
                        lowPrice = it.get("low").toDouble(),
                        closePrice = it.get("close").toDouble()
                    )
                }
                .collect(Collectors.toList())

            candleRepository.saveAll(candleList)
            log.info("시세 데이터 입력: ${stock.name}(${stock.code}) - ${String.format("%,d", candleList.size)}건")
        }
    }
}