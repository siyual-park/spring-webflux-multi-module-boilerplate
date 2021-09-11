package io.github.siyual_park.auth.repository

import io.github.siyual_park.auth.entity.ScopeToken
import io.github.siyual_park.data.expansion.where
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepository
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Repository

@Repository
class ScopeTokenRepository(
    connectionFactory: ConnectionFactory
) : R2DBCRepository<ScopeToken, Long>(
    connectionFactory,
    ScopeToken::class,
) {
    suspend fun findByName(name: String): ScopeToken? {
        return findOne(where(ScopeToken::name).`is`(name))
    }

    suspend fun existsByName(name: String): Boolean {
        return exists(where(ScopeToken::name).`is`(name))
    }

    fun findAllByDefault(default: Boolean): Flow<ScopeToken> {
        return findAll(where(ScopeToken::default).`is`(default))
    }
}