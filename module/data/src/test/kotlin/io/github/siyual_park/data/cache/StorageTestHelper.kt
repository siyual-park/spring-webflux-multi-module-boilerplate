package io.github.siyual_park.data.cache

import io.github.siyual_park.coroutine.test.CoroutineTestHelper
import io.github.siyual_park.data.dummy.DummyPerson
import io.github.siyual_park.data.entity.Person
import io.github.siyual_park.data.repository.Extractor
import io.github.siyual_park.ulid.ULID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class StorageTestHelper(
    private val storage: Storage<ULID, Person>
) : CoroutineTestHelper() {
    protected val nameIndex = object : Extractor<Person, String> {
        override fun getKey(entity: Person): String {
            return entity.name
        }
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()

        blocking {
            storage.clear()
        }
    }

    @Test
    fun createIndex() = blocking {
        assertFalse(storage.containsIndex("name"))
        storage.createIndex("name", nameIndex)
        assertTrue(storage.containsIndex("name"))
    }

    @Test
    fun removeIndex() = blocking {
        storage.createIndex("name", nameIndex)
        storage.removeIndex("name")
        assertFalse(storage.containsIndex("name"))
    }

    @Test
    fun containsIndex() = blocking {
        assertFalse(storage.containsIndex("name"))
        storage.createIndex("name", nameIndex)
        assertTrue(storage.containsIndex("name"))
    }

    @Test
    fun getExtractors() = blocking {
        assertEquals(emptyMap<String, Extractor<Person, *>>(), storage.getExtractors())
        storage.createIndex("name", nameIndex)
        assertEquals(mapOf("name" to nameIndex), storage.getExtractors())
    }

    @Test
    fun getIfPresent() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        assertNull(storage.getIfPresent(value.id))
        assertNull(storage.getIfPresent("name", value.name))

        storage.put(value)

        assertEquals(value, storage.getIfPresent(value.id))
        assertEquals(value, storage.getIfPresent("name", value.name))

        storage.delete(value)
        assertEquals(value, storage.getIfPresent(value.id) { value })
        storage.delete(value)
        assertEquals(value, storage.getIfPresent("name", value.name) { value })
    }

    @Test
    fun remove() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        storage.put(value)
        storage.remove(value.id)

        assertNull(storage.getIfPresent(value.id))
        assertNull(storage.getIfPresent("name", value.name))
    }

    @Test
    fun delete() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        storage.put(value)
        storage.delete(value)

        assertNull(storage.getIfPresent(value.id))
        assertNull(storage.getIfPresent("name", value.name))
    }

    @Test
    fun put() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        storage.put(value)

        assertEquals(value, storage.getIfPresent(value.id))
        assertEquals(value, storage.getIfPresent("name", value.name))
    }

    @Test
    fun entries() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        assertEquals(emptySet<Pair<ULID, Person>>(), storage.entries())

        storage.put(value)

        assertEquals(setOf(value.id to value), storage.entries())
    }

    @Test
    fun clear() = blocking {
        val value = DummyPerson.create()

        storage.createIndex("name", nameIndex)

        storage.put(value)
        storage.clear()

        assertEquals(emptySet<Pair<ULID, Person>>(), storage.entries())

        assertNull(storage.getIfPresent(value.id))
        assertNull(storage.getIfPresent("name", value.name))
    }
}
