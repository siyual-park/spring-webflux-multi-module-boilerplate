package io.github.siyual_park.data.test.repository.r2dbc

import com.google.common.cache.CacheBuilder
import io.github.siyual_park.data.patch.AsyncPatch
import io.github.siyual_park.data.patch.Patch
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepository
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepositoryBuilder
import io.github.siyual_park.data.repository.r2dbc.SimpleR2DBCRepository
import io.github.siyual_park.data.repository.r2dbc.findOneOrFail
import io.github.siyual_park.data.repository.r2dbc.where
import io.github.siyual_park.data.test.R2DBCTest
import io.github.siyual_park.data.test.dummy.DummyPerson
import io.github.siyual_park.data.test.entity.Person
import io.github.siyual_park.data.test.repository.r2dbc.migration.CreatePerson
import io.github.siyual_park.ulid.ULID
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.transaction.reactive.executeAndAwait
import java.time.Duration

class R2DBCRepositoryTest : R2DBCTest() {
    init {
        migrationManager.register(CreatePerson(entityOperations))
    }

    @Test
    fun create() = parameterized { personRepository ->
        val person = DummyPerson.create()
        val savedPerson = personRepository.create(person)

        assertNotNull(savedPerson.id)
        assertNotNull(savedPerson.createdAt)
        assertNotNull(savedPerson.updatedAt)

        assertEquals(person.name, savedPerson.name)
        assertEquals(person.age, savedPerson.age)
    }

    @Test
    fun createAll() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { DummyPerson.create() }
        val savedPersons = personRepository.createAll(persons).toList()

