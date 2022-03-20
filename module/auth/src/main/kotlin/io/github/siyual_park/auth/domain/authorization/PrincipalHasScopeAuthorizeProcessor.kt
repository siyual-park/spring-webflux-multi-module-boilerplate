package io.github.siyual_park.auth.domain.authorization

import io.github.siyual_park.auth.domain.Principal
import io.github.siyual_park.auth.domain.hasScope
import io.github.siyual_park.auth.entity.ScopeToken
import org.springframework.stereotype.Component

@Component
@AuthorizeMapping(AllowAllAuthorizeFilter::class)
class PrincipalHasScopeAuthorizeProcessor : AuthorizeProcessor {
    override suspend fun authorize(principal: Principal, scopeToken: ScopeToken, targetDomainObject: Any?): Boolean {
        return principal.hasScope(scopeToken)
    }
}
