package com.setvect.bokslstock2.slack

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class SlackSendMessageTest {
    @Autowired
    private lateinit var slackMessageService: SlackMessageService

    @Test
    fun sendMessage() {
        slackMessageService.sendMessage("@channel 안녕하세요.")
        println("끝.")
    }
}