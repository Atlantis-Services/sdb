package net.atlantisservices.sdb

/**
 * Handles parsing and serialization of `.sdb` (SimpleDatabase) flat files.
 *
 * The `.sdb` format stores records as named blocks, each containing key-value pairs:
 * ```
 * [user:1]
 * id=1
 * username=default
 * roles=ADMIN,MOD
 * ```
 */
object SdbParser {

    /**
     * Parses the contents of an `.sdb` file into a list of raw string maps.
     *
     * Each block in the file becomes one map entry. Lines starting with `#` are treated
     * as comments and ignored. Empty lines are skipped.
     *
     * @param text The raw text content of an `.sdb` file.
     * @return A list of maps where each map represents one record's fields.
     */
    fun parse(text: String): List<Map<String, String>> {
        val records = mutableListOf<Map<String, String>>()
        var current = mutableMapOf<String, String>()
        var inBlock = false

        for (line in text.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#") || trimmed.isEmpty() -> continue
                trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                    if (inBlock && current.isNotEmpty()) records += current
                    current = mutableMapOf()
                    inBlock = true
                }
                trimmed.contains("=") && inBlock -> {
                    val (key, value) = trimmed.split("=", limit = 2)
                    current[key.trim()] = value.trim()
                }
            }
        }

        if (inBlock && current.isNotEmpty()) records += current
        return records
    }

    /**
     * Serializes a list of raw string maps into `.sdb` file content.
     *
     * Each map is written as a named block using the provided [prefix] and the record's `id` field.
     * If no `id` field is present, the record's index is used instead.
     *
     * @param prefix The block name prefix (typically the entity type name, e.g. `user`).
     * @param records The list of raw string maps to serialize.
     * @return The full `.sdb` file content as a string.
     */
    fun serialize(prefix: String, records: List<Map<String, String>>): String {
        return buildString {
            records.forEachIndexed { i, record ->
                appendLine("[$prefix]")
                record.forEach { (k, v) -> appendLine("$k=$v") }
                appendLine()
            }
        }
    }

    /**
     * Serializes a single field value to its string representation for storage.
     *
     * - Collections and arrays are stored as comma-separated values.
     * - Maps are stored as `key:value` pairs separated by commas.
     * - Null values are stored as an empty string.
     *
     * @param value The value to serialize.
     * @return The serialized string representation.
     */
    fun serializeValue(value: Any?): String = when (value) {
        null -> ""

        is Collection<*> -> value.joinToString(",") { serializeValue(it) }
        is Array<*> -> value.joinToString(",") { serializeValue(it) }
        is Map<*, *> -> value.entries.joinToString(",") { "${it.key}:${it.value}" }

        else -> {
            val serializer = SdbSerializers.get(value::class.java) as? SdbSerializer<Any>
            serializer?.serialize(value) ?: value.toString()
        }
    }

    /**
     * Deserializes a comma-separated string into a list of string tokens.
     *
     * @param raw The raw comma-separated string from the `.sdb` file.
     * @return A list of trimmed string values, or an empty list if [raw] is blank.
     */
    fun deserializeCollection(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }

    /**
     * Deserializes a comma-separated `key:value` string into a string map.
     *
     * @param raw The raw string from the `.sdb` file (e.g. `theme:dark,lang:en`).
     * @return A map of string keys to string values, or an empty map if [raw] is blank.
     */
    fun deserializeMap(raw: String): Map<String, String> =
        if (raw.isBlank()) emptyMap() else raw.split(",").associate {
            val (k, v) = it.split(":", limit = 2)
            k.trim() to v.trim()
        }
}