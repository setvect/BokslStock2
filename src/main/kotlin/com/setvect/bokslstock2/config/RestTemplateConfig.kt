package com.setvect.bokslstock2.config

import java.io.IOException
import java.nio.charset.Charset
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.StreamUtils
import org.springframework.web.client.RestTemplate

/**
 * RestTemplate
 */
@Slf4j
@Configuration
class RestTemplateConfig(private val crawlResourceProperties: CrawlResourceProperties) {

    /**
     * @param restTemplateBuilder .
     * @return 크롤링에 사용할 RestTemplate
     */
    @Bean("crawlRestTemplate")
    fun crawlRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return getDefaultRestTemplate(
            restTemplateBuilder,
            crawlResourceProperties.config.connectionTimeoutMs,
            crawlResourceProperties.config.readTimeoutMs
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