package io.github.siyual_park.auth.domain.cors

import org.springframework.web.cors.reactive.CorsProcessor
import org.springframework.web.cors.reactive.CorsUtils
import org.springframework.web.cors.reactive.DefaultCorsProcessor
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class CorsWebFilter(
    private val configSource: CorsConfigurationSource,
    private val processor: CorsProcessor = DefaultCorsProcessor()
) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        return configSource.getCorsConfiguration(exchange)
            .flatMap { corsConfiguration ->
                val isValid = processor.process(corsConfiguration, exchange)
                if (!isValid || CorsUtils.isPreFlightRequest(request)) {
                    Mono.empty()
                } else chain.filter(exchange)
            }
    }
}
