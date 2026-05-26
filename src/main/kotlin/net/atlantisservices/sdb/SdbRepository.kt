package net.atlantisservices.sdb

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * A schema-free flat-file repository backed by an `.sdb` file.
 *
 * Automatically maps records to and from instances of [T] using reflection.
 * All reads and writes are thread-safe.
 *
 * @param T The data class type this repository manages.
 * @param type The [KClass] of [T].
 * @param repository The base name of the `.sdb` file (without extension). Defaults to the lowercase class name.
 *
 * Example:
 * ```kotlin
 * val users = SdbRepository(User::class)
 * val users = SdbRepository(User::class, repository = "players")
 * ```
 */
class SdbRepository<T : Any>(
    private val type: KClass<T>,
    repository: String = type.simpleName!!.lowercase()
) {
    private val file = File("$repository.sdb")
    private val lock = Any()
    private val records = mutableListOf<MutableMap<String, String>>()

    init {
        if (!file.exists()) file.createNewFile()
        records += SdbParser.parse(file.readText()).map { it.toMutableMap() }
    }

    /**
     * Returns all records in the repository as instances of [T].
     *
     * @return A list of all stored entities.
     */
    fun findAll(): List<T> = synchronized(lock) {
        records.mapNotNull { map(it) }
    }

    /**
     * Finds a single record by its `id` field.
     *
     * @param id The ID to look up.
     * @return The matching entity, or `null` if not found.
     */
    fun findById(id: Any): T? = synchronized(lock) {
        records.find { it["id"] == id.toString() }?.let { map(it) }
    }

    /**
     * Finds the first record where [key] equals [value].
     *
     * @param key The field name to match against.
     * @param value The value to match.
     * @return The first matching entity, or `null` if not found.
     */
    fun findBy(key: String, value: Any): T? = synchronized(lock) {
        records.find { it[key] == value.toString() }?.let { map(it) }
    }

    /**
     * Finds all records where [key] equals [value].
     *
     * @param key The field name to match against.
     * @param value The value to match.
     * @return A list of all matching entities.
     */
    fun findAllBy(key: String, value: Any): List<T> = synchronized(lock) {
        records.filter { it[key] == value.toString() }.mapNotNull { map(it) }
    }

    /**
     * Saves an entity to the repository.
     *
     * If a record with the same `id` already exists it will be updated, otherwise it is inserted.
     * Changes are immediately flushed to disk.
     *
     * @param entity The entity to save.
     */
    fun save(entity: T) = synchronized(lock) {
        val serialized = serialize(entity)
        val id = serialized["id"]
        val existing = records.indexOfFirst { it["id"] == id }
        if (existing >= 0) records[existing] = serialized else records += serialized
        flush()
    }

    /**
     * Deletes a record by its `id` field.
     *
     * Changes are immediately flushed to disk.
     *
     * @param id The ID of the record to delete.
     */
    fun deleteById(id: Any) = synchronized(lock) {
        records.removeIf { it["id"] == id.toString() }
        flush()
    }

    /**
     * Returns the total number of records in the repository.
     *
     * @return The record count.
     */
    fun count(): Int = synchronized(lock) { records.size }

    private fun map(raw: Map<String, String>): T? {
        val ctor = type.primaryConstructor ?: return null
        val args = ctor.parameters.associateWith { param ->
            val value = raw[param.name] ?: return@associateWith null
            coerce(value, param.type)
        }
        return runCatching { ctor.callBy(args) }.getOrNull()
    }

    private fun serialize(entity: T): MutableMap<String, String> {
        return type.memberProperties
            .associate { it.name to SdbParser.serializeValue(it.get(entity)) }
            .toMutableMap()
    }

    private fun coerce(value: String, ktype: KType): Any? {
        val classifier = ktype.classifier as? KClass<*> ?: return value
        val typeArgs = ktype.arguments

        return when (classifier) {
            String::class -> value
            Int::class -> value.toIntOrNull()
            Long::class -> value.toLongOrNull()
            Double::class -> value.toDoubleOrNull()
            Float::class -> value.toFloatOrNull()
            Boolean::class -> value.toBooleanStrictOrNull()
            List::class, Collection::class -> {
                val inner = typeArgs.firstOrNull()?.type?.classifier as? KClass<*>
                SdbParser.deserializeCollection(value).map { coercePrimitive(it, inner) }
            }
            Set::class -> {
                val inner = typeArgs.firstOrNull()?.type?.classifier as? KClass<*>
                SdbParser.deserializeCollection(value).map { coercePrimitive(it, inner) }.toSet()
            }
            Array::class -> {
                val inner = typeArgs.firstOrNull()?.type?.classifier as? KClass<*>
                SdbParser.deserializeCollection(value).map { coercePrimitive(it, inner) }.toTypedArray()
            }
            Map::class -> {
                val keyType = typeArgs.getOrNull(0)?.type?.classifier as? KClass<*>
                val valType = typeArgs.getOrNull(1)?.type?.classifier as? KClass<*>
                SdbParser.deserializeMap(value).entries.associate {
                    coercePrimitive(it.key, keyType) to coercePrimitive(it.value, valType)
                }
            }
            else -> value
        }
    }

    private fun coercePrimitive(value: String, type: KClass<*>?): Any = when (type) {
        Int::class -> value.toIntOrNull() ?: value
        Long::class -> value.toLongOrNull() ?: value
        Double::class -> value.toDoubleOrNull() ?: value
        Float::class -> value.toFloatOrNull() ?: value
        Boolean::class -> value.toBooleanStrictOrNull() ?: value
        else -> value
    }

    private fun flush() {
        file.writeText(SdbParser.serialize(type.simpleName!!.lowercase(), records))
    }
}