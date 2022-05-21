package io.github.siyual_park.data.repository.cache

import io.github.siyual_park.data.cache.AsyncLazy
import io.github.siyual_park.data.cache.AsyncPool
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNestedQueryStorage<T : Any>(
    private val pool: AsyncPool<QueryStorage<T>>,
    override val parent: NestedQueryStorage<T>? = null
) : NestedQueryStorage<T> {
    private val delegator = AsyncLazy { pool.poll() }
    private val mutex = Mutex()

    override suspend fun getIfPresent(where: String): T? {
        return parent?.getIfPresent(where) ?: delegator.get().getIfPresent(where)
    }

    override suspend fun getIfPresent(where: String, loader: suspend () -> T?): T? {
        return parent?.getIfPresent(where) ?: delegator.get().getIfPresent(where, loader)
    }

    override suspend fun getIfPresent(select: SelectQuery): Collection<T>? {
        return parent?.getIfPresent(select) ?: delegator.get().getIfPresent(select)
    }

    override suspend fun getIfPresent(select: SelectQuery, loader: suspend () -> Collection<T>?): Collection<T>? {
        return parent?.getIfPresent(select) ?: delegator.get().getIfPresent(select, loader)
    }

    override suspend fun remove(where: String) {
        delegator.get().remove(where)
    }

    override suspend fun remove(select: SelectQuery) {
        delegator.get().remove(select)
    }

    override suspend fun put(where: String, value: T) {
        delegator.get().put(where, value)
    }

    override suspend fun put(select: SelectQuery, value: Collection<T>) {
        delegator.get().put(select, value)
    }

    override suspend fun clear() {
        delegator.get().clear()
        mutex.withLock {
            pool.add(delegator.get())
            delegator.clear()
        }

        parent?.clear()
    }

    override suspend fun entries(): Pair<Set<Pair<String, T>>, Set<Pair<SelectQuery, Collection<T>>>> {
        return delegator.get().entries()
    }

    override suspend fun diff(): Pair<Set<Pair<String, T>>, Set<Pair<SelectQuery, Collection<T>>>> {
        return entries().also {
            delegator.get().clear()
            mutex.withLock {
                pool.add(delegator.get())
                delegator.clear()
            }
        }
    }

    override suspend fun fork(): NestedQueryStorage<T> {
        return InMemoryNestedQueryStorage(pool, this)
    }

    override suspend fun merge(storage: NestedQueryStorage<T>) {
        val (single, multi) = storage.diff()
        single.forEach { (key, value) ->
            delegator.get().put(key, value)
        }
        multi.forEach { (key, value) ->
            delegator.get().put(key, value)
        }
    }
}
