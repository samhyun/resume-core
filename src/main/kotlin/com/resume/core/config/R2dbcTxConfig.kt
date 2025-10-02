package com.resume.core.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

@Configuration
class R2dbcTxConfig {

    @Bean
    fun reactiveTxManager(cf: ConnectionFactory): ReactiveTransactionManager =
        R2dbcTransactionManager(cf)

    @Bean
    fun transactionalOperator(txManager: ReactiveTransactionManager): TransactionalOperator =
        TransactionalOperator.create(txManager)
}
