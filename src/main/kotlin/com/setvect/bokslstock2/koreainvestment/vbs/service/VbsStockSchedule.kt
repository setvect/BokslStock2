package com.setvect.bokslstock2.koreainvestment.vbs.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class VbsStockSchedule(
    var vbsService: VbsService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // TODO 수능날 처럼 장 시작 시간이 조정되는 경우가 있음. 이런 상황 대비 해야됨
    @Scheduled(cron = "0 50 08 * * MON-FRI") // 월~금 매일 08시 50분에 실행
    fun vbsStart() {
        log.info("start")
        vbsService.start()
    }

    // 월~금 매일 09:00 ~ 14:00 까지 5분 마다 실행. 15:20 이후 장 종료 판단으로 프로그래밍 적으로 해결함
    // 1초를 여유를 줘 계산에 분봉 계산 오류를 방지함
    @Scheduled(cron = "1 0/5 09-16 * * MON-FRI")
    fun vbsSellCheck() {
        log.info("매도 체크")
        vbsService.sellCheck()
    }

    /**
     * 장 종료 이후 현재 잔고상황 리포트
     */
    @Scheduled(cron = "10 30 15 * * MON-FRI") //
    fun vbsReport() {
        log.info("report")
        vbsService.report()
    }
}