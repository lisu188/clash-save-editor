package com.lis.clash

import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


annotation class Column(val no: Int)

class DynamicTableModel<T : Any>(val _data: List<T>, val dataClass: KClass<T>) : AbstractTableModel() {

    override fun getColumnName(column: Int): String {
        return getProperty(column).name
    }

    override fun getColumnCount(): Int {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<Column>() }
            .size
    }

    override fun getRowCount(): Int {
        return _data.size
    }

    override fun getValueAt(row: Int, col: Int): Any? {
        return getProperty(col)
            .get(_data[row])
    }

    private fun getProperty(col: Int): KProperty1<T, *> {
        return dataClass.memberProperties
            .filter { it.findAnnotation<Column>()?.no == col }
            .first()
    }
}

inline fun <reified T : Any> buildTable(data: List<T>): DynamicTableModel<T> {
    return DynamicTableModel(data, T::class);
}