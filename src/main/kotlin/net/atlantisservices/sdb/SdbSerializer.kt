package net.atlantisservices.sdb

/**
 * A pluggable serializer for custom types in SimpleDatabase (SDB).
 *
 * Implement this interface to enable storing and loading complex or non-primitive
 * types inside `.sdb` files.
 *
 * ## Example
 * ```kotlin
 * object UUIDSerializer : SdbSerializer<UUID> {
 *     override fun serialize(value: UUID): String = value.toString()
 *
 *     override fun deserialize(raw: String): UUID = UUID.fromString(raw)
 * }
 *
 * SdbSerializers.register(UUID::class.java, UUIDSerializer)
 * ```
 *
 * ## Notes
 * - Serialization must be reversible (lossless where possible).
 * - Used automatically by SdbRepository during reflection mapping.
 * - Falls back to `toString()` if no serializer is registered.
 *
 * @param T The type being serialized.
 */
interface SdbSerializer<T> {
    fun serialize(value: T): String
    fun deserialize(raw: String): T
}