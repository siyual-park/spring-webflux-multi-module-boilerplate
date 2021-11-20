package io.github.siyual_park.client.domain

import io.github.siyual_park.auth.domain.scope_token.ScopeTokenFinder
import io.github.siyual_park.auth.entity.ScopeToken
import io.github.siyual_park.client.entity.Client
import io.github.siyual_park.client.entity.ClientCredential
import io.github.siyual_park.client.entity.ClientScope
import io.github.siyual_park.client.repository.ClientCredentialRepository
import io.github.siyual_park.client.repository.ClientRepository
import io.github.siyual_park.client.repository.ClientScopeRepository
import io.github.siyual_park.data.event.AfterSaveEvent
import io.github.siyual_park.event.EventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.util.Random

@Component
class ClientFactory(
    private val clientRepository: ClientRepository,
    private val clientCredentialRepository: ClientCredentialRepository,
    private val clientScopeRepository: ClientScopeRepository,
    private val scopeTokenFinder: ScopeTokenFinder,
    private val operator: TransactionalOperator,
    private val eventPublisher: EventPublisher,
) {
    suspend fun create(payload: CreateClientPayload, scope: Collection<ScopeToken>? = null): Client =
        operator.executeAndAwait {
            createClient(payload).also {
                createCredential(it)
                if (scope == null) {
                    createDefaultScope(it).collect()
                } else {
                    createScope(it, scope)
                }
            }
        }!!.also {
            eventPublisher.publish(AfterSaveEvent(it))
        }

    private suspend fun createClient(payload: CreateClientPayload): Client {
        return clientRepository.create(Client(payload.name, payload.type))
    }

    private suspend fun createCredential(client: Client): ClientCredential? {
        if (client.isPublic()) {
            return null
        }

        return clientCredentialRepository.create(
            ClientCredential(
                clientId = client.id!!,
                secret = generateRandomSecret(64)
            )
        )
    }

    private suspend fun createDefaultScope(client: Client): Flow<ClientScope> {
        val clientScope = scopeTokenFinder.findAllByParent("client")
        return clientScopeRepository.createAll(
            clientScope.map {
                ClientScope(
                    clientId = client.id!!,
                    scopeTokenId = it.id!!
                )
            }
        )
    }

    private fun createScope(client: Client, scope: Collection<ScopeToken>): Flow<ClientScope> {
        return clientScopeRepository.createAll(
            scope.filter { it.id != null }
                .map {
                    ClientScope(
                        clientId = client.id!!,
                        scopeTokenId = it.id!!
                    )
                }
        )
    }

    fun generateRandomSecret(len: Int): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%&"
        val random = Random()
        val stringBuilder = StringBuilder(len)
        for (i in 0 until len) {
            stringBuilder.append(chars[random.nextInt(chars.length)])
        }
        return stringBuilder.toString()
    }
}