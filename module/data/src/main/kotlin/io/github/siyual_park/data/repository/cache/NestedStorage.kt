package io.github.siyual_park.data.repository.cache

interface NestedStorage<T : Any, ID : Any> : Storage<T, ID>, GeneralNestedStorage<NestedStorage<T, ID>> {
    suspend fun diff(): Pair<Set<T>, Set<ID>>
}
