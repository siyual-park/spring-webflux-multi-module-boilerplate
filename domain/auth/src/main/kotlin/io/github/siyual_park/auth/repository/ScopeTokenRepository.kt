package io.github.siyual_park.auth.repository

import com.google.common.cache.CacheBuilder
import io.github.siyual_park.auth.entity.ScopeTokenData
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepository
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepositoryBuilder
import io.github.siyual_park.event.EventPublisher
import io.github.siyual_park.ulid.ULID
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class ScopeTokenRepository(
    entityOperations: R2dbcEntityOperations,
    eventPublisher: EventPublisher? = null
) : R2DBCRepository<ScopeTokenData, ULID> by R2DBCRepositoryBuilder<ScopeTokenData, ULID>(entityOperations, ScopeTokenData::class)
    .enableEvent(eventPublisher)
    .enableCache(
        CacheBuilder.newBuilder()
            .softValues()
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(1_000),
    )
    .enableQueryCache({
        CacheBuilder.newBuilder()
            .softValues()
            .expireAfterWrite(Duration.ofSeconds(1))
            .maximumSize(1_000)
    })
    .build()
