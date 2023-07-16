package com.setvect.bokslstock2.crawl.dart.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ResDividend(
    @JsonProperty("rcept_no") 
    // 접수번호(14자리)
    val rceptNo: String,

    @JsonProperty("corp_cls") 
    // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)
    val corpCls: String,

    @JsonProperty("corp_code") 
    // 공시대상회사의 고유번호(8자리)
    val corpCode: String,

    @JsonProperty("corp_name") 
    // 법인명
    val corpName: String,

    @JsonProperty("se") 
    // 유상증자(주주배정), 전환권행사 등
    val se: String,

    @JsonProperty("thstrm") 
    // 당기, 9,999,999,999
    val thstrm: String,

    @JsonProperty("frmtrm") 
    // 전기, 9,999,999,999
    val frmtrm: String,

    @JsonProperty("lwfr") 
    // 전전기, 9,999,999,999
    val lwfr: String
)