package io.github.siyual_park.data.repository.mongo

import io.github.siyual_park.data.expansion.fieldName
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.KProperty

fun <T> where(property: KProperty<T>): Criteria {
    return Criteria.where(fieldName(property))
}
