package io.github.siyual_park.application.server.dto.response

import io.github.siyual_park.client.entity.ClientType
import java.net.URL
import java.time.Instant

data class ClientDetailInfo(
    val id: Long,
    val name: String,
    val type: ClientType,
    var origin: URL,
    val secret: String?,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
)
