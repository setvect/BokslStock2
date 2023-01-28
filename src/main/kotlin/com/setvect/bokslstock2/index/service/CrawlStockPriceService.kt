package com.setvect.bokslstock2.index.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.io.FileUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


@Service
class CrawlStockPriceService(

    private val bokslStockProperties: BokslStockProperties,
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository,
    private val crawlRestTemplate: RestTemplate,
) {
    companion object {
        private val START_DATE = LocalDateTime.of(1991, 1, 1, 0, 0)
        private val START_DATE_DOLLAR = LocalDateTime.of(2002, 1, 1, 0, 0)
    }

    @Autowired
    private lateinit var csvStoreService: CsvStoreService

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 등록된 모든 종목에 대한 시세 데이터를 삭제하고 다시 수집
     */
    @Transactional
    fun crawlStockPriceAll() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            crawlStockPriceWithDelete(it)
        }
    }

    /**
     * [stockEntity] 시세 모두 지우고 다시 수집
     */
    @Transactional
    fun crawlStockPriceWithDelete(stockCode: StockCode) {
        val stockEntityOptional = stockRepository.findByCode(stockCode.code)
        if (stockEntityOptional.isEmpty) {
            log.info("종목 등록: $stockCode")
            stockRepository.save(StockEntity(code = stockCode.code, name = stockCode.desc))
        }
        val stockEntity = stockRepository.findByCode(stockCode.code).get()
        crawlStockPriceWithDelete(stockEntity)
    }

    private fun crawlStockPriceWithDelete(stockEntity: StockEntity) {
        val stockCode = StockCode.findByCode(stockEntity.code)
        val deleteCount = candleRepository.deleteByStock(stockEntity)
        log.info("시세 데이터 삭제: ${stockEntity.name}(${stockEntity.code}) - ${String.format("%,d", deleteCount)}건")

        when (stockCode.national) {
            StockCode.StockType.KOR -> crawlStockPrice(stockEntity)
            StockCode.StockType.USA -> crawlStockPriceGlobal(stockEntity)
            StockCode.StockType.EXCHANGE -> crawlExchangeDollarAll(stockEntity)
        }
    }

    /**
     * 전체 기간 수집
     */
    fun crawlStockPrice(stockEntity: StockEntity) {
        saveCandleEntityList(stockEntity, DateRange(START_DATE, LocalDateTime.now()))
    }

    /**
     * 등록된 모든 종목에 대한 증분 시세 수집
     * [stockCodes] 수집대상 종목, null 이면 등록된 전체 종목 수집
     */
    fun crawlStockPriceIncremental(stockCodes: Set<StockCode>? = null) {
        val stockEntities = if (stockCodes == null) {
            stockRepository.findAll()
        } else {
            stockRepository.findByCodeIn(stockCodes.map { it.code })
        }
        stockEntities.forEach {
            val stockCode = StockCode.findByCode(it.code)
            when (stockCode.national) {
                StockCode.StockType.KOR -> crawlStockPriceIncremental(it)
                StockCode.StockType.USA -> crawlStockPriceGlobal(it)
                StockCode.StockType.EXCHANGE -> crawlExchangeDollarIncremental(it)
            }
        }
    }

    private fun crawlStockPriceGlobal(stockEntity: StockEntity) {
        val from = DateUtil.getUnixTime(LocalDate.of(1994, 1, 1))
        val to = DateUtil.getUnixTimeCurrent()
        val urlStr = bokslStockProperties.crawl.global.url
            .replace("{code}", stockEntity.code)
            .replace("{from}", from.toString())
            .replace("{to}", to.toString())
        val url = URL(urlStr)
        log.info("call: $url")

        url.openStream().use { outputStream ->
            val csvFile = File("./data_source/", "${stockEntity}.csv")

            log.info("${stockEntity.code} downloading from $url")
            FileUtils.copyInputStreamToFile(outputStream, csvFile)
            log.info("${stockEntity.code} complete")
            csvStoreService.store(stockEntity.code, csvFile)
            log.info("$stockEntity store complete")
            TimeUnit.SECONDS.sleep(3)
        }
    }

    /**
     *  마지막 수집일 부터 [end]까지 [stockEntity]종목을 시세 수집
     */
    private fun crawlStockPriceIncremental(stockEntity: StockEntity, end: LocalDateTime = LocalDateTime.now()) {
        val priceList = candleRepository.list(
            stockEntity, PageRequest.of(0, 1, Sort.by("candleDateTime").descending())
        )
        val start = if (priceList.isNotEmpty()) {
            // 완전한 시세 정보를 얻기 위해 마지막 시세 정보를 삭제함
            candleRepository.delete(priceList[0])
            priceList[0].candleDateTime
        } else {
            START_DATE
        }
        saveCandleEntityList(stockEntity, DateRange(start, end))
    }

    /**
     * 원달러 환율 수집
     * 기존 수집 내용 삭제
     */
    private fun crawlExchangeDollarAll(stockEntity: StockEntity) {
        log.info("수집: $stockEntity")
        val start = START_DATE_DOLLAR.toLocalDate()
        val end = LocalDate.now()

        val deleteCount = candleRepository.deleteByStock(stockEntity)
        log.info("시세 데이터 삭제: ${stockEntity.name}(${stockEntity.code}) - ${String.format("%,d", deleteCount)}건")

        crawlExchangeDollarRange(stockEntity, start, end)
    }

    /**
     * 원달러 환율 수집
     * 기존 수집 내용 삭제
     */
    private fun crawlExchangeDollarIncremental(stockEntity: StockEntity) {
        val priceList = candleRepository.list(
            stockEntity, PageRequest.of(0, 1, Sort.by("candleDateTime").descending())
        )
        val start = if (priceList.isNotEmpty()) {
            // 완전한 시세 정보를 얻기 위해 마지막 시세 정보를 삭제함
            candleRepository.delete(priceList[0])
            priceList[0].candleDateTime.toLocalDate()
        } else {
            START_DATE_DOLLAR.toLocalDate()
        }
        val end = LocalDate.now()
        crawlExchangeDollarRange(stockEntity, start, end)
    }

    private fun crawlExchangeDollarRange(stockEntity: StockEntity, start: LocalDate, end: LocalDate) {
        val data = mapOf(
            "BAS_SDT" to DateUtil.format(start, "yyyyMMdd"),
            "BAS_EDT" to DateUtil.format(end, "yyyyMMdd")
        )
        val url = bokslStockProperties.crawl.exchangeRate.url
        val response = Jsoup.connect(url).method(Connection.Method.POST).data(data).execute().body()
        val document = Jsoup.parse(response)

        val elements = document.select("table.tbl-type-1 tbody tr")

        val candleList = elements.map { row ->
            val localDate = DateUtil.getLocalDate(row.select("td:eq(0)").text(), "yyyy.MM.dd")
            val price = row.select("td:eq(6)").text().replace(",", "").toDouble()

            CandleEntity(
                stock = stockEntity,
                periodType = PERIOD_DAY,
                candleDateTime = localDate.atTime(0, 0),
                openPrice = price,
                highPrice = price,
                lowPrice = price,
                closePrice = price,
            )
        }

        saveCandle(candleList, stockEntity)
    }


    private fun saveCandleEntityList(stockEntity: StockEntity, range: DateRange) {
        val stockList = bokslStockProperties.crawl.korea.url.stockPrice
        val url = stockList.replace("{code}", stockEntity.code)
            .replace("{start}", range.getFromDateTimeFormat("yyyyMMdd"))
            .replace("{end}", range.getToDateTimeFormat("yyyyMMdd"))

        log.info("crawl: $url")

        val parameter: MutableMap<String, Any> = HashMap()
        parameter["User-Agent"] = bokslStockProperties.crawl.korea.userAgent
        val httpEntity = HttpEntity<Map<String, Any>>(parameter)

        val result = crawlRestTemplate.exchange(url, GET, httpEntity, String::class.java)
        val body = result.body ?: throw RuntimeException("JSON 결과 없음")

        // json format 맞추기
        body.replace("'", "\"")
        val priceList: ArrayList<ArrayList<String>> =
            Gson().fromJson(body, object : TypeToken<ArrayList<ArrayList<String>>>() {}.type)
        val candleList = priceList.stream().skip(1).map { row ->
            CandleEntity(
                stock = stockEntity,
                periodType = PERIOD_DAY,
                candleDateTime = DateUtil.getLocalDateTime(row[0] + "000000", "yyyyMMddHHmmss"),
                openPrice = row[1].toDouble(),
                highPrice = row[2].toDouble(),
                lowPrice = row[3].toDouble(),
                closePrice = row[4].toDouble(),
            )
        }.toList()

        saveCandle(candleList, stockEntity)
    }

    private fun saveCandle(candleList: List<CandleEntity>, stockEntity: StockEntity) {
        if (candleList.isEmpty()) {
            log.info("empty candle.")
            return
        }

        candleRepository.saveAll(candleList)
        log.info("save $stockEntity, count: ${candleList.size}, from=${candleList[0].candleDateTime}, to=${candleList[candleList.size - 1].candleDateTime}")
    }
}