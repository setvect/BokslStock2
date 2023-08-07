package com.setvect.bokslstock2.crawl.dart.service

import com.setvect.bokslstock2.backtest.dart.model.CommonStatement

interface CrawlDartChecker {
    companion object {
        var DEFAULT_CHECKER: CrawlDartChecker = object : CrawlDartChecker {
            override fun check(commonStatement: CommonStatement): Boolean {
                return true
            }
        }
    }

    fun check(commonStatement: CommonStatement): Boolean
}