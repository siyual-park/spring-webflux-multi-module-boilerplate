package io.github.siyual_park.data.repository.cache

import com.google.common.cache.CacheBuilder
import com.google.common.collect.Sets

@Suppress("UNCHECKED_CAST")
class InMemoryNestedStorage<T : Any, ID : Any>(
    private val cacheBuilder: () -> CacheBuilder<ID, T>,
    private val idExtractor: Extractor<T, ID>,
    override val parent: NestedStorage<T, ID>? = null
) : NestedStorage<T, ID> {
    private val localStorage = InMemoryStorage(cacheBuilder(), idExtractor)
    private val additionalRemoved = Sets.newConcurrentHashSet<ID>()

    override fun getCreated(): Set<T> {
        return localStorage.entries().values.toSet()
    }

    override fun getRemoved(): Set<ID> {
        return additionalRemoved
    }

    override fun fork(): NestedStorage<T, ID> {
        return InMemoryNestedStorage(
            cacheBuilder,
            idExtractor,
            this
        ).also {
            getExtractors().forEach { (name, extractor) ->
                it.createIndex(name, extractor as Extractor<T, Any>)
            }
        }
    }

    override fun join(storage: NestedStorage<T, ID>) {
        val removed = storage.getRemoved()
        val created = storage.getCreated()

        removed.forEach {
            localStorage.remove(it)
        }
        created.forEach {
            localStorage.put(it)
        }
    }

    override fun <KEY : Any> createIndex(name: String, extractor: Extractor<T, KEY>) {
        localStorage.createIndex(name, extractor)
    }

    override fun removeIndex(name: String) {
        localStorage.removeIndex(name)
    }

    override fun getExtractors(): Map<String, Extractor<T, *>> {
        return localStorage.getExtractors()
    }

    override fun containsIndex(name: String): Boolean {
        return localStorage.containsIndex(name)
    }

    override fun <KEY : Any> getIfPresent(key: KEY, index: String): T? {
        return localStorage.getIfPresent(key, index) {
            val value = parent?.getIfPresent(key, index)
            if (value != null && additionalRemoved.contains(idExtractor.getKey(value))) {
                null
            } else {
                value
            }
        }
    }

    override fun <KEY : Any> getIfPresent(key: KEY, index: String, loader: () -> T?): T? {
        return localStorage.getIfPresent(key, index) {
            val value = parent?.getIfPresent(key, index)
            if (value != null && additionalRemoved.contains(idExtractor.getKey(value))) {
                null
            } else {
                value ?: loader()
            }
        }
    }

    override suspend fun <KEY : Any> getIfPresentAsync(key: KEY, index: String, loader: suspend () -> T?): T? {
        return localStorage.getIfPresentAsync(key, index) {
            val value = parent?.getIfPresent(key, index)
            if (value != null && additionalRemoved.contains(idExtractor.getKey(value))) {
                null
            } else {
                value ?: loader()
            }
        }
    }

    override fun getIfPresent(id: ID): T? {
        return localStorage.getIfPresent(id) {
            if (additionalRemoved.contains(id)) {
                null
            } else {
                parent?.getIfPresent(id)
            }
        }
    }

    override fun getIfPresent(id: ID, loader: () -> T?): T? {
        return localStorage.getIfPresent(id) {
            if (additionalRemoved.contains(id)) {
                null
            } else {
                parent?.getIfPresent(id)
            }
        }
    }

    override suspend fun getIfPresentAsync(id: ID, loader: suspend () -> T?): T? {
        return localStorage.getIfPresentAsync(id) {
            if (additionalRemoved.contains(id)) {
                null
            } else {
                parent?.getIfPresentAsync(id, loader)
            }
        }
    }

    override fun remove(id: ID) {
        val local = localStorage.getIfPresent(id)
        if (local != null) {
            localStorage.delete(local)
        } else {
            additionalRemoved.add(id)
        }
    }

    override fun delete(entity: T) {
        val id = idExtractor.getKey(entity) ?: return
        remove(id)
    }

    override fun put(entity: T) {
        val id = idExtractor.getKey(entity) ?: return
        additionalRemoved.remove(id)
        localStorage.put(entity)
    }

    override suspend fun clear() {
        localStorage.clear()
    }
}
