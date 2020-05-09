package com.lis.clash;

import javax.swing.*;

public class ClashGUI {
    private JPanel mainPanel;
    private JButton loadButton;
    private JTable armyTable;
    private JTabbedPane tabbedPane1;
    private JTable unitTable;
    private MapPanel mapPanel;

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
}
