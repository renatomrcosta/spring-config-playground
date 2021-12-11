package com.xunfos.springconfigplayground

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import java.time.Duration

@SpringBootApplication
class SpringConfigPlaygroundApplication

fun main(args: Array<String>) {
    runApplication<SpringConfigPlaygroundApplication>(*args)
}

@RestController
class MyController(
    private val client: DelayClient,
) {
    @GetMapping("/delay")
    suspend fun delay(): String = client.getValueDelayed()
}


@Configuration
class MyHttpConfiguration(
    private val webClientBuilder: WebClient.Builder,
) {
    @Bean
    @RefreshScope
    fun webClient(
        @Value("\${http.defaultTimeout}")
        defaultTimeout: Long
    ): WebClient {
        val httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(defaultTimeout))
        return webClientBuilder.clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }
}

@Component
@RefreshScope
class DelayClient(
    private val webClient: WebClient,
    @Value("\${client.requestDelaySeconds}")
    private val requestDelaySeconds: Int,
    @Value("\${client.url}")
    private val url: String
) {
    suspend fun getValueDelayed(): String = webClient
        .get()
        .uri("$url$delayUri/$requestDelaySeconds")
        .retrieve()
        .awaitBody()

    companion object {
        private const val delayUri = "/delay"
    }
}