package com.lis.clash;

import javax.swing.*;

public class ClashGUI {
    private JPanel mainPanel;
    private JButton loadButton;
    private ClashTable armyUnitsTable;
    private JTabbedPane tabbedPane1;
    private ClashTable unitTable;
    private MapPanel mapPanel;
    private ClashTable tilesTable;
    private ClashTable playersTable;
    private ClashTable castlesTable;
    private ClashTable castleUnitTable;
    private JButton saveButton;
    private BytesTable bytesTable;

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public ClashTable getArmyUnitsTable() {
        return armyUnitsTable;
    }

    public ClashTable getUnitTable() {
        return unitTable;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public ClashTable getTilesTable() {
        return tilesTable;
    }

    public ClashTable getPlayersTable() {
        return playersTable;
    }

    public ClashTable getCastlesTable() {
        return castlesTable;
    }

    public ClashTable getCastleUnitTable() {
        return castleUnitTable;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public BytesTable getBytesTable() {
        return bytesTable;
    }
}
