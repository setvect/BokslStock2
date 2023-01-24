package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.setvect.bokslstock2.util.DateUtil
import java.time.LocalDate

data class HolidayResponse(
    /**기준일자*/
    @JsonProperty("bass_dt") val bassDt: String,
    /**요일구분코드*/
    @JsonProperty("wday_dvsn_cd") val wdayDvsnCd: String,
    /**영업일여부*/
    @JsonProperty("bzdy_yn") val bzdyYn: String,
    /**거래일여부*/
    @JsonProperty("tr_day_yn") val trDayYn: String,
    /**개장일여부*/
    @JsonProperty("opnd_yn") val opndYn: String,
    /**결제일여부*/
    @JsonProperty("sttl_day_yn") val sttlDayYn: String
) {
    fun date(): LocalDate {
        return DateUtil.getLocalDate(bassDt, DateUtil.yyyyMMdd)
    }

    /**
     * @return true: 영업일
     */
    fun isBusinessDay(): Boolean {
        return bzdyYn == "Y"
    }

    /**
     * @return true: 휴장일
     */
    fun isHoliday(): Boolean {
        return !isBusinessDay()
    }
}
