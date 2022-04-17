package io.github.siyual_park.auth.repository

import com.google.common.cache.CacheBuilder
import io.github.siyual_park.auth.entity.ScopeRelationData
import io.github.siyual_park.data.expansion.where
import io.github.siyual_park.data.repository.r2dbc.CachedR2DBCRepository
import io.github.siyual_park.data.repository.r2dbc.R2DBCRepository
import io.github.siyual_park.event.EventPublisher
import io.github.siyual_park.ulid.ULID
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class ScopeRelationRepository(
    entityOperations: R2dbcEntityOperations,
    eventPublisher: EventPublisher? = null
) : R2DBCRepository<ScopeRelationData, Long> by CachedR2DBCRepository.of(
    entityOperations,
    ScopeRelationData::class,
    CacheBuilder.newBuilder()
        .softValues()
        .expireAfterAccess(Duration.ofMinutes(10))
        .expireAfterWrite(Duration.ofMinutes(30))
        .maximumSize(1_000),
    eventPublisher = eventPublisher
) {
    fun findAllByChildId(childId: ULID): Flow<ScopeRelationData> {
        return findAll(where(ScopeRelationData::childId).`is`(childId))
    }

    fun findAllByParentId(parentId: ULID): Flow<ScopeRelationData> {
        return findAll(where(ScopeRelationData::parentId).`is`(parentId))
    }

    suspend fun deleteAllByChildId(childId: ULID) {
        return deleteAll(where(ScopeRelationData::childId).`is`(childId))
    }

    suspend fun deleteAllByParentId(parentId: ULID) {
        return deleteAll(where(ScopeRelationData::parentId).`is`(parentId))
    }
}