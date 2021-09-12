package io.github.siyual_park.user.domain

import io.github.siyual_park.auth.domain.authenticator.Authenticator
import io.github.siyual_park.auth.domain.hash
import io.github.siyual_park.user.exception.PasswordIncorrectException
import io.github.siyual_park.user.repository.UserCredentialRepository
import io.github.siyual_park.user.repository.UserRepository
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class PasswordGrantAuthenticator(
    private val userRepository: UserRepository,
    private val userCredentialRepository: UserCredentialRepository,
    private val userPrincipalExchanger: UserPrincipalExchanger,
) : Authenticator<PasswordGrantPayload, UserPrincipal> {
    override val payloadClazz = PasswordGrantPayload::class

    override suspend fun authenticate(payload: PasswordGrantPayload): UserPrincipal {
        val user = userRepository.findByNameOrFail(payload.username)
        val userCredential = userCredentialRepository.findByUserOrFail(user)

        val messageDigest = MessageDigest.getInstance(userCredential.hashAlgorithm)
        val encodedPassword = messageDigest.hash(payload.password)

        if (userCredential.password != encodedPassword) {
            throw PasswordIncorrectException()
        }

        return userPrincipalExchanger.exchange(user)
    }
}
