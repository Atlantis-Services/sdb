package net.atlantisservices.sdb

/**
 * Global registry for custom [SdbSerializer] implementations.
 *
 * This allows SimpleDatabase to support user-defined types beyond primitives,
 * collections, and maps.
 *
 * ## Example
 * ```kotlin
 * SdbSerializers.register(UUID::class.java, UUIDSerializer)
 * ```
 *
 * ## Internal behavior
 * - Stores serializers by raw Java class
 * - Used during both serialization and deserialization
 * - Unsafe casts are intentional for performance and flexibility
 */
object SdbSerializers {

    private val serializers = mutableMapOf<Class<*>, SdbSerializer<*>>()

    /**
     * Registers a serializer for a specific type.
     *
     * @param type The Java class of the type
     * @param serializer The serializer implementation
     */
    fun <T : Any> register(type: Class<T>, serializer: SdbSerializer<T>) {
        serializers[type] = serializer
    }

    /**
     * Retrieves a serializer for the given type, if registered.
     *
     * @param type The Java class to look up
     * @return A matching serializer or null if none exists
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: Class<T>): SdbSerializer<T>? {
        return serializers[type] as? SdbSerializer<T>
    }
}