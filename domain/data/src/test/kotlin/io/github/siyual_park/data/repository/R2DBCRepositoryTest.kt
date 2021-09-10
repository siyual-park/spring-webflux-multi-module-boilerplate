package io.github.siyual_park.data.repository

import io.github.siyual_park.data.R2DBCTest
import io.github.siyual_park.data.factory.PersonFactory
import io.github.siyual_park.data.migration.CreatePersonCheckpoint
import io.github.siyual_park.data.mock.Person
import io.github.siyual_park.data.patch.AsyncPatch
import io.github.siyual_park.data.patch.Patch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class R2DBCRepositoryTest : R2DBCTest() {
    private val personRepository = R2DBCRepository<Person, Long>(
        connectionFactory,
        Person::class
    )
    private val personFactory = PersonFactory()

    init {
        migrationManager.register(CreatePersonCheckpoint())
    }

    @Test
    fun create() = async {
        val person = personFactory.create()
        val savedPerson = personRepository.create(person)

        assertNotNull(savedPerson.id)
        assertNotNull(savedPerson.createdAt)
        assertEquals(person.name, savedPerson.name)
        assertEquals(person.age, savedPerson.age)
    }

    @Test
    fun createAll() = async {
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { personFactory.create() }
        val savedPersons = personRepository.createAll(persons).toList()

        assertEquals(persons.size, savedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val savedPerson = savedPersons[i]

            assertNotNull(savedPerson.id)
            assertNotNull(savedPerson.createdAt)
            assertEquals(person.name, savedPerson.name)
            assertEquals(person.age, savedPerson.age)
        }
    }

    @Test
    fun existsById() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }

        assertTrue(personRepository.existsById(person.id!!))
    }

    @Test
    fun findById() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }
        val foundPerson = personRepository.findById(person.id!!)!!

        assertEquals(person.id, foundPerson.id)
        assertEquals(person.createdAt, foundPerson.createdAt)
        assertEquals(person.name, foundPerson.name)
        assertEquals(person.age, foundPerson.age)
    }

    @Test
    fun findAll() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }
        val foundPersons = personRepository.findAll().toList()

        assertEquals(foundPersons.size, 1)
        assertEquals(person.id, foundPersons[0].id)
        assertEquals(person.createdAt, foundPersons[0].createdAt)
        assertEquals(person.name, foundPersons[0].name)
        assertEquals(person.age, foundPersons[0].age)
    }

    @Test
    fun findAllById() = async {
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()
        val ids = persons.map { it.id!! }

        val foundPersons = personRepository.findAllById(ids).toList()

        assertEquals(persons.size, foundPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val foundPerson = foundPersons[i]

            assertNotNull(foundPerson.id)
            assertNotNull(foundPerson.createdAt)
            assertEquals(person.name, foundPerson.name)
            assertEquals(person.age, foundPerson.age)
        }
    }

    @Test
    fun update() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }
        val person2 = personFactory.create()

        person.name = person2.name
        person.age = person2.age

        val updatedPerson = personRepository.update(person)!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateByPatch() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }
        val person2 = personFactory.create()

        val updatedPerson = personRepository.update(
            person,
            Patch.with {
                it.name = person2.name
                it.age = person2.age
            }
        )!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateByAsyncPatch() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }
        val person2 = personFactory.create()

        val updatedPerson = personRepository.update(
            person,
            AsyncPatch.with {
                it.name = person2.name
                it.age = person2.age
            }
        )!!

        assertEquals(person.id, updatedPerson.id)
        assertEquals(person.createdAt, updatedPerson.createdAt)
        assertEquals(person.name, updatedPerson.name)
        assertEquals(person.age, updatedPerson.age)
    }

    @Test
    fun updateAll() = async {
        val numOfPerson = 10

        val person2 = personFactory.create()
        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .map {
                it.name = person2.name
                it.age = person2.age
                it
            }
            .toList()

        val updatedPersons = personRepository.updateAll(persons).toList()

        assertEquals(persons.size, updatedPersons.size)
        for (i in 0 until numOfPerson) {
            val person = persons[i]
            val updatedPerson = updatedPersons[i]!!

            assertNotNull(updatedPerson.id)
            assertNotNull(updatedPerson.createdAt)
            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun updateAllByPatch() = async {
        val numOfPerson = 10

        val person2 = personFactory.create()
        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()

        val updatedPersons = personRepository.updateAll(
            persons,
            Patch.with {
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
            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun updateAllByAsyncPatch() = async {
        val numOfPerson = 10

        val person2 = personFactory.create()
        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()

        val updatedPersons = personRepository.updateAll(
            persons,
            AsyncPatch.with {
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
            assertEquals(person.name, updatedPerson.name)
            assertEquals(person.age, updatedPerson.age)
        }
    }

    @Test
    fun count() = async {
        assertEquals(personRepository.count(), 0L)

        val numOfPerson = 10
        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()

        assertEquals(personRepository.count(), persons.size.toLong())
    }

    @Test
    fun delete() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }

        personRepository.delete(person)

        assertFalse(personRepository.existsById(person.id!!))
    }

    @Test
    fun deleteById() = async {
        val person = personFactory.create()
            .let { personRepository.create(it) }

        personRepository.deleteById(person.id!!)

        assertFalse(personRepository.existsById(person.id!!))
    }

    @Test
    fun deleteAll() = async {
        personFactory.create()
            .let { personRepository.create(it) }

        personRepository.deleteAll()

        assertEquals(0, personRepository.count())
    }

    @Test
    fun deleteAllById() = async {
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()
        val ids = persons.map { it.id!! }

        personRepository.deleteAllById(ids)

        assertEquals(0, personRepository.count())
    }

    @Test
    fun deleteAllByEntity() = async {
        val numOfPerson = 10

        val persons = (0 until numOfPerson).map { personFactory.create() }
            .let { personRepository.createAll(it) }
            .toList()

        personRepository.deleteAll(persons)

        assertEquals(0, personRepository.count())
    }
}