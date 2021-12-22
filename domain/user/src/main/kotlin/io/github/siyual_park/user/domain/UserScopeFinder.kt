package io.github.siyual_park.user.domain

import io.github.siyual_park.auth.domain.scope_token.ScopeTokenFinder
import io.github.siyual_park.auth.entity.ScopeToken
import io.github.siyual_park.auth.repository.ScopeRelationRepository
import io.github.siyual_park.reader.finder.Finder
import io.github.siyual_park.user.entity.User
import io.github.siyual_park.user.repository.UserScopeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class UserScopeFinder(
    private val userScopeRepository: UserScopeRepository,
    private val scopeRelationRepository: ScopeRelationRepository,
    private val scopeTokenFinder: ScopeTokenFinder
) : Finder<ScopeToken, Long> {
    override suspend fun findById(id: Long): ScopeToken? {
        val parent = scopeTokenFinder.findByName("user") ?: return null
        val scopeToken = scopeTokenFinder.findById(id) ?: return null

        return if (scopeRelationRepository.findBy(parent.id!!, scopeToken.id!!) != null) {
            scopeToken
        } else {
            null
        }
    }

    override fun findAllById(ids: Iterable<Long>): Flow<ScopeToken> {
        val idSet = ids.toSet()
        return findAll().filter { idSet.contains(it.id) }
    }

    override fun findAll(): Flow<ScopeToken> {
        return scopeTokenFinder.findAllByParent("user")
    }

    fun findAllByUser(user: User): Flow<ScopeToken> {
        return user.id?.let { findAllByUserId(it) } ?: emptyFlow()
    }

    fun findAllByUserId(userId: Long): Flow<ScopeToken> {
        return flow {
            val userScopes = userScopeRepository.findAllByUserId(userId).toList()
            emitAll(scopeTokenFinder.findAllById(userScopes.map { it.scopeTokenId }))
        }
    }
}
