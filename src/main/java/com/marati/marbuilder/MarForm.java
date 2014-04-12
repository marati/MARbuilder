package com.marati.marbuilder;

import java.io.*;
import java.util.prefs.Preferences;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.WindowAdapter;
import javax.swing.*;
import javax.swing.JFileChooser.*;
//mport javax.swing.table.TableColumn;
import javax.swing.table.*
import javax.swing.filechooser.FileFilter;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableModel;

import com.marati.marbuilder.FoldersWatcher;
import gen.ParseException;
import gen.JTableGen;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import static javax.swing.TransferHandler.MOVE;


/**
 *
 * @author marat
 */

public class MarForm extends JFrame
                        implements ActionListener {
    JButton openFileButton;
    JLabel captionLocationLabel;
    JLabel locationLabel;
    JTabbedPane structureTables;
    JFileChooser fileChooser;
    FoldersWatcher foldersWatcher;
    JTableGen tablesGenner;
    
    private static final String LOCATION = "location";
    
    public MarForm(String title) throws ParseException, IOException {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //rename t myframeClass
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSettings();
            }
        });
        
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        openFileButton = new JButton("Выберите папку для Вашего проекта...");
        openFileButton.addActionListener(this);
        
        captionLocationLabel = new JLabel("Рабочая директория: ");
        locationLabel = new JLabel("не задана");
        
        structureTables = new JTabbedPane(JTabbedPane.LEFT);
        
        DefaultTableModel model = new DefaultTableModel();
        
        model.addColumn("Column 0");
        model.addColumn("Column 1");
        model.addColumn("Column 2");
        model.addColumn("Column 3");
        
        model.addRow(new String[]{"Table 00", "Table 01",
                                  "Table 02", "Table 03"});
        model.addRow(new String[]{"Table 10", "Table 11",
                                  "Table 12", "Table 13"});
        model.addRow(new String[]{"Table 20", "Table 21",
                                  "Table 22", "Table 23"});
        model.addRow(new String[]{"Table 30", "Table 31",
                                  "Table 32", "Table 33"});
        
        JTable summaryTable = new JTable(model);
        summaryTable.setName("Сводная таблица");
        
        //JTableHeader header = new JTableHeader();
        //header.set
        //summaryTable.setTableHeader(null);
        
        JScrollPane summaryTablePane = new JScrollPane(summaryTable);
        
        summaryTable.setDragEnabled(true);
        summaryTable.setTransferHandler(new TableTransferHandler());
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        //mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Box labelsBox = new Box(BoxLayout.X_AXIS);
        labelsBox.add(captionLocationLabel);
        labelsBox.add(locationLabel);
        
        mainPanel.add(BorderLayout.SOUTH, openFileButton);
        mainPanel.add(BorderLayout.NORTH, labelsBox);
        mainPanel.add(BorderLayout.WEST, structureTables);
        mainPanel.add(BorderLayout.EAST, summaryTablePane);
        
        add(mainPanel, BorderLayout.CENTER);
        
        readSettings();
        
        tablesGenner = new JTableGen(this);
        
        foldersWatcher = new FoldersWatcher(locationLabel.getText(), tablesGenner);
        foldersWatcher.checkWorkingDir();
    }
    
    private void readSettings() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String location = prefs.get(LOCATION, "");
        locationLabel.setText(location);
    }
    
    private void saveSettings() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        prefs.put(LOCATION, locationLabel.getText());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openFileButton) {
            int returnValue = fileChooser.showOpenDialog(MarForm.this);
            
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                locationLabel.setText(selectedFile.getPath());
            }
        }
    }
    
    public void addTabsInPane(HashMap<String, ArrayList<String>> tables) {
        
        for (Map.Entry<String, ArrayList<String>> tableInfo: tables.entrySet()) {
            //get Table Name
            String tableName = tableInfo.getKey();
            
            //get Columns
            ArrayList<String> columns = tableInfo.getValue();
            DefaultListModel listModel = new DefaultListModel();
            
            for(int i=0; i<columns.size(); i++) {
                listModel.addElement(columns.get(i));
                
                /*TableColumn column = new TableColumn(i);
                column.setHeaderValue(columns.get(i).toString());
                table.addColumn(column);*/
            }

            JList columnsList = new JList(listModel);
            columnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane tabbedPane = new JScrollPane(columnsList);
            
            columnsList.setDragEnabled(true);
            columnsList.setTransferHandler(new ListTransferHandler());
            
            structureTables.add(tabbedPane, tableName);
        }
    }

}

class ListTransferHandler extends TransferHandler {
    private int[] indices = null;
            
    protected String exportString(JComponent c) {
        JList list = (JList)c;
        indices = list.getSelectedIndices();
        Object[] values = list.getSelectedValues();
        
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < values.length; i++) {
            Object val = values[i];
            buff.append(val == null ? "" : val.toString());
            if (i != values.length - 1) {
                buff.append("\n");
            }
        }
        
        return buff.toString();
    }
    
    protected Transferable createTransferable(JComponent c) {
        return new StringSelection(exportString(c));
    }
    
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
    
    protected void cleanup(JComponent c, boolean remove) {
        if (remove && indices != null) {
            JList source = (JList)c;
            DefaultListModel model  = (DefaultListModel)source.getModel();

            for (int i = indices.length - 1; i >= 0; i--) {
                model.remove(indices[i]);
            }
        }
        indices = null;
    }
    
    protected void exportDone(JComponent c, Transferable data, int action) {
        cleanup(c, action == MOVE);
    }
}

class TableTransferHandler extends TransferHandler {
    
    protected void importString(JComponent c, String str) {
        JTable target = (JTable)c;
        DefaultTableModel model = (DefaultTableModel)target.getModel();
        int index = target.getSelectedRow();

        int colCount = target.getColumnCount();
        String[] values = str.split("\n");
        
        for (int i = 0; i < values.length && i < colCount; i++) {
            model.addColumn(values[i]);
        }
    }
    
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
    
    public boolean importData(JComponent c, Transferable t) {
        if (canImport(c, t.getTransferDataFlavors())) {
            try {
                String str = (String)t.getTransferData(DataFlavor.stringFlavor);
                importString(c, str);
                return true;
            } catch (UnsupportedFlavorException ufe) {
            } catch (IOException ioe) {
            }
        }

        return false;
    }
    
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.stringFlavor.equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }
    
}
