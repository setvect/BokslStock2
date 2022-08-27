package com.setvect.bokslstock2.slack

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.util.ApplicationUtil.getQueryString
import com.setvect.bokslstock2.util.ApplicationUtil.request
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SlackMessageService(
    private val bokslStockProperties: BokslStockProperties
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun sendMessage(message: String) {
        val config = bokslStockProperties.slack
        if (!config.enable) {
            log.debug("skip")
            return
        }
        val param: MutableMap<String, String> = HashMap()
        param["channel"] = config.channelId
        param["text"] = message
        param["link_names"] = "true"
        val url = MESSAGE_POST + "?" + getQueryString(param)
        val request = HttpPost(url)
        val params: MutableList<NameValuePair> = ArrayList()
        params.add(BasicNameValuePair("token", config.token))
        request.entity = UrlEncodedFormEntity(params)
        val response = request(url, request)
        if (response!!.contains("{\"ok\":false")) {
            log.warn(response)
        }
    }

    companion object {
        private const val MESSAGE_POST = "https://slack.com/api/chat.postMessage"
    }
}