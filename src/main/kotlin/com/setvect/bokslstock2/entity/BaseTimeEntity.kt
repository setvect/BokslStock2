package com.setvect.bokslstock2.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {
    @CreatedDate
    @Column(name = "REG_DATE", nullable = false)
    var regDate: LocalDateTime = LocalDateTime.MIN
        private set

    @LastModifiedDate
    @Column(name = "EDIT_DATE", nullable = false)
    var editDate: LocalDateTime = LocalDateTime.MIN
        private set
}