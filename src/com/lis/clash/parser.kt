package com.lis.clash

import com.lis.clash.objects.ClashObject
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


annotation class ClashSimpleProperty(val index: Int, val length: Int)

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

    fun setString(_object: ClashObject, value: String) {
        _property.setter.call(_object, getConverter().fromString(value))
    }

    fun setBytes(_object: ClashObject, value: List<Byte>) {
        _property.setter.call(_object, getConverter().fromBytes(value))
    }

    fun set(_object: ClashObject, value: Any) {
        _property.setter.call(_object, value)
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
        return converters[_property.getter.returnType.jvmErasure]!!
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

    fun count(): Int {
        return annotation.count
    }

    fun size(): Int {
        return annotation.size
    }

    fun getConstructor(): KFunction<ClashObject> {
        return annotation.clas.constructors.first { it.parameters.size == 2 }
    }

}

class ClassDescriptor(val properties: List<ClashPropertyDescriptor>) {

    fun getSimpleProperties(): List<SimplePropertyDescriptor> {
        return properties.filter { it.isSimple() }.map { it as SimplePropertyDescriptor }
    }

    fun getAggregateProperties(): List<AggregatePropertyDescriptor> {
        return properties.filter { it.isAggregate() }.map { it as AggregatePropertyDescriptor }
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

    fun getSimpleProperty(name: String): SimplePropertyDescriptor? {
        return getSimpleProperties().find { it.getName() == name }
    }


    fun getAggregateProperty(name: String): AggregatePropertyDescriptor? {
        return getAggregateProperties().find { it.getName() == name }
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