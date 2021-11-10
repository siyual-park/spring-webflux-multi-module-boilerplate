package io.github.siyual_park.auth.domain.authenticator

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class AuthenticateMapping(
    val filterBy: KClass<*>,
)