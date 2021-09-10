package io.github.siyual_park.data.repository

import io.github.siyual_park.data.patch.Patch
import kotlinx.coroutines.flow.Flow

interface Repository<T : Any, ID : Any> {
    suspend fun <S : T> create(entity: S): S

    fun <S : T> createAll(entities: Iterable<S>): Flow<S>

    suspend fun findById(id: ID): T?

    suspend fun existsById(id: ID): Boolean

    fun findAll(): Flow<T>

    fun findAllById(ids: Iterable<ID>): Flow<T>

    suspend fun updateById(id: ID, patch: Patch<T>): T?

    suspend fun update(entity: T, patch: Patch<T>): T?

    fun updateAllById(ids: Iterable<ID>, patch: Patch<T>): Flow<T?>

    fun updateAll(entity: Iterable<T>, patch: Patch<T>): Flow<T?>

    suspend fun count(): Long

    suspend fun deleteById(id: ID)

    suspend fun delete(entity: T)

    suspend fun deleteAllById(ids: Iterable<ID>)

    suspend fun deleteAll(entities: Iterable<T>)

    suspend fun deleteAll()

}
