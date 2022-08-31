package com.setvect.bokslstock2

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * Spring bean 객체를 가져옴
 */
@Component
class ApplicationContextProvider : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(ctx: ApplicationContext) {
        applicationContext = ctx
    }

    companion object {
        /**
         * @return Spring application context
         */
        /**
         * Spring application context
         */
        var applicationContext: ApplicationContext? = null
            private set
    }
}