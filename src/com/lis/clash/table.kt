package com.lis.clash

import com.lis.clash.objects.ClashObject
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass


class DynamicTableModel(val _data: () -> List<ClashObject>, val dataClass: KClass<out ClashObject>) :
    AbstractTableModel() {

    override fun getColumnName(column: Int): String {
        return getClassDescriptor(dataClass).getSimpleProperty(column).getName()
    }

    override fun getColumnCount(): Int {
        return getClassDescriptor(dataClass).getSimplePropertiesCount()
    }


    override fun getRowCount(): Int {
        return _data().size
    }

    override fun getValueAt(row: Int, col: Int): Any? {
        return getClassDescriptor(dataClass).getSimpleProperty(col).get(_data()[row])
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return true
    }

    override fun setValueAt(aValue: Any, row: Int, col: Int) {
        return getClassDescriptor(dataClass).getSimpleProperty(col).set(_data()[row], aValue as String)
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