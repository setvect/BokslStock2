package com.setvect.bokslstock2.util

import com.setvect.bokslstock2.ApplicationContextProvider
import org.springframework.context.ApplicationContext

/**
 * static한 메소드 등에서 spring bean 객체를 가져오기 위해 제공
 */
object BeanUtils {
    /**
     * @param beanId beanId
     * @return Spring Bean
     */
    fun getBean(beanId: String?): Any {
        val applicationContext = applicationContext
        return applicationContext.getBean(beanId!!)
    }

    /**
     * @param classType 클래스 타입
     * @param <T>       Bean 타입
     * @return Spring Bean
    </T> */
    @JvmStatic
    fun <T> getBean(classType: Class<T>): T {
        val applicationContext = applicationContext
        return applicationContext.getBean(classType)
    }

    /**
     * @return Application Context
     */
    private val applicationContext: ApplicationContext
        private get() = ApplicationContextProvider.applicationContext
                ?: throw NullPointerException("ApplicationContext not initialized.")
}