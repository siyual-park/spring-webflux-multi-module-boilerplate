package io.github.siyual_park.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

class NestedQueryStorage<T : Any>(
    private val pool: Pool<QueryStorage<T>>,
    override val parent: NestedQueryStorage<T>? = null,
    private val storages: MutableSet<QueryStorage<T>> = Collections.synchronizedSet(mutableSetOf())
) : QueryStorage<T>, GeneralNestedStorage<NestedQueryStorage<T>> {
    private val delegator = AsyncLazy { pool.poll().also { storages.add(it) } }
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
        storages.forEach { it.clear() }
        mutex.withLock {
            storages.remove(delegator.get())
            pool.add(delegator.get())
            delegator.clear()
        }
    }

    override suspend fun entries(): Pair<Set<Pair<String, T>>, Set<Pair<SelectQuery, Collection<T>>>> {
        return delegator.get().entries()
    }

    suspend fun diff(): Pair<Set<Pair<String, T>>, Set<Pair<SelectQuery, Collection<T>>>> {
        return entries().also {
            delegator.clear()
            mutex.withLock {
                storages.remove(delegator.get())
                pool.add(delegator.get())
                delegator.clear()
            }
        }
    }

    override suspend fun fork(): NestedQueryStorage<T> {
        return NestedQueryStorage(pool, this, storages)
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
