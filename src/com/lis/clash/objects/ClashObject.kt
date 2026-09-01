package com.lis.clash.objects

import com.lis.clash.getClassDescriptor
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

open class ClashObject(val parent: ClashObject?, val index: Int) {

    open fun isValid(): Boolean {
        return true;
    }

    internal var bytes: List<Byte> by Delegates.observable(
        listOf(),
        { _, oldValue, newValue ->
            if (oldValue != newValue) {
                onBytesChanged()
            }
        })


    fun <T> clashProperty(
        initialValue: T
    ): ReadWriteProperty<Any?, T> {
        return Delegates.observable(initialValue, { property, oldValue, newValue ->
            if (oldValue != newValue) {
                getClassDescriptor(this::class).getSimpleProperty(property.name)?.let {
                    val currentBytes = bytes.slice(it.index() until (it.index() + it.length()))
                    val toBytes = it.toBytes(newValue!!, currentBytes)
                    val boundedBytes = if (toBytes.size > it.length()) {
                        toBytes.take(it.length())
                    } else {
                        val preservedTail = bytes.slice((it.index() + toBytes.size) until (it.index() + it.length()))
                        toBytes + preservedTail
                    }
                    refreshBytes(it.index(), boundedBytes)
                }

                getClassDescriptor(this::class).getAggregateProperty(property.name)?.let {
                    for (ob in (newValue as? List<*>)?.filterIsInstance<ClashObject>().orEmpty()) {
                        refreshBytes(ob.index, ob.bytes)
                    }
                }
            }
        })
    }

    private fun onBytesChanged() {
        getClassDescriptor(this::class).getSimpleProperties().forEach {
            val startIndex = it.index()
            val endIndex = it.index() + it.length()
            val propertyBytes = bytes.slice(startIndex until endIndex)
            it.setBytes(this, propertyBytes)
        }

        getClassDescriptor(this::class).getAggregateProperties().forEach {
            val list = mutableListOf<ClashObject>()
            for (i in 0 until it.count()) {
                val startIndex = it.index() + i * it.size()
                val endIndex = it.index() + (i + 1) * it.size()
                val element = it.getConstructor().call(this, startIndex)
                    .withBytes<ClashObject>(
                        bytes.slice(startIndex until endIndex)
                    )
                if (!element.isValid()) {
                    if (it.stopAtFirstInvalid()) {
                        break
                    }
                    continue
                }
                list.add(element)
            }
            it.set(this, list)
        }

        parent?.refreshBytes(index, bytes)
    }

    private fun refreshBytes(index: Int, _bytes: List<Byte>) {
        if (bytes.slice(index until index + _bytes.size) != _bytes) {
            val start = bytes.subList(0, index)
            val mid = _bytes
            val end = bytes.subList(index + _bytes.size, bytes.size)
            bytes = start + mid + end
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> withBytes(slice: List<Byte>): T {
        bytes = slice
        return this as T
    }

    fun changeByte(index: Int, byte: Byte) {
        refreshBytes(index, listOf(byte));
    }
}
