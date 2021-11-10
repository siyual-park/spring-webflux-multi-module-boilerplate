package io.github.siyual_park.auth.domain.scope_token

import io.github.siyual_park.auth.entity.ScopeRelation
import io.github.siyual_park.auth.entity.ScopeToken
import io.github.siyual_park.auth.repository.ScopeRelationRepository
import io.github.siyual_park.auth.repository.ScopeTokenRepository
import io.github.siyual_park.data.callback.AfterSaveCallbacks
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class ScopeTokenGenerator(
    private val scopeTokenRepository: ScopeTokenRepository,
    private val scopeRelationRepository: ScopeRelationRepository,
    private val afterSaveCallbacks: AfterSaveCallbacks
) {
    private val scopeTokens = mutableListOf<Pair<ScopeToken, Collection<ScopeToken>>>()

    fun register(scopeToken: ScopeToken, parents: Collection<ScopeToken> = emptyList()): ScopeTokenGenerator {
        scopeTokens.add(scopeToken to parents)
        return this
    }

    suspend fun generate() {
        scopeTokens.forEach { (scopeToken, parents) ->
            val child = upsert(scopeToken)
            val parents = scopeTokenRepository.findAllByName(parents.map { it.name }).toList()

            parents.forEach { parent ->
                upsert(
                    ScopeRelation(
                        parentId = parent.id!!,
                        childId = child.id!!
                    )
                )
            }
        }
    }

    private suspend fun upsert(scopeToken: ScopeToken): ScopeToken {
        return scopeTokenRepository.findByName(scopeToken.name)
            ?: scopeTokenRepository.create(scopeToken)
                .also { afterSaveCallbacks.onAfterSave(it) }
    }

    private suspend fun upsert(scopeRelation: ScopeRelation): ScopeRelation {
        return scopeRelationRepository.findBy(scopeRelation.parentId, scopeRelation.childId)
            ?: scopeRelationRepository.create(scopeRelation)
                .also { afterSaveCallbacks.onAfterSave(it) }
    }
}