        assertEquals(persons.size, savedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val savedPerson = savedPersons[i]

            assertNotNull(savedPerson.id)
            assertNotNull(savedPerson.createdAt)
            assertNotNull(savedPerson.updatedAt)

            assertEquals(person.name, savedPerson.name)
            assertEquals(person.age, savedPerson.age)
        }
    }

    @Test
    fun existsById() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }

        assertTrue(personRepository.existsById(person.id))
    }

    @Test
    fun findById() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPerson = personRepository.findById(person.id)!!

        assertEquals(person.id, foundPerson.id)
        assertEquals(person.createdAt, foundPerson.createdAt)
        assertEquals(person.updatedAt, foundPerson.updatedAt)

        assertEquals(person.name, foundPerson.name)
        assertEquals(person.age, foundPerson.age)
    }

    @Test
    fun findAll() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPersons = personRepository.findAll().toList()

        assertEquals(foundPersons.size, 1)
        assertEquals(person.id, foundPersons[0].id)
        assertEquals(person.createdAt, foundPersons[0].createdAt)
        assertEquals(person.updatedAt, foundPersons[0].updatedAt)

        assertEquals(person.name, foundPersons[0].name)
        assertEquals(person.age, foundPersons[0].age)
    }

    @Test
    fun findAllCustomQuery() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPersons = personRepository.findAll(where(Person::id).`is`(person.id)).toList()

        assertEquals(foundPersons.size, 1)
        assertEquals(person.id, foundPersons[0].id)
        assertEquals(person.createdAt, foundPersons[0].createdAt)
        assertEquals(person.updatedAt, foundPersons[0].updatedAt)

        assertEquals(person.name, foundPersons[0].name)
        assertEquals(person.age, foundPersons[0].age)
    }

    @Test
    fun findAllByNameIs() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPersons = personRepository.findAll(where(Person::name).`is`(person.name)).toList()

        assertEquals(foundPersons.size, 1)
        assertEquals(person.id, foundPersons[0].id)
        assertEquals(person.createdAt, foundPersons[0].createdAt)
        assertEquals(person.updatedAt, foundPersons[0].updatedAt)

        assertEquals(person.name, foundPersons[0].name)
        assertEquals(person.age, foundPersons[0].age)
    }

    @Test
    fun findAllByNameIn() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPersons = personRepository.findAll(where(Person::name).`in`(person.name)).toList()

        assertEquals(foundPersons.size, 1)
        assertEquals(person.id, foundPersons[0].id)
        assertEquals(person.createdAt, foundPersons[0].createdAt)
        assertEquals(person.updatedAt, foundPersons[0].updatedAt)

        assertEquals(person.name, foundPersons[0].name)
        assertEquals(person.age, foundPersons[0].age)
    }

    @Test
    fun findOneByName() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val foundPerson = personRepository.findOneOrFail(where(Person::name).`is`(person.name))

        assertEquals(person.id, foundPerson.id)
        assertEquals(person.createdAt, foundPerson.createdAt)
        assertEquals(person.updatedAt, foundPerson.updatedAt)

        assertEquals(person.name, foundPerson.name)
        assertEquals(person.age, foundPerson.age)
    }

    @Test
    fun findAllById() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()
        val ids = persons.map { it.id }

        val foundPersons = personRepository.findAllById(ids).toList()

        assertEquals(persons.size, foundPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val foundPerson = foundPersons[i]

            assertNotNull(foundPerson.id)
            assertNotNull(foundPerson.createdAt)
            assertNotNull(foundPerson.updatedAt)

            assertEquals(person.name, foundPerson.name)
            assertEquals(person.age, foundPerson.age)
        }
    }

    @Test
    fun update() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val person2 = DummyPerson.create()

        person.name = person2.name
        person.age = person2.age

        val updatedPerson = personRepository.update(person)!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertNotNull(updatedPerson.updatedAt)

        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateByPatch() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val person2 = DummyPerson.create()

        val updatedPerson = personRepository.update(
            person,
            Patch.with {
                it.name = person2.name
                it.age = person2.age
            }
        )!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertNotNull(updatedPerson.updatedAt)

        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateByAsyncPatch() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }
        val person2 = DummyPerson.create()

        val updatedPerson = personRepository.update(
            person,
            AsyncPatch.with {
                it.name = person2.name
                it.age = person2.age
            }
        )!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertNotNull(updatedPerson.updatedAt)

        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateAll() = parameterized { personRepository ->
        val numOfPerson = 10

        var persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()

        persons = persons.map {
            val person2 = DummyPerson.create()
            it.name = person2.name
            it.age = person2.age
            it
        }

        val updatedPersons = personRepository.updateAll(persons).toList()

        assertEquals(persons.size, updatedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val updatedPerson = updatedPersons[i]!!

            assertNotNull(updatedPerson.id)
            assertNotNull(updatedPerson.createdAt)
            assertNotNull(updatedPerson.updatedAt)

            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun updateAllByPatch() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()

        val updatedPersons = personRepository.updateAll(
            persons,
            Patch.with {
                val person2 = DummyPerson.create()
                it.name = person2.name
                it.age = person2.age
            }
        ).toList()

        assertEquals(persons.size, updatedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val updatedPerson = updatedPersons[i]!!

            assertNotNull(updatedPerson.id)
            assertNotNull(updatedPerson.createdAt)
            assertNotNull(updatedPerson.updatedAt)

            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun updateAllByAsyncPatch() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()

        val updatedPersons = personRepository.updateAll(
            persons,
            AsyncPatch.with {
                val person2 = DummyPerson.create()
                it.name = person2.name
                it.age = person2.age
            }
        ).toList()

        assertEquals(persons.size, updatedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val updatedPerson = updatedPersons[i]!!

            assertNotNull(updatedPerson.id)
            assertNotNull(updatedPerson.createdAt)
            assertNotNull(updatedPerson.updatedAt)

            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun count() = parameterized { personRepository ->
        assertEquals(personRepository.count(), 0L)

        val numOfPerson = 10
        val persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()

        assertEquals(personRepository.count(), persons.size.toLong())
    }

    @Test
    fun delete() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }

        personRepository.delete(person)

        assertFalse(personRepository.existsById(person.id))
    }

    @Test
    fun deleteById() = parameterized { personRepository ->
        val person = DummyPerson.create()
            .let { personRepository.create(it) }

        personRepository.deleteById(person.id)

        assertFalse(personRepository.existsById(person.id))
    }

    @Test
    fun deleteAll() = parameterized { personRepository ->
        DummyPerson.create()
            .let { personRepository.create(it) }

        personRepository.deleteAll()

        assertEquals(0, personRepository.count())
    }

    @Test
    fun deleteAllById() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()
        val ids = persons.map { it.id }

        personRepository.deleteAllById(ids)

        assertEquals(0, personRepository.count())
    }

    @Test
    fun deleteAllByEntity() = parameterized { personRepository ->
        val numOfPerson = 10

        val persons = (0 until numOfPerson)
            .map { DummyPerson.create() }
            .let { personRepository.createAll(it) }
            .toList()

        personRepository.deleteAll(persons)

        assertEquals(0, personRepository.count())
    }

    @Test
    fun transactionCommit() = parameterized { personRepository ->
        var person: Person? = null

        transactionalOperator.executeAndAwait {
            person = DummyPerson.create()
                .let { personRepository.create(it) }
        }

        assertTrue(personRepository.findById(person?.id!!) != null)
        assertTrue(personRepository.existsById(person?.id!!))
    }

    @Test
    fun transactionRollback() = blocking {
        repositories().forEach { personRepository ->
            var person: Person? = null

            assertThrows<RuntimeException> {
                transactionalOperator.executeAndAwait {
                    it.setRollbackOnly()
                    person = DummyPerson.create()
                        .let { personRepository.create(it) }
                    throw RuntimeException()
                }
            }

            assertTrue(personRepository.findById(person?.id!!) == null)
            assertFalse(personRepository.existsById(person?.id!!))
        }
    }

    private fun parameterized(func: suspend (R2DBCRepository<Person, ULID>) -> Unit) {
        transactional {
            repositories().forEach {
                func(it)
                migrationManager.revert()
                migrationManager.run()
            }
        }
        blocking {
            repositories().forEach {
                func(it)
                migrationManager.revert()
                migrationManager.run()
            }
        }
    }

    private fun repositories(): List<R2DBCRepository<Person, ULID>> {
        return listOf(
            SimpleR2DBCRepository(entityOperations, Person::class),
            R2DBCRepositoryBuilder<Person, ULID>(entityOperations, Person::class)
                .set(
                    CacheBuilder.newBuilder()
                        .softValues()
                        .expireAfterAccess(Duration.ofMinutes(2))
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(1_000)
                )
                .build()
        )
    }
}
