package io.github.siyual_park.auth.migration

import io.github.siyual_park.data.migration.Migration
import io.github.siyual_park.data.migration.createUniqueIndex
import io.github.siyual_park.data.migration.createUpdatedAtTrigger
import io.github.siyual_park.data.migration.dropTable
import io.github.siyual_park.data.migration.fetchSQL
import io.github.siyual_park.data.migration.isDriver
import org.springframework.data.r2dbc.core.R2dbcEntityOperations

class CreateScopeRelation : Migration {
    private val tableName = "scope_relations"

    override suspend fun up(entityOperations: R2dbcEntityOperations) {
        if (entityOperations.isDriver("PostgreSQL")) {
            entityOperations.fetchSQL(
                "CREATE TABLE $tableName" +
                    "(" +
                    "id SERIAL PRIMARY KEY, " +

                    "parent_id INTEGER NOT NULL REFERENCES scope_tokens (id), " +
                    "child_id INTEGER NOT NULL REFERENCES scope_tokens (id), " +

                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
            )
            entityOperations.createUpdatedAtTrigger(tableName)
        } else {
            entityOperations.fetchSQL(
                "CREATE TABLE $tableName" +
                    "(" +
                    "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +

                    "parent_id BIGINT NOT NULL REFERENCES scope_tokens (id), " +
                    "child_id BIGINT NOT NULL REFERENCES scope_tokens (id), " +

                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")"
            )
        }

        entityOperations.createUniqueIndex(tableName, listOf("parent_id", "child_id"))
    }

    override suspend fun down(entityOperations: R2dbcEntityOperations) {
        entityOperations.dropTable(tableName)
    }
}