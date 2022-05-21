package io.github.siyual_park.data.repository.cache

import com.google.common.collect.Sets
import io.github.siyual_park.data.cache.Lazy
import io.github.siyual_park.data.cache.Pool
import io.github.siyual_park.data.repository.Extractor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
class InMemoryNestedStorage<T : Any, ID : Any>(
    private val pool: Pool<Storage<T, ID>>,
    override val parent: NestedStorage<T, ID>? = null
) : NestedStorage<T, ID> {
    private val delegator = Lazy { pool.poll() }
    private val mutex = Mutex()

    private val forceRemoved = Sets.newConcurrentHashSet<ID>()

    override val idExtractor = delegator.get().idExtractor

    override suspend fun diff(): Pair<Set<T>, Set<ID>> {
        return delegator.get().entries().map { it.second }.toSet() to forceRemoved.toSet().also {
            clear()
        }
    }

    override suspend fun fork(): NestedStorage<T, ID> {
        return InMemoryNestedStorage(
            pool,
            this
        ).also {
            getExtractors().forEach { (name, extractor) ->
                it.createIndex(name, extractor as Extractor<T, Any>)
            }
        }
    }

    override suspend fun merge(storage: NestedStorage<T, ID>) {
        val (created, removed) = storage.diff()
        removed.forEach {
            remove(it)
        }
        created.forEach {
            put(it)
        }
    }

    override fun <KEY : Any> createIndex(name: String, extractor: Extractor<T, KEY>) {
        delegator.get().createIndex(name, extractor)
    }

    override fun removeIndex(name: String) {
        delegator.get().removeIndex(name)
    }

    override fun getExtractors(): Map<String, Extractor<T, *>> {
        return delegator.get().getExtractors()
    }

    override fun containsIndex(name: String): Boolean {
        return delegator.get().containsIndex(name)
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY): T? {
        return guard { parent?.getIfPresent(index, key) } ?: delegator.get().getIfPresent(index, key)
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY, loader: suspend () -> T?): T? {
        return guard { parent?.getIfPresent(index, key) } ?: delegator.get().getIfPresent(index, key, withRemove(loader))
    }

    override suspend fun getIfPresent(id: ID, loader: suspend () -> T?): T? {
        return guard { parent?.getIfPresent(id) } ?: delegator.get().getIfPresent(id, withRemove(loader))
    }

    override suspend fun getIfPresent(id: ID): T? {
        return guard { parent?.getIfPresent(id) } ?: delegator.get().getIfPresent(id)
    }

    override suspend fun remove(id: ID) {
        delegator.get().remove(id)
        forceRemoved.add(id)
    }

    override suspend fun delete(entity: T) {
        idExtractor.getKey(entity)?.let { remove(it) }
    }

    override suspend fun put(entity: T) {
        delegator.get().put(entity)
        forceRemoved.remove(idExtractor.getKey(entity))
    }

    override suspend fun clear() {
        delegator.get().clear()
        forceRemoved.clear()
        mutex.withLock {
            pool.add(delegator.get())
            delegator.clear()
        }
    }

    override suspend fun entries(): Set<Pair<ID, T>> {
        return delegator.get().entries()
    }

    private fun withRemove(loader: suspend () -> T?): suspend () -> T? {
        return { loader()?.also { forceRemoved.remove(idExtractor.getKey(it)) } }
    }

    private suspend fun guard(loader: suspend () -> T?): T? {
        return loader() ?.let {
            val id = idExtractor.getKey(it)
            if (!forceRemoved.contains(id)) {
                it
            } else {
                null
            }
        }
    }
}
