package io.github.siyual_park.data.repository.cache

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import io.github.siyual_park.data.repository.Extractor
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
class InMemoryNestedStorageNode<T : Any, ID : Any>(
    override val parent: NestedStorage<T, ID>
) : NestedStorage<T, ID> {
    override val idExtractor = parent.idExtractor

    private val indexes = Maps.newConcurrentMap<String, MutableMap<*, ID>>()
    private val extractors = Maps.newConcurrentMap<String, Extractor<T, *>>()

    private val data = Maps.newConcurrentMap<ID, T>()
    private val forceRemoved = Sets.newConcurrentHashSet<ID>()

    override fun diff(): Pair<Set<T>, Set<ID>> {
        return data.values.toSet() to forceRemoved.toSet()
    }

    override suspend fun fork(): NestedStorage<T, ID> {
        return InMemoryNestedStorageNode(
            this
        ).also {
            getExtractors().forEach { (name, extractor) ->
                it.createIndex(name, extractor as Extractor<T, Any>)
            }
        }
    }

    override suspend fun merge(storage: NestedStorage<T, ID>) {
        val (created, removed) = storage.diff()
        storage.clear()

        removed.forEach {
            remove(it)
        }
        created.forEach {
            put(it)
        }
    }

    override fun <KEY : Any> createIndex(name: String, extractor: Extractor<T, KEY>) {
        indexes[name] = ConcurrentHashMap<KEY, ID>()
        extractors[name] = extractor
    }

    override fun removeIndex(name: String) {
        indexes.remove(name)
        extractors.remove(name)
    }

    override fun getExtractors(): Map<String, Extractor<T, *>> {
        return extractors
    }

    override fun containsIndex(name: String): Boolean {
        return indexes.keys.contains(name)
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY): T? {
        val fallback = suspend {
            parent.getIfPresent(index, key)
                ?.let {
                    val id = idExtractor.getKey(it)
                    if (!forceRemoved.contains(id)) {
                        it
                    } else {
                        null
                    }
                }
        }

        val indexMap = indexes[index] ?: return fallback()
        val id = indexMap[key] ?: return fallback()

        return getIfPresent(id)
    }

    override suspend fun <KEY : Any> getIfPresent(index: String, key: KEY, loader: suspend () -> T?): T? {
        val indexMap = getIndex(index)
        val id = indexMap[key]

        return if (id == null) {
            (parent.getIfPresent(index, key)?.also { put(it) } ?: loader()?.also { put(it) })
                ?.let {
                    val id = idExtractor.getKey(it)
                    if (!forceRemoved.contains(id)) {
                        it
                    } else {
                        null
                    }
                }
        } else {
            getIfPresent(id, loader)
        }
    }

    override suspend fun getIfPresent(id: ID, loader: suspend () -> T?): T? {
        return getIfPresent(id)
            ?: loader()?.also { put(it) }
    }

    override suspend fun getIfPresent(id: ID): T? {
        return data[id] ?: if (!forceRemoved.contains(id)) {
            parent.getIfPresent(id)?.also { put(it) }
        } else {
            null
        }
    }

    override suspend fun remove(id: ID) {
        data[id]?.let { entity ->
            indexes.forEach { (name, index) ->
                val extractor = extractors[name] ?: return@forEach
                index.remove(extractor.getKey(entity))
            }
        }
        data.remove(id)
        forceRemoved.add(id)
    }

    override suspend fun delete(entity: T) {
        idExtractor.getKey(entity)?.let { remove(it) }
    }

    override suspend fun put(entity: T) {
        val id = idExtractor.getKey(entity) ?: return
        data[id] = entity
        forceRemoved.remove(id)

        indexes.forEach { (name, index) ->
            val extractor = extractors[name] ?: return@forEach
            val key = extractor.getKey(entity) ?: return@forEach

            index as MutableMap<Any, ID>
            extractor as Extractor<T, Any>

            index[key] = id
        }
    }

    override suspend fun clear() {
        data.clear()
        forceRemoved.clear()
        indexes.forEach { (_, index) -> index.run { clear() } }
    }

    private fun getIndex(index: String): MutableMap<*, ID> {
        return indexes[index] ?: throw RuntimeException("Can't find index.")
    }
}
