package io.github.siyual_park.auth.domain.token

import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.scope_token.ScopeToken
import io.github.siyual_park.auth.entity.TokenData
import io.github.siyual_park.auth.repository.TokenRepository
import io.github.siyual_park.util.retry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.toList
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

class TokenFactory(
    private val template: TokenTemplate,
    private val claimEmbedder: ClaimEmbedder,
    private val tokenStorage: TokenStorage,
    private val tokenRepository: TokenRepository,
    private val tokenMapper: TokenMapper,
) {
    private val random = SecureRandom.getInstance("SHA1PRNG")

    init {
        random.setSeed(random.generateSeed(128))
    }

    suspend fun create(
        principal: Principal,
        age: Duration,
        claims: Map<String, Any>? = null,
        pop: Set<ScopeToken>? = null,
        push: Set<ScopeToken>? = null,
        filter: Set<ScopeToken>? = null,
    ): Token {
        val finalPop = merge(template.pop, pop)
        val finalPush = merge(template.push, push)
        val finalFilter = merge(template.filter, filter)

        val baseClaims = claimEmbedder.embedding(principal)
        val scope = mutableSetOf<ScopeToken>().also { scope ->
            scope.addAll(
                principal.scope
                    .filter { finalFilter?.contains(it) ?: true }
                    .filter { finalPop?.contains(it)?.let { !it } ?: true }
            )
            finalPush?.let { scope.addAll(it) }
        }

        val finalClaims = claims?.toMutableMap() ?: mutableMapOf()
        finalClaims.putAll(baseClaims)
        finalClaims["scope"] = scope.map { it.id.toString() }
        finalClaims["type"] = template.type

        deprecated(finalClaims)

        val now = Instant.now()
        val expiredAt = now.plus(age)
        val data = retry(3) {
            TokenData(
                type = template.type,
                signature = generateSignature(template.type, 40),
                claims = finalClaims,
                expiredAt = expiredAt
            ).let { tokenRepository.create(it) }
        }

        return tokenMapper.map(data)
    }

    private fun merge(x: Set<ScopeToken>?, y: Set<ScopeToken>?): Set<ScopeToken>? {
        if (x == null && y == null) {
            return null
        }

        return mutableSetOf<ScopeToken>()
            .apply {
                x?.let { addAll(it) }
                y?.let { addAll(it) }
            }
    }

    private suspend fun deprecated(claims: Map<String, Any>) {
        val now = Instant.now()
        val expiredAt = now.plus(Duration.ofMinutes(1))

        template.limit?.forEach { (key, limit) ->
            val value = claims[key] ?: return@forEach

            val existed = tokenStorage.load(
                type = template.type,
                claims = mapOf(key to value),
            )
                .toList()

            val removeSize = existed.size - limit + 1
            if (removeSize > 0) {
                existed.subList(0, removeSize).map {
                    CoroutineScope(Dispatchers.IO).async {
                        if (it.expiredAt?.isAfter(expiredAt) != false) {
                            it.expiredAt = expiredAt
                            it.sync()
                        }
                    }
                }.awaitAll()
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun generateSignature(type: String?, length: Int): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val stringBuilder = StringBuilder(length)
        for (i in 0 until length) {
            stringBuilder.append(chars[random.nextInt(chars.length)])
        }

        if (type == null) {
            return stringBuilder.toString()
        }

        return "${type}_$stringBuilder"
    }
}
