package com.lis.clash;

import javax.swing.*;

public class ClashGUI {
    private JPanel mainPanel;
    private JButton loadButton;
    private JTable armyTable;
    private JTabbedPane tabbedPane1;
    private JTable unitTable;
    private MapPanel mapPanel;
    private JTable tilesTable;
    private JTable playersTable;
    private JTable castlesTable;
    private JTable castleUnitTable;
    private JButton saveButton;

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JTable getArmyTable() {
        return armyTable;
    }

    public JTable getUnitTable() {
        return unitTable;
    }

    public MapPanel getMapPanel() {
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
}
