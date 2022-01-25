package io.github.numichi.reactive.logger.kotlin.example.logger

import io.github.numichi.reactive.logger.MDC

class AnotherMDC : MDC(CONTEXT_KEY) {
    companion object {
        @JvmStatic
        val CONTEXT_KEY: String = AnotherMDC::class.java.canonicalName
    }
}