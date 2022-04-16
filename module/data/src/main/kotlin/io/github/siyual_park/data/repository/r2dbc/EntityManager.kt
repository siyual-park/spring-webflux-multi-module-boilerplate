package io.github.siyual_park.data.repository.r2dbc

import io.github.siyual_park.data.expansion.columnName
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.data.r2dbc.mapping.OutboundRow
import org.springframework.data.r2dbc.query.UpdateMapper
import org.springframework.data.r2dbc.support.ArrayUtils
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.data.util.ProxyUtils
import org.springframework.r2dbc.core.Parameter
import org.springframework.util.CollectionUtils
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Suppress("UNCHECKED_CAST")
class EntityManager<T : Any, ID : Any>(
    entityOperations: R2dbcEntityOperations,
    val clazz: KClass<T>,
) {
    private val client = entityOperations.databaseClient
    private val converter = entityOperations.converter
    private val dialect = DialectResolver.getDialect(client.connectionFactory)
    private val updateManager = UpdateMapper(dialect, converter)

    private val mappingContext = converter.mappingContext

    private val idGetter: KProperty1<T, ID>
    val idColumn: SqlIdentifier
    val idProperty: String

    init {
        val persistentEntity = getRequiredEntity(clazz.java)

        idColumn = persistentEntity.requiredIdProperty.columnName
        idProperty = updateManager.toSql(idColumn)
        idGetter = (
            clazz.memberProperties.find { columnName(it) == idProperty }
                ?: throw RuntimeException("")
            ) as KProperty1<T, ID>
    }

    fun getId(entity: T): ID {
        return idGetter.get(entity)
    }

    fun getOutboundRow(entity: T): OutboundRow {
        val row = OutboundRow()

        converter.write(entity, row)

        val persistentEntity = getRequiredEntity(entity)
        for (property in persistentEntity) {
            val value = row[property.columnName]
            if (value.value != null && shouldConvertArrayValue(property, value)) {
                val writeValue = getArrayValue(value, property)
                row[property.columnName] = writeValue
            }
        }

        return row
    }

    private fun shouldConvertArrayValue(property: RelationalPersistentProperty, value: Parameter): Boolean {
        if (!property.isCollectionLike) {
            return false
        }
        if (value.hasValue() && (value.value is Collection<*> || value.value!!.javaClass.isArray)) {
            return true
        }
        return MutableCollection::class.java.isAssignableFrom(value.type) || value.type.isArray
    }

    private fun getArrayValue(value: Parameter, property: RelationalPersistentProperty): Parameter {
        if (value.type == ByteArray::class.java) {
            return value
        }
        val arrayColumns = dialect.arraySupport
        if (!arrayColumns.isSupported) {
            throw InvalidDataAccessResourceUsageException("Dialect ${dialect.javaClass.name} does not support array columns")
        }
        var actualType: Class<*>? = null
        if (value.value is Collection<*>) {
            actualType = CollectionUtils.findCommonElementType((value.value as Collection<*>?)!!)
        } else if (!value.isEmpty && value.value!!.javaClass.isArray) {
            actualType = value.value!!.javaClass.componentType
        }
        if (actualType == null) {
            actualType = property.actualType
        }
        actualType = converter.getTargetType(actualType)
        if (value.isEmpty) {
            val targetType = arrayColumns.getArrayType(actualType)
            val depth = if (actualType.isArray) ArrayUtils.getDimensionDepth(actualType) else 1
            val targetArrayType = ArrayUtils.getArrayClass(targetType, depth)
            return Parameter.empty(targetArrayType)
        }
        return Parameter.fromOrEmpty(
            converter.getArrayValue(
                arrayColumns, property,
                value.value!!
            ),
            actualType
        )
    }

    fun getRequiredEntity(entity: T): RelationalPersistentEntity<T> {
        val entityType = ProxyUtils.getUserClass(entity)
        return getRequiredEntity(entityType) as RelationalPersistentEntity<T>
    }

    fun getRequiredEntity(entityClass: Class<*>): RelationalPersistentEntity<*> {
        return mappingContext.getRequiredPersistentEntity(entityClass)
    }

    fun getProperty(sql: SqlIdentifier): KProperty1<T, *>? {
        val requiredEntity = mappingContext.getRequiredPersistentEntity(clazz.java)
        val current = requiredEntity.find { it.columnName == sql } ?: return null

        return clazz.memberProperties.find { it.javaField == current.field }
    }
}
