package io.github.numichi.reactive.logger.kotlin.example.controller

import io.github.numichi.reactive.logger.MDC
import io.github.numichi.reactive.logger.kotlin.ReactiveLogger
import io.github.numichi.reactive.logger.kotlin.example.logger.AnotherMDC
import io.github.numichi.reactive.logger.kotlin.readMDC
import io.github.numichi.reactive.logger.kotlin.withMDCContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ExampleController {

    /**
     * ## Default parameters with non-parametered builder
     * ```
     * ReactiveLogger.builder()
     *     .withContext {  coroutineContext[ReactorContext]?.context ?: Context.empty }
     *     .withScheduler(Schedulers.boundedElastic())
     *     .withLogger(io.github.numichi.reactive.logger.kotlin.ReactiveLogger::class.java)
     *     .withMDCContextKey("DEFAULT_REACTOR_CONTEXT_MDC_KEY")
     * ```
     *
     * ## Default parameters with parameter builder.
     * This parameter can be any extend of [kotlin.coroutines.CoroutineContext.Element]
     *
     * Recommend override: [ReactiveLogger.Builder.withContext] and [ReactiveLogger.Builder.withMDCContextKey]
     * ```
     * ReactiveLogger.builder(ReactorContext)
     *     .withContext { null }
     *     .withScheduler(Schedulers.boundedElastic())
     *     .withLogger(io.github.numichi.reactive.logger.kotlin.ReactiveLogger::class.java)
     *     .withMDCContextKey("DEFAULT_REACTOR_CONTEXT_MDC_KEY")
     * ```
     *
     * ## Other
     * There is a [ReactiveLogger.Builder.enableError] method what allows to throwable exception when the looking for context ID is NOT
     * exist. If not enabled, an empty context will be included in slf4j.
     */
    val defaultLogger = ReactiveLogger.builder()
        .withLogger(this::class.java)
        .build()

    val defaultLoggerWithError = ReactiveLogger.builder()
        .withLogger(this::class.java)
        .withMDCContextKey("not-exist-context-id")
        .enableError()
        .build()

    val anotherLogger = ReactiveLogger.builder()
        .withLogger(this::class.java)
        .withMDCContextKey(AnotherMDC.CONTEXT_KEY)
        .build()

    /**
     * This request collect current MDC from defaultLogger. So source is "DEFAULT_REACTOR_CONTEXT_MDC_KEY" context key.
     *
     * ## Response
     * ```
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * Content-Length: 34
     *
     * {
     *     "fromFilter": "Application Filter"
     * }
     * ```
     */
    @GetMapping("example/1")
    suspend fun getExample1(): MDC? {
        return defaultLogger.snapshot()
    }

    /**
     * ## Response
     * ```
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * Content-Length: 34
     *
     * {
     *     "fromFilter": "Another MDC From Filter"
     * }
     * ```
     */
    @GetMapping("example/2")
    suspend fun getExample2(): MDC? {
        return anotherLogger.snapshot()
    }

    /**
     * ## Response
     *
     * Throw exception so by default it will be HTTP500.
     *
     * ```
     * HTTP/1.1 500 Internal Server Error
     * Content-Type: application/json
     * Content-Length: 135
     *
     * {
     *     "timestamp": "2022-01-25T21:49:59.959+00:00",
     *     "path": "/example/3",
     *     "status": 500,
     *     "error": "Internal Server Error",
     *     "requestId": "1474dcba-1"
     * }
     * ```
     *
     * @see getExample4
     */
    @GetMapping("example/3")
    suspend fun getExample3(): MDC? {
        return defaultLoggerWithError.snapshot()
    }

    /**
     * ## Response
     * ```
     * HTTP/1.1 200 OK
     * Content-Type: text/plain;charset=UTF-8
     * Content-Length: 109
     *
     * io.github.numichi.reactive.logger.exception.ContextNotExistException; "not-exist-context-id" context not found
     * ```
     */
    @GetMapping("example/4")
    suspend fun getExample4(): String? {
        val exception = runCatching { defaultLoggerWithError.snapshot() }.exceptionOrNull()!!
        return "${exception::class.java.canonicalName}; ${exception.message}"
    }

    /**
     * ## Log on console
     *
     * You can see context is: `{"fromFilter":"Application Filter"}`
     *
     * ```json
     * {"instant":1643148212.103057300,"timestamp":"2022-01-25T22:03:32.103057300Z","level":"INFO","loggerName":"io.github.numichi.reactive.logger.kotlin.example.controller.ExampleController","message":"example log","context":{"fromFilter":"Application Filter"}}
     * ```
     */
    @GetMapping("example/5")
    suspend fun getExample5() {
        defaultLogger.info("example log")
    }

    /**
     * ## Log on console
     *
     * You can see context is: `{"fromFilter":"Another MDC From Filter"}`
     *
     * ```json
     * {"instant":1643148341.068555900,"timestamp":"2022-01-25T22:05:41.068555900Z","level":"INFO","loggerName":"io.github.numichi.reactive.logger.kotlin.example.controller.ExampleController","message":"example log","context":{"fromFilter":"Another MDC From Filter"}}
     * ```
     */
    @GetMapping("example/6")
    suspend fun getExample6() {
        anotherLogger.info("example log")
    }

    /**
     * ## Log on console
     *
     * Add a new key-value into one exist MDC
     *
     * You can see context is: `{"newKey":"newValue","fromFilter":"Application Filter"}`
     *
     * ```json
     * {"instant":1643148702.443064400,"timestamp":"2022-01-25T22:11:42.443064400Z","level":"INFO","loggerName":"io.github.numichi.reactive.logger.kotlin.example.controller.ExampleController","message":"default","context":{"newKey":"newValue","fromFilter":"Application Filter"}}
     * ```
     */
    @GetMapping("example/7")
    suspend fun getExample7() {
        val mdcDefault = readMDC() // without parameter will use "DEFAULT_REACTOR_CONTEXT_MDC_KEY"

        mdcDefault["newKey"] = "newValue"

        withMDCContext(mdcDefault) {
            defaultLogger.info("default")
        }
    }

    /**
     * ## Log on console
     *
     * Extend both MDCs in parallel.
     *
     * You can see generated 2 logs.
     *
     * First is contained context: `{"fromFilter":"Application Filter","newKey1":"newValue1"}`
     * Second (another) is contained context: `{"newKey2":"newValue2","fromFilter":"Another MDC From Filter"}`
     *
     * ```json
     * {"instant":1643148796.120295300,"timestamp":"2022-01-25T22:13:16.120295300Z","level":"INFO","loggerName":"io.github.numichi.reactive.logger.kotlin.example.controller.ExampleController","message":"default","context":{"fromFilter":"Application Filter","newKey1":"newValue1"}}
     * {"instant":1643148796.158296300,"timestamp":"2022-01-25T22:13:16.158296300Z","level":"INFO","loggerName":"io.github.numichi.reactive.logger.kotlin.example.controller.ExampleController","message":"another","context":{"newKey2":"newValue2","fromFilter":"Another MDC From Filter"}}
     * ```
     */
    @GetMapping("example/8")
    suspend fun getExample8() {
        val mdcDefault = readMDC()
        val mdcAnother = readMDC(AnotherMDC.CONTEXT_KEY)

        mdcDefault["newKey1"] = "newValue1"
        mdcAnother["newKey2"] = "newValue2"

        withMDCContext(mdcDefault, mdcAnother) {
            defaultLogger.info("default")
            anotherLogger.info("another")
        }
    }
}