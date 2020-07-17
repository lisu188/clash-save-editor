package com.lis.clash

import com.lis.clash.objects.ClashObject
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


annotation class ClashSimpleProperty(val index: Int, val length: Int, val converter: KClass<out Converter>)

annotation class ClashAggregateProperty(
    val index: Int,
    val count: Int,
    val size: Int,
    val clas: KClass<out ClashObject>
)


abstract class ClashPropertyDescriptor(val _property: KMutableProperty<ClashObject>) {
    abstract fun index(): Int
    abstract fun length(): Int
    open fun isSimple(): Boolean {
        return false
    }

    open fun isAggregate(): Boolean {
        return false
    }

    fun get(_object: ClashObject): Any {
        return _property.getter.call(_object)
    }

    fun set(_object: ClashObject, value: String) {
        _property.setter.call(_object, getConverter().fromString(value))
    }

    abstract fun getConverter(): Converter

    fun getName(): String {
        return _property.name
    }
}

class SimplePropertyDescriptor(_property: KMutableProperty<ClashObject>) : ClashPropertyDescriptor(_property) {
    private val annotation: ClashSimpleProperty = _property.findAnnotation()!!
    override fun index(): Int {
        return annotation.index
    }

    override fun length(): Int {
        return annotation.length
    }

    override fun isSimple(): Boolean {
        return true
    }

    override fun getConverter(): Converter {
        return annotation.converter.objectInstance!!
    }
}

class AggregatePropertyDescriptor(_property: KMutableProperty<ClashObject>) : ClashPropertyDescriptor(_property) {
    private val annotation: ClashAggregateProperty = _property.findAnnotation()!!
    override fun index(): Int {
        return annotation.index
    }

    override fun length(): Int {
        return annotation.size * annotation.count
    }

    override fun isAggregate(): Boolean {
        return true
    }

    override fun getConverter(): Converter {
        TODO("Not yet implemented")
    }
}

class ClassDescriptor(val properties: List<ClashPropertyDescriptor>) {

    fun getSimpleProperties(): List<ClashPropertyDescriptor> {
        return properties.filter { it.isSimple() }
    }

    fun getPropertyWithIndex(index: Int): ClashPropertyDescriptor {
        return getOrderedSimpleProperties()[index]
    }

    fun getSimplePropertiesCount(): Int {
        return getSimpleProperties().size
    }

    private fun getOrderedSimpleProperties() = getSimpleProperties().sortedBy { it.index() }

    fun getSimpleProperty(col: Int): ClashPropertyDescriptor {
        return getOrderedSimpleProperties()[col]
    }
}

fun getClassDescriptor(dataClass: KClass<out ClashObject>) =
    AnnotationParser::class.objectInstance!!.descriptor(dataClass)


object AnnotationParser {
    val classes: MutableMap<KClass<out ClashObject>, ClassDescriptor> = mutableMapOf()

    fun descriptor(klas: KClass<out ClashObject>): ClassDescriptor {
        return classes.computeIfAbsent(klas) { parseClass(it) }
    }

    private fun parseClass(klas: KClass<out ClashObject>): ClassDescriptor {
        return ClassDescriptor(klas.memberProperties.filter { it is KMutableProperty<*> }
            .map { it as KMutableProperty<ClashObject> }
            .map { parseProperty(it) }
            .filter { it != null }
            .map { it as ClashPropertyDescriptor })
    }

    private fun parseProperty(property: KMutableProperty<ClashObject>): ClashPropertyDescriptor? {
        if (property.hasAnnotation<ClashSimpleProperty>()) {
            return SimplePropertyDescriptor(property)
        } else if (property.hasAnnotation<ClashAggregateProperty>()) {
            return AggregatePropertyDescriptor(property)
        }
        return null
    }
}