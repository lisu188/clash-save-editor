package com.lis.clash

import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


annotation class ClashProperty(val index: Int, val length: Int, val converter: KClass<out Converter>)
annotation class ClashAggregateProperty(
    val index: Int,
    val count: Int,
    val size: Int,
    val clas: KClass<out ClashObject>
)


class DynamicTableModel<T : Any>(val _data: List<T>, val dataClass: KClass<T>) : AbstractTableModel() {

    override fun getColumnName(column: Int): String {
        return getProperty(column).name
    }

    override fun getColumnCount(): Int {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .size
    }

    override fun getRowCount(): Int {
        return _data.size
    }

    override fun getValueAt(row: Int, col: Int): Any? {
        val call = getProperty(col)
            .getter.call(_data[row])
        return call
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return getConverter(columnIndex) != NoneConverter::class.objectInstance;//TODO: validate
    }

    private fun getConverter(columnIndex: Int) = getAnnotation(columnIndex).converter.objectInstance!!

    override fun setValueAt(aValue: Any, row: Int, col: Int) {
        getProperty(col).setter.call(_data[row], getConverter(col).fromString(aValue as String))
    }

    private fun getProperty(col: Int): KMutableProperty<T> {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .sortedBy { it.findAnnotation<ClashProperty>()?.index }[col]
                as KMutableProperty<T>

    }

    //TODO: validate annotations
    private fun getAnnotation(col: Int): ClashProperty {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .sortedBy { it.findAnnotation<ClashProperty>()?.index }[col]
            .findAnnotation()!!

    }
}

inline fun <reified T : Any> buildTable(data: List<T>): DynamicTableModel<T> {
    return DynamicTableModel(data, T::class);
}