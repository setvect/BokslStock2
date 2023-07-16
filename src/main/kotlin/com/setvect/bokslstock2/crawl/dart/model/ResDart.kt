package com.setvect.bokslstock2.crawl.dart.model

data class ResDart<T>(
    val status: String,
    val message: String,
    val list: List<T>
)