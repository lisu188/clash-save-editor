package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSimpleProperty
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

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
            if (oldValue != newValue && property.hasAnnotation<ClashSimpleProperty>()) {
                val annotation = getAnnotation<ClashSimpleProperty>(property)
                val toBytes = getConverter(annotation).toBytes(newValue!!)
                val augumentedBytes =
                    if (toBytes.size < annotation.length) toBytes + List(annotation.length - toBytes.size) { -1 } else toBytes
                refreshBytes(annotation.index, augumentedBytes)
            }
            if (oldValue != newValue && property.hasAnnotation<ClashAggregateProperty>()) {
                for (ob in newValue as List<ClashObject>) {
                    refreshBytes(ob.index, ob.bytes)
                }
            }
        })
    }

    private fun onBytesChanged() {
        this::class.memberProperties
            .filter { it.hasAnnotation<ClashSimpleProperty>() }
            .map { it as KMutableProperty<*> }
            .forEach {
                val annotation = getAnnotation<ClashSimpleProperty>(it)
                val startIndex = annotation.index
                val endIndex = annotation.index + annotation.length
                val propertyBytes = bytes.slice(startIndex until endIndex)
                val propertyValue = getConverter(annotation).fromBytes(propertyBytes)
                it.setter.call(this, propertyValue)
            }

        this::class.memberProperties
            .filter { it.hasAnnotation<ClashAggregateProperty>() }
            .map { it as KMutableProperty<*> }
            .forEach {
                val annotation = getAnnotation<ClashAggregateProperty>(it)
                val list = mutableListOf<ClashObject>()
                for (i in 0 until annotation.count) {
                    val startIndex = annotation.index + i * annotation.size
                    val endIndex = annotation.index + (i + 1) * annotation.size
                    val element = getConstructor(annotation).call(this, startIndex)
                        .withBytes<ClashObject>(
                            bytes.slice(startIndex until endIndex)
                        )
                    if (!element.isValid()) {
                        break;
                    }
                    list.add(element)
                }
                it.setter.call(this, list)
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

    private fun getConstructor(annotation: ClashAggregateProperty): KFunction<ClashObject> {
        return annotation.clas.constructors.first { it.parameters.size == 2 }
    }

    private inline fun <reified T : Annotation> getAnnotation(it: KProperty<*>) = it.findAnnotation<T>()!!

    private fun getConverter(annotation: ClashSimpleProperty?) = annotation?.converter?.objectInstance!!

    fun <T> withBytes(slice: List<Byte>): T {
        bytes = slice
        return this as T
    }

    fun changeByte(index: Int, byte: Byte) {
        refreshBytes(index, listOf(byte));
    }
}