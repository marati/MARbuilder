package com.marati.marbuilder;

import java.io.*;

//utils
import java.util.prefs.Preferences;
import java.util.*;

import nu.xom.ParsingException;

//gui
import java.awt.*;
import java.awt.event.*;
import java.awt.event.WindowAdapter;
import java.awt.datatransfer.*;

import javax.swing.*;
//import javax.swing.table.*;

import org.apache.log4j.Logger;

//my
import gen.ParseException;
import gen.DocUtil;

/**
 *
 * @author marat
 */

public class MarForm extends JFrame
                        implements ActionListener {
    private final JButton getClientsTables;
    private final JButton openFileButton;
    private final JLabel captionLocationLabel;
    JLabel locationLabel;
    JTabbedPane structureTables;
    private final JFileChooser fileChooser;
    private final SummaryTable summaryTable;
    private JPanel mainPanel;
    private JScrollPane summaryTablePane;
    private final JMenuItem deleteColumnItem;
    private final JMenuItem addRowItem;
    private final FoldersWatcher foldersWatcher;
    private final DocUtil tablesGenner;
    private final static Logger logger = Logger.getLogger(MarForm.class);
    
    private static final String LOCATION = "location";
    private static final String MESSAGE_IDS = "message_ids";
    
    public MarForm(String title) throws ParseException, IOException, ParsingException {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSettings();
            }
        });
        
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        openFileButton = new JButton("Выбрать папку проекта...");
        openFileButton.addActionListener(this);
        
        getClientsTables = new JButton("Получить таблицы от клиентов");
        getClientsTables.addActionListener(this);
        
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        buttonsPanel.add(openFileButton);
        buttonsPanel.add(getClientsTables);
        
        captionLocationLabel = new JLabel("Рабочая директория: ");
        locationLabel = new JLabel("не задана");
        
        JLabel structureLabel = new JLabel("Доступные схемы");
        structureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        structureLabel.setOpaque(true);
        structureLabel.setBackground(Color.lightGray);
        structureLabel.setBorder(BorderFactory.createLineBorder(java.awt.Color.gray));
        structureLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 15));
        
        structureTables = new JTabbedPane(JTabbedPane.LEFT);
        structureTables.setPreferredSize(new Dimension(200, 200));
        
        JPanel structureTablesPanel = new JPanel();
        structureTablesPanel.setLayout(new BoxLayout(structureTablesPanel, BoxLayout.Y_AXIS));

        structureTablesPanel.add(structureLabel);
        structureTablesPanel.add(structureTables);
        
        deleteColumnItem = new JMenuItem("Удалить колонку");
        deleteColumnItem.addActionListener(this);
        
        addRowItem = new JMenuItem("Добавить строку");
        addRowItem.addActionListener(this);
        
        summaryTable = new SummaryTable();
        //summaryTable.setPre
        summaryTable.setAutoResizeMode(400);
        summaryTable.addContextMenu(deleteColumnItem);
        summaryTable.addContextMenu(addRowItem);

        summaryTablePane = new JScrollPane(summaryTable);
        //summaryTablePane.setPreferredSize(new Dimension(300, 300));
        
        mainPanel = new JPanel(new BorderLayout());

        JPanel labelsPanel = new JPanel(new FlowLayout());
        labelsPanel.add(captionLocationLabel);
        labelsPanel.add(locationLabel);
        
        mainPanel.add(BorderLayout.NORTH, labelsPanel);
        mainPanel.add(BorderLayout.SOUTH, buttonsPanel);
        mainPanel.add(BorderLayout.WEST, structureTablesPanel);
        mainPanel.add(BorderLayout.EAST, summaryTablePane);
        
        add(mainPanel, BorderLayout.CENTER);
        
        structureTables.setSize(100, 500);
        structureTables.setVisible(false);
        
        readSettings();
        
        String locationPath = locationLabel.getText();
        
        tablesGenner = new DocUtil(this);
        
        foldersWatcher = new FoldersWatcher(tablesGenner);
        foldersWatcher.checkWorkingDir(locationPath);
        
        //UIManager.setLookAndFeel(new syntheticaBlueMoon());
        UIManager.getDefaults (). put ("TabbedPane.lightHighlight" ,  Color.BLACK );
        UIManager.getDefaults (). put ("TabbedPane.selectHighlight",  Color.BLACK );
        UIManager.getDefaults().put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
    }
    
    private void readSettings() {
        //try {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            
            String location = prefs.get(LOCATION, "");
            locationLabel.setText(location);
            
            /*if (prefs.nodeExists(MESSAGE_IDS)) {
                String messageIds = prefs.get(MESSAGE_IDS, "");
                foldersWatcher.setReceivedMessageIds(messageIds);
            }*/
//        } catch (BackingStoreException ex) {
//            Logger.getLogger(MarForm.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    private void saveSettings() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        prefs.put(LOCATION, locationLabel.getText());
        //prefs.put(MESSAGE_IDS, foldersWatcher.getReceivedMessageIdsByString());
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openFileButton) {
            int returnValue = fileChooser.showOpenDialog(MarForm.this);
            
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String locationPath = selectedFile.getPath();
                
                locationLabel.setText(locationPath);
                try {
                    foldersWatcher.checkWorkingDir(locationPath);
                } catch (ParseException ex) {
                    logger.error(ex);
                } catch (IOException ex) {
                    logger.error(ex);
                } catch (ParsingException ex) {
                    logger.error(ex);
                }
            }
            
        } else if (e.getSource() == getClientsTables) {
            String nameReportDialog = JOptionPane.showInputDialog(this, "Введите название сводного отчёта:");

            MARDefaultTableModel marModel = (MARDefaultTableModel)summaryTable.getModel();
            //System.out.println(marModel.getColumnIdentifiers().toString());
            
            foldersWatcher.buildReport(nameReportDialog, marModel.getChoosedColumns());
        } else if (e.getSource() == deleteColumnItem) {            
            int selectionColumn = summaryTable.getSelectedColumn();
            //System.out.println("delete menu click" + selectionColumn);
            summaryTable.removeColumnAndData(selectionColumn);
        } else if (e.getSource() == addRowItem) {
            MARDefaultTableModel model = (MARDefaultTableModel)summaryTable.getModel();
            model.addRow(new Object[]{"", ""});
        }
    }
    
    public void addTabsInPane(HashMap<String, ArrayList<String>> tables) {
        if (!structureTables.isVisible())
            structureTables.setVisible(true);
        
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
            columnsList.setName(tableName);
            columnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            
            JScrollPane tabbedPane = new JScrollPane(columnsList);
            
            columnsList.setDragEnabled(true);
            columnsList.setTransferHandler(new ListTransferHandler());
            
            structureTables.add(tabbedPane, tableName);
            
            logger.info(String.format("add new tab (%s) and columns (%s)", tableName, columns.toString()));
            //не работает
            /*structureTables.repaint();
            structureTables.validate();
            
            
            summaryTablePane.repaint();
            summaryTablePane.revalidate();
            
            summaryTable.repaint();
            summaryTable.revalidate();
            
            mainPanel.repaint();
            mainPanel.revalidate();*/
        }
    }
    
    public void addRowsInSummaryTable(String column, ArrayList<String> values) {
        MARDefaultTableModel marModel =  (MARDefaultTableModel)summaryTable.getModel();
        
        int rowIndex = 0;
        int columnIndex = marModel.getIndexByColumnName(column);
        
        Iterator<String> valuesIt = values.iterator();
        while (valuesIt.hasNext()) {
            marModel.setValueAt(valuesIt.next(), rowIndex, columnIndex);
            ++rowIndex;

            int rowCount = marModel.getRowCount();
            if (rowIndex >= rowCount && valuesIt.hasNext())
                marModel.addRow(new Object[]{"", ""});
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
        
        return list.getName() + "|" + buff.toString();
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