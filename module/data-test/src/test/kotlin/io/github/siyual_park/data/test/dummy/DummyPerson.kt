package io.github.siyual_park.data.test.dummy

import io.github.siyual_park.data.test.entity.Person
import io.github.siyual_park.util.Presence
import java.util.UUID
import kotlin.random.Random

object DummyPerson {
    data class PersonTemplate(
        val name: Presence<String> = Presence.Empty(),
        val age: Presence<Int> = Presence.Empty(),
    )

    fun create(template: PersonTemplate? = null): Person {
        val t = Presence.ofNullable(template)
        return Person(
            name = t.flatMap { it.name }.orElseGet { UUID.randomUUID().toString().slice(0..10) },
            age = t.flatMap { it.age }.orElseGet { Random.nextInt() }
        )
    }
}