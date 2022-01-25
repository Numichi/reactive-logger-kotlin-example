package io.github.numichi.reactive.logger.kotlin.example.filter

import io.github.numichi.reactive.logger.MDC
import io.github.numichi.reactive.logger.kotlin.example.logger.AnotherMDC
import io.github.numichi.reactive.logger.kotlin.putMdc
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ApplicationFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val mdc1 = MDC() // ContextKey: "DEFAULT_REACTOR_CONTEXT_MDC_KEY"
        mdc1["fromFilter"] = "Application Filter"

        /**
         * AnotherMDC just example for parallel MDC Context!
         */
        val mdc2 = AnotherMDC() // ContextKey: "io.github.numichi.reactive.logger.kotlin.example.logger.AnotherMDC"
        mdc2["fromFilter"] = "Another MDC From Filter"

        return chain.filter(exchange)
            .contextWrite { putMdc(it, mdc1, mdc2) }
    }
}