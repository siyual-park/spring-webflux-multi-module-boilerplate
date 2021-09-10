package io.github.siyual_park.data.factory

import io.github.siyual_park.data.mock.Person
import java.util.UUID
import kotlin.random.Random.Default.nextInt

class PersonFactory {
    fun create() = Person(
        name = UUID.randomUUID().toString().slice(0..10),
        age = nextInt(0, 100)
    )
}