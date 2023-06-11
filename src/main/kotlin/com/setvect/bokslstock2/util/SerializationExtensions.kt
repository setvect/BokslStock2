package com.setvect.bokslstock2.util

import com.fasterxml.jackson.module.kotlin.readValue

/**
 * 직렬화 역직렬화 하기 때문에 성능이 좋지 않음.
 * @return deep copy
 */
fun <K, V> Map<K, V>.deepCopyWithSerialization(): Map<K, V> {
    val jsonString = JsonUtil.mapper.writeValueAsString(this)
    return JsonUtil.mapper.readValue<Map<K, V>>(jsonString)
}
