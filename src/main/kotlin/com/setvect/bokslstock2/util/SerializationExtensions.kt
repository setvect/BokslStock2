package com.setvect.bokslstock2.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JavaType

/**
 * 직렬화 역직렬화 하기 때문에 성능이 좋지 않음.
 * @return deep copy
 */
fun <K, V> Map<K, V>.deepCopyWithSerialization(): Map<K, V> {
    val jsonString = JsonUtil.mapper.writeValueAsString(this)
    return JsonUtil.mapper.readValue(jsonString, object : TypeReference<Map<K, V>>() {})
}


inline fun <K : Enum<K>, reified V> Map<K, V>.deepCopyWithSerialization(enumClass: Class<K>): Map<K, V> {
    val jsonString = JsonUtil.mapper.writeValueAsString(this)
    val mapType: JavaType = JsonUtil.mapper.typeFactory.constructMapType(
        Map::class.java,
        JsonUtil.mapper.typeFactory.constructType(enumClass),
        JsonUtil.mapper.typeFactory.constructType(V::class.java)
    )
    return JsonUtil.mapper.readValue(jsonString, mapType)
}
