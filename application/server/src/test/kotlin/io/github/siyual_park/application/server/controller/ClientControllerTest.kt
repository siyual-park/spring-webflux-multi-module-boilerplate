package io.github.siyual_park.application.server.controller

import io.github.siyual_park.IntegrationTest
import io.github.siyual_park.application.server.dto.request.UpdateClientRequest
import io.github.siyual_park.application.server.dummy.DummyCreateClientPayload
import io.github.siyual_park.application.server.dummy.DummyCreateClientRequest
import io.github.siyual_park.application.server.dummy.DummyCreateUserPayload
import io.github.siyual_park.application.server.gateway.ClientControllerGateway
import io.github.siyual_park.application.server.gateway.GatewayAuthorization
import io.github.siyual_park.auth.domain.scope_token.ScopeTokenFactory
import io.github.siyual_park.client.domain.ClientFactory
import io.github.siyual_park.client.domain.ClientStorage
import io.github.siyual_park.client.entity.ClientType
import io.github.siyual_park.coroutine.test.CoroutineTestHelper
import io.github.siyual_park.test.DummyNameFactory
import io.github.siyual_park.user.domain.UserFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.Optional

@IntegrationTest
class ClientControllerTest @Autowired constructor(
    private val gatewayAuthorization: GatewayAuthorization,
    private val clientControllerGateway: ClientControllerGateway,
    private val userFactory: UserFactory,
    private val clientFactory: ClientFactory,
    private val scopeTokenFactory: ScopeTokenFactory,
    private val clientStorage: ClientStorage
) : CoroutineTestHelper() {

    @Test
    fun `POST clients, status = 201`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:create")
        )

        ClientType.values().forEach {
            val request = DummyCreateClientRequest.create(
                DummyCreateClientRequest.Template(
                    type = Optional.of(it)
                )
            )
            val response = clientControllerGateway.create(request)

            assertEquals(HttpStatus.CREATED, response.status)

            val clientInfo = response.responseBody.awaitSingle()
            val client = clientInfo.id?.orElse(null)?.let { clientStorage.load(it) }

            assertNotNull(client)
            assertNotNull(clientInfo.id)
            assertEquals(request.name, clientInfo.name?.orElse(null))
            assertEquals(request.type, clientInfo.type?.orElse(null))
            assertEquals(request.origin, clientInfo.origin?.orElse(null))
            assertEquals(client?.getScope(deep = false)?.toList()?.size, clientInfo.scope?.orElse(null)?.size)
            if (request.type == ClientType.CONFIDENTIAL) {
                assertNotNull(clientInfo.secret?.orElse(null))
            } else {
                assertEquals(null, clientInfo.secret?.orElse(null))
            }
            assertNotNull(clientInfo.createdAt?.orElse(null))
            assertNotNull(clientInfo.updatedAt?.orElse(null))
        }
    }

    @Test
    fun `POST clients, status = 409`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:create")
        )

        val request = DummyCreateClientRequest.create()
        clientControllerGateway.create(request)

        val response = clientControllerGateway.create(request)
        assertEquals(HttpStatus.CONFLICT, response.status)
    }

    @Test
    fun `POST clients, status = 400`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:create")
        )

        val request = DummyCreateClientRequest.create(
            DummyCreateClientRequest.Template(
                name = Optional.of(DummyNameFactory.create(25))
            )
        )

        val response = clientControllerGateway.create(request)
        assertEquals(HttpStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun `POST clients, status = 403`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        gatewayAuthorization.setPrincipal(
            principal,
            pop = listOf("clients:create")
        )

        val request = DummyCreateClientRequest.create()
        val response = clientControllerGateway.create(request)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `GET clients, status = 200`() = blocking {
        val principal = DummyCreateUserPayload.create()
            .let { userFactory.create(it).toPrincipal() }

        val client = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:read")
        )

        val response = clientControllerGateway.readAll(
            name = "eq:${client.name}",
            sort = "asc:created_at",
            page = 0,
            perPage = 1
        )

        assertEquals(HttpStatus.OK, response.status)

        val responseClients = response.responseBody.asFlow().toList()
        assertEquals(1, responseClients.size)

        val clientInfo = responseClients[0]
        assertEquals(client.id, clientInfo.id?.orElse(null))
        assertEquals(client.name, clientInfo.name?.orElse(null))
        assertEquals(client.type, clientInfo.type?.orElse(null))
        assertEquals(client.origin, clientInfo.origin?.orElse(null))
        assertNotNull(clientInfo.createdAt?.orElse(null))
        assertNotNull(clientInfo.updatedAt?.orElse(null))
    }

    @Test
    fun `GET clients, status = 403`() = blocking {
        val principal = DummyCreateUserPayload.create()
            .let { userFactory.create(it).toPrincipal() }

        val client = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            pop = listOf("clients:read")
        )

        val response = clientControllerGateway.readAll(
            name = "eq:${client.name}",
            sort = "asc:created_at",
            page = 0,
            perPage = 1
        )
        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `GET clients_{client-id}, status = 200`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:read")
        )

        val response = clientControllerGateway.read(otherClient.id)

        assertEquals(HttpStatus.OK, response.status)

        val clientInfo = response.responseBody.awaitSingle()

        assertEquals(otherClient.id, clientInfo.id?.orElse(null))
        assertEquals(otherClient.name, clientInfo.name?.orElse(null))
        assertEquals(otherClient.type, clientInfo.type?.orElse(null))
        assertEquals(otherClient.origin, clientInfo.origin?.orElse(null))
        assertNotNull(clientInfo.createdAt?.orElse(null))
        assertNotNull(clientInfo.updatedAt?.orElse(null))
    }

    @Test
    fun `GET clients_{client-id}, status = 403`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            pop = listOf("clients:read")
        )

        val response = clientControllerGateway.read(otherClient.id)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `GET clients_{client-id}, status = 404`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        otherClient.clear()

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:read")
        )

        val response = clientControllerGateway.read(otherClient.id)

        assertEquals(HttpStatus.NOT_FOUND, response.status)
    }

    @Test
    fun `PATCH clients_{client-id}, status = 200`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update")
        )

        val name = DummyNameFactory.create(10)
        val request = UpdateClientRequest(
            name = Optional.of(name),
        )
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.OK, response.status)

        val clientInfo = response.responseBody.awaitSingle()

        assertEquals(otherClient.id, clientInfo.id?.orElse(null))
        assertEquals(name, clientInfo.name?.orElse(null))
        assertEquals(otherClient.type, clientInfo.type?.orElse(null))
        assertEquals(otherClient.origin, clientInfo.origin?.orElse(null))
        assertNotNull(clientInfo.createdAt?.orElse(null))
        assertNotNull(clientInfo.updatedAt?.orElse(null))
    }

    @Test
    fun `PATCH clients_{client-id}, status = 200, with grant scope`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        val scope = scopeTokenFactory.upsert(DummyNameFactory.create(10))

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update", "clients.scope:create")
        )

        val finalScope = otherClient.getScope(false).toSet().toMutableSet().also { it.add(scope) }
        val request = UpdateClientRequest(scope = Optional.of(finalScope.map { it.id }))
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.OK, response.status)
        assertTrue(otherClient.has(scope))
    }

    @Test
    fun `PATCH clients_{client-id}, status = 403, with grant scope`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        val scope = scopeTokenFactory.upsert(DummyNameFactory.create(10))

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update"),
            pop = listOf("clients.scope:create")
        )

        val finalScope = otherClient.getScope(false).toSet().toMutableSet().also { it.add(scope) }
        val request = UpdateClientRequest(scope = Optional.of(finalScope.map { it.id }))
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `PATCH clients_{client-id}, status = 200, with revoke scope`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        val scope = scopeTokenFactory.upsert(DummyNameFactory.create(10))

        otherClient.grant(scope)

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update", "clients.scope:delete")
        )

        val finalScope = otherClient.getScope(false).toSet().toMutableSet().also { it.remove(scope) }
        val request = UpdateClientRequest(scope = Optional.of(finalScope.map { it.id }))
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.OK, response.status)
        assertFalse(otherClient.has(scope))
    }

    @Test
    fun `PATCH clients_{client-id}, status = 403, with revoke scope`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        val scope = scopeTokenFactory.upsert(DummyNameFactory.create(10))

        otherClient.grant(scope)

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update"),
            pop = listOf("clients.scope:delete")
        )

        val finalScope = otherClient.getScope(false).toSet().toMutableSet().also { it.remove(scope) }
        val request = UpdateClientRequest(scope = Optional.of(finalScope.map { it.id }))
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `PATCH clients_{client-id}, status = 400, when name is null`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update")
        )

        val request = UpdateClientRequest(
            name = Optional.empty(),
        )
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun `PATCH clients_{client-id}, status = 400, when name is excess size`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:update")
        )

        val name = DummyNameFactory.create(25)
        val request = UpdateClientRequest(
            name = Optional.of(name),
        )
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun `PATCH clients_{client-id}, status = 403`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            pop = listOf("clients:update")
        )

        val request = UpdateClientRequest(
            name = Optional.of(DummyNameFactory.create(10)),
            origin = Optional.of(otherClient.origin)
        )
        val response = clientControllerGateway.update(otherClient.id, request)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }

    @Test
    fun `DELEATE clients_{client-id}, status = 200`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            push = listOf("clients:delete")
        )

        val response = clientControllerGateway.delete(otherClient.id)

        assertEquals(HttpStatus.NO_CONTENT, response.status)

        val clientScope = otherClient.getScope().toSet()
        assertEquals(0, clientScope.size)
    }

    @Test
    fun `DELEATE clients_{client-id}, status = 403`() = blocking {
        val principal = DummyCreateClientPayload.create()
            .let { clientFactory.create(it).toPrincipal() }

        val otherClient = DummyCreateClientPayload.create()
            .let { clientFactory.create(it) }

        gatewayAuthorization.setPrincipal(
            principal,
            pop = listOf("clients:delete")
        )

        val response = clientControllerGateway.delete(otherClient.id)

        assertEquals(HttpStatus.FORBIDDEN, response.status)
    }
}
