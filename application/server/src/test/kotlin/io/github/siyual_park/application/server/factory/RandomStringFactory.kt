package io.github.siyual_park.application.server.factory

import java.util.UUID

object RandomStringFactory {
    fun create(size: Int) = UUID.randomUUID().toString().slice(0..size)
}