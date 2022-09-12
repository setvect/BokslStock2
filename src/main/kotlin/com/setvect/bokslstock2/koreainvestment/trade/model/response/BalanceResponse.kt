package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class BalanceResponse(

    @JsonProperty("ctx_area_fk100") val ctxAreaFk100: String,
    @JsonProperty("ctx_area_nk100") val ctxAreaNk100: String,
    /**주식잔고 */
    @JsonProperty("output1") val holdings: ArrayList<Holdings> = arrayListOf(),
    /**예수금*/
    @JsonProperty("output2") val deposit: ArrayList<Deposit> = arrayListOf(),
    @JsonProperty("rt_cd") val rtCd: String,
    @JsonProperty("msg_cd") val msgCd: String,
    @JsonProperty("msg1") val msg1: String

) {
    data class Holdings(

        /**상품번호 - 종목코드*/
        @JsonProperty("pdno") val code: String,
        /**상품명*/
        @JsonProperty("prdt_name") val prdtName: String,
        /**매매구분명*/
        @JsonProperty("trad_dvsn_name") val tradDvsnName: String,
        /**전일매수수량*/
        @JsonProperty("bfdy_buy_qty") val bfdyBuyQty: String,
        /**전일매도수량*/
        @JsonProperty("bfdy_sll_qty") val bfdySllQty: String,
        /**금일매수수량*/
        @JsonProperty("thdt_buyqty") val thdtBuyqty: String,
        /**금일매도수량*/
        @JsonProperty("thdt_sll_qty") val thdtSllQty: String,
        /**보유수량*/
        @JsonProperty("hldg_qty") val hldgQty: Int,
        /**주문가능수량*/
        @JsonProperty("ord_psbl_qty") val ordPsblQty: String,
        /**매입평균가격*/
        @JsonProperty("pchs_avg_pric") val pchsAvgPric: Double,
        /**매입금액*/
        @JsonProperty("pchs_amt") val pchsAmt: String,
        /**현재가*/
        @JsonProperty("prpr") val prpr: String,
        /**평가금액*/
        @JsonProperty("evlu_amt") val evluAmt: String,
        /**평가손익금액*/
        @JsonProperty("evlu_pfls_amt") val evluPflsAmt: String,
        /**평가손익율*/
        @JsonProperty("evlu_pfls_rt") val evluPflsRt: String,
        /**평가수익율*/
        @JsonProperty("evlu_erng_rt") val evluErngRt: String,
        /**대출일자*/
        @JsonProperty("loan_dt") val loanDt: String,
        /**대출금액*/
        @JsonProperty("loan_amt") val loanAmt: String,
        /**대주매각대금*/
        @JsonProperty("stln_slng_chgs") val stlnSlngChgs: String,
        /**만기일자*/
        @JsonProperty("expd_dt") val expdDt: String,
        /**등락율*/
        @JsonProperty("fltt_rt") val flttRt: String,
        /**전일대비증감*/
        @JsonProperty("bfdy_cprs_icdc") val bfdyCprsIcdc: String,
        /**종목증거금율명*/
        @JsonProperty("item_mgna_rt_name") val itemMgnaRtName: String,
        /**보증금율명*/
        @JsonProperty("grta_rt_name") val grtaRtName: String,
        /**대용가격*/
        @JsonProperty("sbst_pric") val sbstPric: String,
        /**주식대출단가*/
        @JsonProperty("stck_loan_unpr") val stckLoanUnpr: String

    )

    data class Deposit(

        /**예수금총금액*/
        @JsonProperty("dnca_tot_amt") val dncaTotAmt: String,
        /**익일정산금액*/
        @JsonProperty("nxdy_excc_amt") val nxdyExccAmt: String,
        /**가수도정산금액*/
        @JsonProperty("prvs_rcdl_excc_amt") val prvsRcdlExccAmt: String,
        /**CMA평가금액*/
        @JsonProperty("cma_evlu_amt") val cmaEvluAmt: String,
        /**전일매수금액*/
        @JsonProperty("bfdy_buy_amt") val bfdyBuyAmt: String,
        /**금일매수금액*/
        @JsonProperty("thdt_buy_amt") val thdtBuyAmt: String,
        /**익일자동상환금액*/
        @JsonProperty("nxdy_auto_rdpt_amt") val nxdyAutoRdptAmt: String,
        /**전일매도금액*/
        @JsonProperty("bfdy_sll_amt") val bfdySllAmt: String,
        /**금일매도금액*/
        @JsonProperty("thdt_sll_amt") val thdtSllAmt: String,
        /**D+2자동상환금액*/
        @JsonProperty("d2_auto_rdpt_amt") val d2AutoRdptAmt: String,
        /**전일제비용금액*/
        @JsonProperty("bfdy_tlex_amt") val bfdyTlexAmt: String,
        /**금일제비용금액*/
        @JsonProperty("thdt_tlex_amt") val thdtTlexAmt: String,
        /**총대출금액*/
        @JsonProperty("tot_loan_amt") val totLoanAmt: String,
        /**유가평가금액*/
        @JsonProperty("scts_evlu_amt") val sctsEvluAmt: String,
        /**총평가금액*/
        @JsonProperty("tot_evlu_amt") val totEvluAmt: String,
        /**순자산금액*/
        @JsonProperty("nass_amt") val nassAmt: String,
        /**융자금자동상환여부*/
        @JsonProperty("fncg_gld_auto_rdpt_yn") val fncgGldAutoRdptYn: String,
        /**매입금액합계금액*/
        @JsonProperty("pchs_amt_smtl_amt") val pchsAmtSmtlAmt: String,
        /**평가금액합계금액*/
        @JsonProperty("evlu_amt_smtl_amt") val evluAmtSmtlAmt: String,
        /**평가손익합계금액*/
        @JsonProperty("evlu_pfls_smtl_amt") val evluPflsSmtlAmt: String,
        /**총대주매각대금*/
        @JsonProperty("tot_stln_slng_chgs") val totStlnSlngChgs: String,
        /**전일총자산평가금액*/
        @JsonProperty("bfdy_tot_asst_evlu_amt") val bfdyTotAsstEvluAmt: String,
        /**자산증감액*/
        @JsonProperty("asst_icdc_amt") val asstIcdcAmt: String,
        /**자산증감수익율*/
        @JsonProperty("asst_icdc_erng_rt") val asstIcdcErngRt: String
    )
}