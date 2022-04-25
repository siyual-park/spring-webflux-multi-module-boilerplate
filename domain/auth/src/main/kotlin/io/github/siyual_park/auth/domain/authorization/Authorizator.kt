package io.github.siyual_park.auth.domain.authorization

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.scope_token.ScopeToken
import io.github.siyual_park.auth.domain.scope_token.ScopeTokenStorage
import org.springframework.stereotype.Component
import java.time.Duration

@Suppress("UNCHECKED_CAST")
@Component
class Authorizator(
    private val scopeTokenStorage: ScopeTokenStorage,
    private val cache: Cache<ArrayList<Any?>, Boolean> = CacheBuilder.newBuilder()
        .softValues()
        .expireAfterAccess(Duration.ofMinutes(1))
        .expireAfterWrite(Duration.ofMinutes(2))
        .maximumSize(1_0000)
        .build()
) {
    private val strategies = mutableListOf<Pair<AuthorizeFilter, AuthorizeStrategy>>()

    fun register(filter: AuthorizeFilter, strategy: AuthorizeStrategy): Authorizator {
        strategies.add(filter to strategy)
        return this
    }

    suspend fun authorize(
        principal: Principal,
        scope: List<*>,
        targetDomainObjects: List<*>? = null
    ): Boolean {
        return authorizeWithOr(principal, scope, targetDomainObjects)
    }

    private suspend fun authorizeWithOr(
        principal: Principal,
        scope: List<*>,
        targetDomainObjects: List<*>? = null
    ): Boolean {
        if (targetDomainObjects != null && scope.size != targetDomainObjects.size) {
            return false
        }

        for (i in scope.indices) {
            val scopeToken = scope[i]
            if (scopeToken is ScopeToken) {
                if (authorize(principal, scopeToken, targetDomainObjects?.get(i))) {
                    return true
                }
            } else if (scopeToken is List<*>) {
                if (authorizeWithAnd(principal, scopeToken, targetDomainObjects?.get(i) as? List<Any?>?)) {
                    return true
                }
            } else {
                return false
            }
        }

        return false
    }

    private suspend fun authorizeWithAnd(
        principal: Principal,
        scope: List<*>,
        targetDomainObjects: List<*>? = null
    ): Boolean {
        if (targetDomainObjects != null && scope.size != targetDomainObjects.size) {
            return false
        }

        for (i in scope.indices) {
            val scopeToken = scope[i]
            if (scopeToken is ScopeToken) {
                if (!authorize(principal, scopeToken, targetDomainObjects?.get(i))) {
                    return false
                }
            } else if (scopeToken is List<*>) {
                if (!authorizeWithOr(principal, scopeToken, targetDomainObjects?.get(i) as? List<Any?>?)) {
                    return false
                }
            } else {
                return false
            }
        }

        return true
    }

    suspend fun authorize(
        principal: Principal,
        scopeToken: String,
        targetDomainObject: Any? = null
    ): Boolean {
        return scopeTokenStorage.load(scopeToken)?.let {
            authorize(principal, it, targetDomainObject)
        } ?: return false
    }

    suspend fun authorize(
        principal: Principal,
        scopeToken: ScopeToken,
        targetDomainObject: Any? = null
    ): Boolean {
        val key = cacheKey(principal, scopeToken, targetDomainObject)
        val existed = cache.getIfPresent(key)
        if (existed != null) {
            return existed
        }

        return strategies.filter { (filter, _) -> filter.isSubscribe(principal, scopeToken) }
            .map { (_, evaluator) -> evaluator }
            .all { it.authorize(principal, scopeToken, targetDomainObject) }
            .also { cache.put(key, it) }
    }

    private fun cacheKey(
        principal: Principal,
        scopeToken: ScopeToken,
        targetDomainObject: Any? = null
    ): ArrayList<Any?> {
        return ArrayList<Any?>().apply {
            add(principal)
            add(scopeToken.raw())
            add(targetDomainObject)
        }
    }
}
