package com.lis.clash;

import javax.swing.*;

public class ClashGUI {
    private JPanel mainPanel;
    private JButton loadButton;
    private JTable armyUnitsTable;
    private JTabbedPane tabbedPane1;
    private JTable unitTable;
    private JPanel mapPanel;
    private JTable tilesTable;
    private JTable playersTable;
    private JTable castlesTable;
    private JTable castleUnitTable;
    private JButton saveButton;
    private JTable bytesTable;
    private JTextField xTile;
    private JTextField yTile;
    private JPanel scriptsTab;
    private JButton executeButton;
    private JComboBox scriptBox;

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JTable getArmyUnitsTable() {
        return armyUnitsTable;
    }

    public JTable getUnitTable() {
        return unitTable;
    }

    public JPanel getMapPanel() {
        return mapPanel;
    }

    public JTable getTilesTable() {
        return tilesTable;
    }

    public JTable getPlayersTable() {
        return playersTable;
    }

    public JTable getCastlesTable() {
        return castlesTable;
    }

    public JTable getCastleUnitTable() {
        return castleUnitTable;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JTable getBytesTable() {
        return bytesTable;
    }

    public JTextField getxTile() {
        return xTile;
    }

    public JTextField getyTile() {
        return yTile;
    }

    public JButton getExecuteButton() {
        return executeButton;
    }

    public JComboBox getScriptBox() {
        return scriptBox;
    }
}
