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

annotation class ClashSignedProperty(val index: Int, val length: Int)

annotation class ClashMaskedProperty(
    val index: Int,
    val length: Int,
    val mask: Int,
    val shift: Int = 0
)

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
        _property.setter.call(_object, fromString(value))
    }

    fun setBytes(_object: ClashObject, value: List<Byte>) {
        _property.setter.call(_object, fromBytes(value))
    }

    fun set(_object: ClashObject, value: Any) {
        _property.setter.call(_object, value)
    }

    abstract fun getConverter(): Converter

    fun getName(): String {
        return _property.name
    }

    open fun fromString(value: String): Any {
        return getConverter().fromString(value)
    }

    open fun fromBytes(value: List<Byte>): Any {
        return getConverter().fromBytes(value, length())
    }

    open fun toBytes(value: Any, currentBytes: List<Byte>): List<Byte> {
        return getConverter().toBytes(value, length())
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

class SignedPropertyDescriptor(_property: KMutableProperty<ClashObject>) : ClashPropertyDescriptor(_property) {
    private val annotation: ClashSignedProperty = _property.findAnnotation()!!

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
        return SignedIntConverter
    }
}

class MaskedPropertyDescriptor(_property: KMutableProperty<ClashObject>) : ClashPropertyDescriptor(_property) {
    private val annotation: ClashMaskedProperty = _property.findAnnotation()!!

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
        return IntConverter
    }

    override fun fromString(value: String): Any {
        return value.toInt()
    }

    fun mask(): Int {
        return annotation.mask
    }

    fun shift(): Int {
        return annotation.shift
    }

    override fun fromBytes(value: List<Byte>): Any {
        val rawValue = readLittleEndianInt(value)
        return (rawValue ushr annotation.shift) and annotation.mask
    }

    override fun toBytes(value: Any, currentBytes: List<Byte>): List<Byte> {
        val currentValue = readLittleEndianInt(currentBytes)
        val shiftedMask = annotation.mask shl annotation.shift
        val maskedValue = (((value as Int) and annotation.mask) shl annotation.shift)
        val updatedValue = (currentValue and shiftedMask.inv()) or maskedValue
        return writeLittleEndianInt(updatedValue, length())
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

    fun getSimpleProperties(): List<ClashPropertyDescriptor> {
        return properties.filter { it.isSimple() }
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

    fun getSimpleProperty(name: String): ClashPropertyDescriptor? {
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

    @Suppress("UNCHECKED_CAST")
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
        } else if (property.hasAnnotation<ClashSignedProperty>()) {
            return SignedPropertyDescriptor(property)
        } else if (property.hasAnnotation<ClashMaskedProperty>()) {
            return MaskedPropertyDescriptor(property)
        } else if (property.hasAnnotation<ClashAggregateProperty>()) {
            return AggregatePropertyDescriptor(property)
        }
        return null
    }
}
