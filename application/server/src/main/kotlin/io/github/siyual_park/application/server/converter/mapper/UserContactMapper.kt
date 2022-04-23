package io.github.siyual_park.application.server.converter.mapper

import io.github.siyual_park.application.server.dto.response.UserContactInfo
import io.github.siyual_park.mapper.Mapper
import io.github.siyual_park.mapper.TypeReference
import io.github.siyual_park.user.domain.UserContact
import org.springframework.stereotype.Component

@Component
class UserContactMapper : Mapper<UserContact, UserContactInfo> {
    override val sourceType = object : TypeReference<UserContact>() {}
    override val targetType = object : TypeReference<UserContactInfo>() {}

    override suspend fun map(source: UserContact): UserContactInfo {
        val raw = source.raw()
        return UserContactInfo(
            email = raw.email,
            createdAt = raw.createdAt!!,
            updatedAt = raw.updatedAt
        )
    }
}
