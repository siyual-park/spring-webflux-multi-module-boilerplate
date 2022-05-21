package io.github.siyual_park.data.repository.cache

import io.github.siyual_park.data.repository.Extractor

class TransactionalStorage<T : Any, ID : Any>(
    private val root: NestedStorage<T, ID>,
) : Storage<T, ID> {
    private val provider = TransactionalStorageProvider(root)

    override val idExtractor: Extractor<T, ID>
        get() = root.idExtractor

    override fun <KEY : Any> createIndex(name: String, extractor: Extractor<T, KEY>) {
        root.createIndex(name, extractor)
    }

    override fun removeIndex(name: String) {
        root.removeIndex(name)
    }

    override fun containsIndex(name: String): Boolean {
        return root.containsIndex(name)
    }

    override fun getExtractors(): Map<String, Extractor<T, *>> {
        return root.getExtractors()
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY): T? {
        return provider.get().getIfPresent(index, key)
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY, loader: suspend () -> T?): T? {
        return provider.get().getIfPresent(index, key, loader)
    }

    override suspend fun getIfPresent(id: ID): T? {
        return provider.get().getIfPresent(id)
    }

    override suspend fun getIfPresent(id: ID, loader: suspend () -> T?): T? {
        return provider.get().getIfPresent(id, loader)
    }

    override suspend fun remove(id: ID) {
        return provider.get().remove(id)
    }

    override suspend fun delete(entity: T) {
        return provider.get().delete(entity)
    }

    override suspend fun put(entity: T) {
        return provider.get().put(entity)
    }

    override suspend fun clear() {
        return provider.get().clear()
    }
}
