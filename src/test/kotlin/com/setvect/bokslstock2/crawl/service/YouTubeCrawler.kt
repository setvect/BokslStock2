package com.setvect.bokslstock2.crawl.service

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.setvect.bokslstock2.util.DateUtil
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class YouTubeCrawler {
    var URL_PREFIX = "https://www.youtube.com/watch?v="

    @Test
    fun test() {
        val apiKey = "" // API 키
        val channelId = "" // 유튜브 채널 ID
        val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null)
            .setApplicationName("youtube-video-fetcher")
            .build()
        var nextPageToken: String? = ""
        val youtubeItems = mutableListOf<YoutubeItem>()
        while (nextPageToken != null) {
            val request = youtube.search()
                .list("snippet")
                .setChannelId(channelId)
                .setMaxResults(50L)
                .setOrder("date")
                .setType("video")
                .setVideoDefinition("high") // 쇼트 동영상 제외
                .setKey(apiKey)
                .setPageToken(nextPageToken)
            val response = request.execute()
            val items = response.items
            if (items != null) {
                for (item in items) {
                    val title = item.snippet.title
                    val publishedAt = item.snippet.publishedAt.toStringRfc3339()
                    val videoId = item.id.videoId

                    youtubeItems.add(
                        YoutubeItem(
                            title = title,
                            published = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            videoId = videoId
                        )
                    )
                }
            } else {
                println("동영상 목록을 찾을 수 없습니다.")
            }
            nextPageToken = response.nextPageToken
        }

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        val cellStyle = workbook.createCellStyle()
        val font = workbook.createFont()
        font.color = IndexedColors.BLUE.getIndex()
        font.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE)
        cellStyle.setFont(font)

        youtubeItems.filter {
            // title 시작문자열이 숫자 + .으로 시작하는 경우 true
            it.title.matches(Regex("^\\d+\\..*"))
        }.reversed().forEachIndexed { index, item ->

            // '132.ㅎㅎㅎ'에서 '132' 뽑기
            val no = item.title.split(".")[0]

            val row = sheet.createRow(index)
            row.createCell(0).setCellValue(no)
            val cellTitle = row.createCell(1)
            val creationHelper: CreationHelper = workbook.creationHelper

            val hyperlink = creationHelper.createHyperlink(HyperlinkType.URL)
            hyperlink.address = URL_PREFIX + item.videoId

            // 셀에 하이퍼링크를 할당
            try {
                var title = combineJamo(item.title)
                // title html decode 처리
                title = htmlToText(title)
                cellTitle.setCellValue(title)
            } catch (e: Exception) {
                println("error")
            }
            cellTitle.setHyperlink(hyperlink)
            cellTitle.cellStyle = cellStyle

            val cellRegdate = row.createCell(2)
            cellRegdate.setCellValue(DateUtil.format(item.published))
        }
        // XSSFWorkbook를 파일에 작성
        val outputStream = FileOutputStream("temp/유튜브_동영상_목록1.xlsx")
        workbook.write(outputStream)

        // 자원 닫기
        outputStream.close()
        workbook.close()
    }


    data class YoutubeItem(
        val title: String,
        val published: LocalDateTime,
        val videoId: String,
    )

    fun combineJamo(input: String): String {
        val SBase = 0xAC00
        val LBase = 0x1100
        val VBase = 0x1161
        val TBase = 0x11A7
        val LCount = 19
        val VCount = 21
        val TCount = 28
        val NCount = VCount * TCount

        val result = StringBuilder()
        var i = 0
        while (i < input.length) {
            val lIndex = input[i].toInt() - LBase
            if (0 <= lIndex && lIndex < LCount && i + 1 < input.length) {
                val vIndex = input[i + 1].toInt() - VBase
                if (0 <= vIndex && vIndex < VCount) {
                    val tIndex = if (i + 2 < input.length) input[i + 2].toInt() - TBase else -1
                    if (0 <= tIndex && tIndex < TCount) {
                        result.append((SBase + (lIndex * NCount) + (vIndex * TCount) + tIndex).toChar())
                        i += 3
                    } else {
                        result.append((SBase + (lIndex * NCount) + (vIndex * TCount)).toChar())
                        i += 2
                    }
                } else {
                    result.append(input[i])
                    i++
                }
            } else {
                result.append(input[i])
                i++
            }
        }
        return result.toString()
    }

    fun htmlToText(html: String): String {
        return Jsoup.parse(html).text()
    }
}
