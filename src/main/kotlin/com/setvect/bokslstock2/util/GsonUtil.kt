package com.setvect.bokslstock2.util

import com.google.gson.*
import java.lang.reflect.Field
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object GsonUtil {
    @JvmField
    val GSON: Gson

    init {
        val gsonBuilder = GsonBuilder().setFieldNamingStrategy { f: Field ->
            // 숫자로 구분된 필드명도 변환 가능하도록 이름을 변경
            // 예)
            // accTradePrice24h => accTradePrice_24h
            // highest52WeekDate => highest_52WeekDate

            // 숫자로 구분된 필드명도 변환 가능하도록 이름을 변경
            // 예)
            // accTradePrice24h => accTradePrice_24h
            // highest52WeekDate => highest_52WeekDate
            val name = f.name.replace("(\\d+)".toRegex(), "_$1")
            separateCamelCase(name, "_").lowercase()
        }
        gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json: JsonElement?, _: Type?, _: JsonDeserializationContext? ->
            var dateTimeString = getString(json!!)
            // 날짜 표현 문자열 일관성 맞추기
            if (dateTimeString.length == "2021-05-15T11:26:30+09:00".length) {
                dateTimeString = dateTimeString.substring(0, 19)
            }
            DateUtil.getLocalDateTime(dateTimeString, DateUtil.yyyy_MM_ddTHH_mm_ss)
        } as JsonDeserializer<LocalDateTime>)

        gsonBuilder.registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json: JsonElement?, _: Type?, _: JsonDeserializationContext? ->
            val dateString = getString(json!!)
            if (dateString.length == 10) {
                return@JsonDeserializer DateUtil.getLocalDate(dateString, DateUtil.yyyy_MM_dd)
            }
            DateUtil.getLocalDate(dateString, DateUtil.yyyyMMdd)
        } as JsonDeserializer<LocalDate>)

        gsonBuilder.registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json: JsonElement?, _: Type?, _: JsonDeserializationContext? ->
            val timeStr = getString(json!!)
            if (timeStr.length == "12:00:00".length) {
                return@JsonDeserializer DateUtil.getLocalTime(timeStr, DateUtil.HH_mm_ss)
            }
            DateUtil.getLocalTime(timeStr, DateUtil.HHmmss)
        } as JsonDeserializer<LocalTime>)

        gsonBuilder.registerTypeAdapter(LocalDateTime::class.java, JsonSerializer { localDateTime: LocalDateTime?, _: Type?, _: JsonSerializationContext? ->
            JsonPrimitive(DateUtil.format(localDateTime!!, DateUtil.yyyy_MM_ddTHH_mm_ss))
        })

        GSON = gsonBuilder.create()
    }

    private fun separateCamelCase(name: String, separator: String): String {
        val translation = StringBuilder()
        var i = 0
        val length = name.length
        while (i < length) {
            val character = name[i]
            if (Character.isUpperCase(character) && translation.isNotEmpty()) {
                translation.append(separator)
            }
            translation.append(character)
            i++
        }
        return translation.toString()
    }

    private fun getString(json: JsonElement): String {
        val asJsonPrimitive = json.asJsonPrimitive
        return asJsonPrimitive.asString
    }

}