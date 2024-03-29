package com.setvect.bokslstock2.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * RestTemplate
 */
@Configuration
class RestTemplateConfig(
    private val bokslStockProperties: BokslStockProperties
) {

    class LoggingRequestInterceptor : ClientHttpRequestInterceptor {

        companion object {
            private val log = LoggerFactory.getLogger(LoggingRequestInterceptor::class.java)
        }

        override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
//            logRequest(request, body)
            return execution.execute(request, body)
        }

        private fun logRequest(request: HttpRequest, body: ByteArray) {
            val headersString = request.headers.entries.joinToString(" ") {
                "-H \"${it.key}: ${it.value.joinToString(", ")}\""
            }
            val requestBody = if (body.isNotEmpty()) {
                "-d '${String(body, StandardCharsets.UTF_8)}'"
            } else {
                ""
            }
            log.debug("curl -X ${request.method} $headersString $requestBody ${request.uri}")
        }
    }

    /**
     * @param restTemplateBuilder .
     * @return 크롤링에 사용할 RestTemplate
     */
    @Bean("crawlRestTemplate")
    fun crawlRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        val restTemplate = getDefaultRestTemplate(
            restTemplateBuilder,
            bokslStockProperties.restTemplate.connectionTimeoutMs,
            bokslStockProperties.restTemplate.readTimeoutMs
        )
        restTemplate.interceptors.add(LoggingRequestInterceptor())

        return restTemplate
    }

    /**
     * @param restTemplateBuilder .
     * @return 크롤링에 사용할 RestTemplate
     */
    @Bean("stockRestTemplate")
    fun stockRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return getDefaultRestTemplate(
            restTemplateBuilder,
            bokslStockProperties.restTemplate.connectionTimeoutMs,
            bokslStockProperties.restTemplate.readTimeoutMs
        )
    }

    private fun getDefaultRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        connectionTimeoutMs: Int,
        readTimeoutMs: Int
    ): RestTemplate {
        return restTemplateBuilder.requestFactory {
            val factory = HttpComponentsClientHttpRequestFactory()
            factory.setConnectTimeout(connectionTimeoutMs)
            factory.setConnectionRequestTimeout(connectionTimeoutMs)
            factory.setReadTimeout(readTimeoutMs)
            BufferingClientHttpRequestFactory(factory)
        }
            .interceptors(LoggingInterceptor())
            .build()
    }

    private class LoggingInterceptor : ClientHttpRequestInterceptor {
        val log: Logger = LoggerFactory.getLogger(javaClass)

        @Throws(IOException::class)
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val start = System.currentTimeMillis()
            logRequest(request, body)
            val response = execution.execute(request, body)
            logResponse(response, start)
            return response
        }

        private fun logRequest(request: HttpRequest, body: ByteArray) {
            log.debug("--- request begin ---")
            log.debug("URI         : {}", request.uri)
            log.debug("Method      : {}", request.method)
            log.debug("Headers     : {}", request.headers)
            log.debug("Request body: {}", String(body, Charset.defaultCharset()))
            log.debug("--- request end ---")
        }

        @Throws(IOException::class)
        private fun logResponse(response: ClientHttpResponse, start: Long) {
            log.debug("--- response begin ---")
            log.debug("Status code  : {}", response.statusCode)
            log.debug("Status text  : {}", response.statusText)
            log.debug("Headers      : {}", response.headers)
            log.debug("Response body: {}", StreamUtils.copyToString(response.body, Charset.defaultCharset()))
            log.debug("Elapsed      : {}", System.currentTimeMillis() - start)
            log.debug("--- response end ---")
        }
    }
}