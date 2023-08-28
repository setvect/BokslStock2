package com.setvect.bokslstock2.strategy.companyvalue.model

data class Rank(
    var per: Int = 0,
    var pbr: Int = 0,
    var dvr: Int = 0,
) {
    fun total(): Int {
        return per + pbr + dvr
    }
}