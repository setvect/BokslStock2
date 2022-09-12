package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class CurrentPriceResponse(

    /**종목 상태 구분 코드*/
    @JsonProperty("iscd_stat_cls_code") val iscdStatClsCode: String,
    /**증거금 비율*/
    @JsonProperty("marg_rate") val margRate: String,
    /**대표 시장 한글 명*/
    @JsonProperty("rprs_mrkt_kor_name") val rprsMrktKorName: String,
    /**신 고가 저가 구분 코드*/
    @JsonProperty("bstp_kor_isnm") val bstpKorIsnm: String,
    /**업종 한글 종목명*/
    @JsonProperty("temp_stop_yn") val tempStopYn: String,
    /**임시 정지 여부*/
    @JsonProperty("oprc_rang_cont_yn") val oprcRangContYn: String,
    /**시가 범위 연장 여부*/
    @JsonProperty("clpr_rang_cont_yn") val clprRangContYn: String,
    /**종가 범위 연장 여부*/
    @JsonProperty("crdt_able_yn") val crdtAbleYn: String,
    /**신용 가능 여부*/
    @JsonProperty("grmn_rate_cls_code") val grmnRateClsCode: String,
    /**보증금 비율 구분 코드*/
    @JsonProperty("elw_pblc_yn") val elwPblcYn: String,
    /**ELW 발행 여부*/
    @JsonProperty("stck_prpr") val stckPrpr: String,
    /**주식 현재가*/
    @JsonProperty("prdy_vrss") val prdyVrss: String,
    /**전일 대비*/
    @JsonProperty("prdy_vrss_sign") val prdyVrssSign: String,
    /**전일 대비 부호*/
    @JsonProperty("prdy_ctrt") val prdyCtrt: String,
    /**전일 대비율*/
    @JsonProperty("acml_tr_pbmn") val acmlTrPbmn: String,
    /**누적 거래 대금*/
    @JsonProperty("acml_vol") val acmlVol: String,
    /**누적 거래량*/
    @JsonProperty("prdy_vrss_vol_rate") val prdyVrssVolRate: String,
    /**전일 대비 거래량 비율*/
    @JsonProperty("stck_oprc") val stckOprc: String,
    /**주식 시가*/
    @JsonProperty("stck_hgpr") val stckHgpr: String,
    /**주식 최고가*/
    @JsonProperty("stck_lwpr") val stckLwpr: String,
    /**주식 최저가*/
    @JsonProperty("stck_mxpr") val stckMxpr: String,
    /**주식 상한가*/
    @JsonProperty("stck_llam") val stckLlam: String,
    /**주식 하한가*/
    @JsonProperty("stck_sdpr") val stckSdpr: String,
    /**주식 기준가*/
    @JsonProperty("wghn_avrg_stck_prc") val wghnAvrgStckPrc: String,
    /**가중 평균 주식 가격*/
    @JsonProperty("hts_frgn_ehrt") val htsFrgnEhrt: String,
    /**HTS 외국인 소진율*/
    @JsonProperty("frgn_ntby_qty") val frgnNtbyQty: String,
    /**외국인 순매수 수량*/
    @JsonProperty("pgtr_ntby_qty") val pgtrNtbyQty: String,
    /**프로그램매매 순매수 수량*/
    @JsonProperty("pvt_scnd_dmrs_prc") val pvtScndDmrsPrc: String,
    /**피벗 2차 디저항 가격*/
    @JsonProperty("pvt_frst_dmrs_prc") val pvtFrstDmrsPrc: String,
    /**피벗 1차 디저항 가격*/
    @JsonProperty("pvt_pont_val") val pvtPontVal: String,
    /**피벗 포인트 값*/
    @JsonProperty("pvt_frst_dmsp_prc") val pvtFrstDmspPrc: String,
    /**피벗 1차 디지지 가격*/
    @JsonProperty("pvt_scnd_dmsp_prc") val pvtScndDmspPrc: String,
    /**피벗 2차 디지지 가격*/
    @JsonProperty("dmrs_val") val dmrsVal: String,
    /**디저항 값*/
    @JsonProperty("dmsp_val") val dmspVal: String,
    /**디지지 값*/
    @JsonProperty("cpfn") val cpfn: String,
    /**자본금*/
    @JsonProperty("rstc_wdth_prc") val rstcWdthPrc: String,
    /**제한 폭 가격*/
    @JsonProperty("stck_fcam") val stckFcam: String,
    /**주식 액면가*/
    @JsonProperty("stck_sspr") val stckSspr: String,
    /**주식 대용가*/
    @JsonProperty("aspr_unit") val asprUnit: String,
    /**호가단위*/
    @JsonProperty("hts_deal_qty_unit_val") val htsDealQtyUnitVal: String,
    /**HTS 매매 수량 단위 값*/
    @JsonProperty("lstn_stcn") val lstnStcn: String,
    /**상장 주수*/
    @JsonProperty("hts_avls") val htsAvls: String,
    /**HTS 시가총액*/
    @JsonProperty("per") val per: String,
    /**PER*/
    @JsonProperty("pbr") val pbr: String,
    /**PBR*/
    @JsonProperty("stac_month") val stacMonth: String,
    /**결산 월*/
    @JsonProperty("vol_tnrt") val volTnrt: String,
    /**거래량 회전율*/
    @JsonProperty("eps") val eps: String,
    /**EPS*/
    @JsonProperty("bps") val bps: String,
    /**BPS*/
    @JsonProperty("d250_hgpr") val d250Hgpr: String,
    /**250일 최고가*/
    @JsonProperty("d250_hgpr_date") val d250HgprDate: String,
    /**250일 최고가 일자*/
    @JsonProperty("d250_hgpr_vrss_prpr_rate") val d250HgprVrssPrprRate: String,
    /**250일 최고가 대비 현재가 비율*/
    @JsonProperty("d250_lwpr") val d250Lwpr: String,
    /**250일 최저가*/
    @JsonProperty("d250_lwpr_date") val d250LwprDate: String,
    /**250일 최저가 일자*/
    @JsonProperty("d250_lwpr_vrss_prpr_rate") val d250LwprVrssPrprRate: String,
    /**250일 최저가 대비 현재가 비율*/
    @JsonProperty("stck_dryy_hgpr") val stckDryyHgpr: String,
    /**주식 연중 최고가*/
    @JsonProperty("dryy_hgpr_vrss_prpr_rate") val dryyHgprVrssPrprRate: String,
    /**연중 최고가 대비 현재가 비율*/
    @JsonProperty("dryy_hgpr_date") val dryyHgprDate: String,
    /**연중 최고가 일자*/
    @JsonProperty("stck_dryy_lwpr") val stckDryyLwpr: String,
    /**주식 연중 최저가*/
    @JsonProperty("dryy_lwpr_vrss_prpr_rate") val dryyLwprVrssPrprRate: String,
    /**연중 최저가 대비 현재가 비율*/
    @JsonProperty("dryy_lwpr_date") val dryyLwprDate: String,
    /**연중 최저가 일자*/
    @JsonProperty("w52_hgpr") val w52Hgpr: String,
    /**52주일 최고가*/
    @JsonProperty("w52_hgpr_vrss_prpr_ctrt") val w52HgprVrssPrprCtrt: String,
    /**52주일 최고가 대비 현재가 대비*/
    @JsonProperty("w52_hgpr_date") val w52HgprDate: String,
    /**52주일 최고가 일자*/
    @JsonProperty("w52_lwpr") val w52Lwpr: String,
    /**52주일 최저가*/
    @JsonProperty("w52_lwpr_vrss_prpr_ctrt") val w52LwprVrssPrprCtrt: String,
    /**52주일 최저가 대비 현재가 대비*/
    @JsonProperty("w52_lwpr_date") val w52LwprDate: String,
    /**52주일 최저가 일자*/
    @JsonProperty("whol_loan_rmnd_rate") val wholLoanRmndRate: String,
    /**전체 융자 잔고 비율*/
    @JsonProperty("ssts_yn") val sstsYn: String,
    /**공매도가능여부*/
    @JsonProperty("stck_shrn_iscd") val stckShrnIscd: String,
    /**주식 단축 종목코드*/
    @JsonProperty("fcam_cnnm") val fcamCnnm: String,
    /**액면가 통화명*/
    @JsonProperty("cpfn_cnnm") val cpfnCnnm: String,
    /**자본금 통화명*/
    @JsonProperty("frgn_hldn_qty") val frgnHldnQty: String,
    /**접근도*/
    @JsonProperty("vi_cls_code") val viClsCode: String,
    /**외국인 보유 수량*/
    @JsonProperty("ovtm_vi_cls_code") val ovtmViClsCode: String,
    /**VI적용구분코드*/
    @JsonProperty("last_ssts_cntg_qty") val lastSstsCntgQty: String,
    /**시간외단일가VI적용구분코드*/
    @JsonProperty("invt_caful_yn") val invtCafulYn: String,
    /**최종 공매도 체결 수량*/
    @JsonProperty("mrkt_warn_cls_code") val mrktWarnClsCode: String,
    /**투자유의여부*/
    @JsonProperty("short_over_yn") val shortOverYn: String,
    /**시장경고코드*/
    @JsonProperty("sltr_yn") val sltrYn: String

)