package com.lis.clash.objects

import com.lis.clash.AggregatePropertyDescriptor
import com.lis.clash.StringConverter
import com.lis.clash.getClassDescriptor
import java.nio.charset.Charset
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** A view into a single owning byte array. Decoding never writes to that array. */
open class ClashObject(val parent: ClashObject?, val index: Int) {
    private var storage = ByteArray(0)
    private var viewSize = 0
    private val aggregateViews = mutableMapOf<String, List<ClashObject>>()
    private val root: ClashObject get() = parent?.root ?: this
    val absoluteOffset: Int get() = (parent?.absoluteOffset ?: 0) + index
    var textEncoding: Charset
        get() = root.encoding
        set(value) { root.encoding = value }
    private var encoding: Charset = StringConverter.DEFAULT_CHARSET

    open fun isValid(): Boolean = true

    /** A defensive snapshot, never a mutable reference to document storage. */
    var bytes: List<Byte>
        get() = toByteArray().toList()
        internal set(value) {
            if (parent == null) {
                storage = value.toByteArray()
                viewSize = storage.size
            } else {
                viewSize = value.size
                patch(0, value.toByteArray())
            }
        }

    fun toByteArray(): ByteArray = root.storage.copyOfRange(absoluteOffset, absoluteOffset + viewSize)

    private fun read(offset: Int, size: Int): List<Byte> {
        require(offset >= 0 && size >= 0 && offset + size <= viewSize) { "Field exceeds record bounds" }
        return root.storage.copyOfRange(absoluteOffset + offset, absoluteOffset + offset + size).toList()
    }

    /** Explicit bounded patch. Callers needing multi-field consistency use SaveDocument commands. */
    fun patch(offset: Int, replacement: ByteArray) {
        require(offset >= 0 && offset.toLong() + replacement.size <= viewSize) { "Patch exceeds record bounds" }
        replacement.copyInto(root.storage, absoluteOffset + offset)
    }

    fun <T> clashProperty(initialValue: T): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (viewSize == 0) return initialValue
            val descriptor = getClassDescriptor(this@ClashObject::class)
            descriptor.getSimpleProperty(property.name)?.let {
                val raw = read(it.index(), it.length())
                return (if (it.getConverter() === StringConverter) StringConverter.decode(raw, textEncoding)
                    else it.fromBytes(raw)) as T
            }
            descriptor.getAggregateProperty(property.name)?.let {
                val slots = physicalSlots(it)
                return (if (it.stopAtFirstInvalid()) slots.takeWhile(ClashObject::isValid)
                    else slots.filter(ClashObject::isValid)) as T
            }
            return initialValue
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val descriptor = getClassDescriptor(this@ClashObject::class)
            val field = descriptor.getSimpleProperty(property.name)
                ?: throw IllegalArgumentException("Aggregate replacement requires a transactional structural command")
            require(!field.isReadOnly()) { "${property.name} is read-only runtime metadata" }
            val current = read(field.index(), field.length())
            val encoded = if (field.getConverter() === StringConverter)
                StringConverter.encode(value as String, field.length(), textEncoding, field.requiresTerminator())
            else field.toBytes(requireNotNull(value), current)
            require(encoded.size == field.length()) { "${property.name} requires exactly ${field.length()} bytes" }
            patch(field.index(), encoded.toByteArray())
        }
    }

    /** All physical slots, independent of validity and packed-list sentinels. */
    fun physicalSlots(name: String): List<ClashObject> = physicalSlots(
        requireNotNull(getClassDescriptor(this::class).getAggregateProperty(name)) { "Unknown aggregate $name" }
    )

    private fun physicalSlots(descriptor: AggregatePropertyDescriptor): List<ClashObject> =
        aggregateViews.getOrPut(descriptor.getName()) {
            // The list and its records are lazy; map reads don't instantiate 30,000 objects.
            object : AbstractList<ClashObject>() {
                override val size = descriptor.count()
                private val records = arrayOfNulls<ClashObject>(size)
                override fun get(index: Int): ClashObject {
                    require(index in indices) { "Physical slot out of range: $index" }
                    return records[index] ?: descriptor.getConstructor()
                        .call(this@ClashObject, descriptor.index() + index * descriptor.size())
                        .also { it.viewSize = descriptor.size(); records[index] = it }
                }
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun <T> withBytes(slice: List<Byte>): T {
        bytes = slice
        return this as T
    }

    fun changeByte(index: Int, byte: Byte) = patch(index, byteArrayOf(byte))
}
