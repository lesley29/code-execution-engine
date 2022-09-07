package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*

fun Application.configureLogging() {
    install(CallLogging) {
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Request uri: ${call.request.uri}, Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }
}