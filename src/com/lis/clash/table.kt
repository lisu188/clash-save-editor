package com.lis.clash

import com.lis.clash.objects.ClashObject
import javax.swing.JTable
import javax.swing.ListSelectionModel
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


class DynamicTableModel(val _data: () -> List<ClashObject>, val dataClass: KClass<out ClashObject>) :
    AbstractTableModel() {

    override fun getColumnName(column: Int): String {
        return getProperty(column).name
    }

    override fun getColumnCount(): Int {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .size
    }

    override fun getRowCount(): Int {
        return _data().size
    }

    override fun getValueAt(row: Int, col: Int): Any? {
        return getProperty(col)
            .getter.call(_data()[row])
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return getConverter(columnIndex) != NoneConverter::class.objectInstance;//TODO: validate
    }

    private fun getConverter(columnIndex: Int) = getAnnotation(columnIndex).converter.objectInstance!!

    override fun setValueAt(aValue: Any, row: Int, col: Int) {
        getProperty(col).setter.call(_data()[row], getConverter(col).fromString(aValue as String))
    }

    private fun getProperty(col: Int): KMutableProperty<ClashObject> {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .sortedBy { it.findAnnotation<ClashProperty>()?.index }[col]
                as KMutableProperty<ClashObject>

    }

    //TODO: validate annotations
    private fun getAnnotation(col: Int): ClashProperty {
        return dataClass.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .sortedBy { it.findAnnotation<ClashProperty>()?.index }[col]
            .findAnnotation()!!

    }
}

class SelectionController {
    var currentSelectedTable: ClashTable? = null
    var currentSelectedIndex: Int = -1;
    var bytesTable: BytesTable? = null

    fun onSelection(table: ClashTable, selectedIndex: Int) {
        if (table != currentSelectedTable
            && table != currentSelectedTable?.subTable
            && table != currentSelectedTable?.masterTable
        ) {
            currentSelectedTable?.clearSelection();
            currentSelectedTable?.masterTable?.clearSelection();
        }
        currentSelectedTable = table;
        currentSelectedIndex = selectedIndex;

        bytesTable?.model = BytesTableModel(this)
    }

    fun withBytesTable(bytesTable: BytesTable): SelectionController {
        this.bytesTable = bytesTable
        return this
    }
}

class ClashTable : JTable() {
    var subTable: ClashTable? = null;
    var masterTable: ClashTable? = null;
    var _data: () -> List<ClashObject> = { emptyList() };

    init {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    }

    //TODO: extract to builder
    inline fun <reified T : ClashObject> withData(noinline function: () -> List<T>): ClashTable {
        _data = function;
        model = DynamicTableModel(function, T::class)
        return this
    }

    //TODO: extract to builder
    inline fun <reified T : ClashObject, reified U : ClashObject> withSubTable(
        slaveTable: ClashTable,
        noinline slaveDataExtractor: (U) -> List<T>
    ): ClashTable {
        slaveTable.masterTable = this
        subTable = slaveTable;
        selectionModel.addListSelectionListener {
            subTable?.withData {
                if (selectedRow != -1) slaveDataExtractor(_data()[selectedRow] as U) else emptyList()
            }
        }
        return this
    }

    //TODO: extract to builder
    fun withSelectionController(selectionController: SelectionController): ClashTable {
        selectionModel.addListSelectionListener {
            selectionController.onSelection(this, selectedRow)
        }
        return this
    }

}

class BytesTableModel(val selectionController: SelectionController) : AbstractTableModel() {

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return true
    }

    override fun getRowCount(): Int {
        return if (getCurrentSelected() == -1) 0 else 1;
    }

    override fun getColumnCount(): Int {
        if (getCurrentSelected() != -1) {
            return getCurrentObject()?.bytes?.size ?: 0
        }
        return 0
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Byte? {
        return getCurrentObject()?.bytes?.get(columnIndex)
    }

    private fun getCurrentSelected() = selectionController.currentSelectedIndex

    private fun getCurrentData() = selectionController.currentSelectedTable?._data

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        getCurrentObject()?.changeByte(columnIndex, (aValue as String).toByte())
    }

    private fun getCurrentObject() =
        getCurrentData()?.let { it() }?.getOrElse(getCurrentSelected()) { null }

}

class BytesTable : JTable() {

}