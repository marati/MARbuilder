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
import javax.swing.table.*;
//import static javax.swing.TransferHandler.COPY_OR_MOVE;
//import static javax.swing.TransferHandler.MOVE;

//my
import com.marati.marbuilder.FoldersWatcher;
import com.marati.marbuilder.MARmq;
import gen.ParseException;
import gen.JTableGen;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author marat
 */

class MARDefaultTableModel extends DefaultTableModel {
    public Vector getColumnIdentifiers() {
        return columnIdentifiers;
    }
}

class SummaryTable extends JTable {
    public SummaryTable() {
        DefaultTableModel model = new MARDefaultTableModel();
        model.addColumn("Перенесите сюда колонки из левой части");
        model.addRow(new String[]{"Этот текст пропадёт, если вы что-то перенесёте сюда"});
        setModel(model);
        
        getTableHeader().setDefaultRenderer(new HeaderRenderer());
        
        setDragEnabled(true);
        setTransferHandler(new TableTransferHandler());
    }
    
    //one parameter == one item in menu
    public void addContextMenu(JMenuItem menuItem) {
        final JPopupMenu popup = new JPopupMenu();
        popup.add(menuItem);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {

            }
            
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    Point currentPoint = me.getPoint();
                    
                    int colNumber = columnAtPoint(currentPoint);
                    int rowNumber = rowAtPoint(currentPoint);
                    
                    setColumnSelectionInterval(colNumber, colNumber);
                    setRowSelectionInterval(rowNumber, rowNumber);
                }
                
                maybeShowPopup(me);
            }
            
            @Override
            public void mouseReleased(MouseEvent me) {
                maybeShowPopup(me);
            }
            
            private void maybeShowPopup(MouseEvent me) {
                if (me.isPopupTrigger()) {
                    popup.show(me.getComponent(),
                               me.getX(), me.getY());
                }
            }
            
        });
    }
    
    public void removeColumnAndData(int colIndex) {
        MARDefaultTableModel model = (MARDefaultTableModel)getModel();
        TableColumn col = getColumnModel().getColumn(colIndex);
        int columnModelIndex = col.getModelIndex();
        Vector data = model.getDataVector();
        Vector colIds = model.getColumnIdentifiers();

        // Remove the column from the table
        removeColumn(col);

        // Remove the column header from the table model
        colIds.removeElementAt(columnModelIndex);

        // Remove the column data
        for (int r=0; r<data.size(); r++) {
            Vector row = (Vector)data.get(r);
            row.removeElementAt(columnModelIndex);
        }
        model.setDataVector(data, colIds);

        Enumeration enums = getColumnModel().getColumns();
        for (; enums.hasMoreElements(); ) {
            TableColumn c = (TableColumn)enums.nextElement();
            if (c.getModelIndex() >= columnModelIndex) {
                c.setModelIndex(c.getModelIndex()-1);
            }
        }
        model.fireTableStructureChanged();
    }
    
}

public class MarForm extends JFrame
                        implements ActionListener {
    private final JButton getClientsTables;
    private final JButton openFileButton;
    private final JLabel captionLocationLabel;
    JLabel locationLabel;
    JTabbedPane structureTables;
    private final JFileChooser fileChooser;
    private final SummaryTable summaryTable;
    private final JMenuItem deleteColumnItem;
    private final FoldersWatcher foldersWatcher;
    private final JTableGen tablesGenner;
    private final MARmq messageQueue;
    
    private static final String LOCATION = "location";
    
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
        
        structureTables = new JTabbedPane(JTabbedPane.LEFT);
        
        deleteColumnItem = new JMenuItem("Удалить колонку");
        deleteColumnItem.addActionListener(this);
        
        summaryTable = new SummaryTable();
        summaryTable.addContextMenu(deleteColumnItem);

        JScrollPane summaryTablePane = new JScrollPane(summaryTable);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        //mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JPanel labelsPanel = new JPanel(new FlowLayout());
        labelsPanel.add(captionLocationLabel);
        labelsPanel.add(locationLabel);
        
        mainPanel.add(BorderLayout.NORTH, labelsPanel);
        mainPanel.add(BorderLayout.SOUTH, buttonsPanel);
        mainPanel.add(BorderLayout.WEST, structureTables);
        mainPanel.add(BorderLayout.EAST, summaryTablePane);
        
        add(mainPanel, BorderLayout.CENTER);
        
        readSettings();
        
        String locationPath = locationLabel.getText();
        
        messageQueue = new MARmq(locationPath);
        messageQueue.activateReceiver();
        
        tablesGenner = new JTableGen(this);
        
        foldersWatcher = new FoldersWatcher(tablesGenner);
        foldersWatcher.checkWorkingDir(locationPath);
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
                String locationPath = selectedFile.getPath();
                
                locationLabel.setText(locationPath);
                messageQueue.updateProjectPath(locationPath);
                try {
                    foldersWatcher.checkWorkingDir(locationPath);
                } catch (ParseException ex) {
                    Logger.getLogger(MarForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MarForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParsingException ex) {
                    Logger.getLogger(MarForm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (e.getSource() == getClientsTables) {
            //messageQueue.clickReceivedButton();
        } else if (e.getSource() == deleteColumnItem) {            
            int selectionColumn = summaryTable.getSelectedColumn();
            //System.out.println("delete menu click" + selectionColumn);
            summaryTable.removeColumnAndData(selectionColumn);
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
    
    public void sendFileToTopic(String filePath) throws IOException {
        System.out.println("send file: " + filePath);
        messageQueue.sendFile(filePath);
    }

}

class HeaderRenderer extends DefaultTableCellRenderer {
    
	// возвращает компонент для прорисовки
	public Component getTableCellRendererComponent(
		JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) 
	{
		JLabel label =
			(JLabel) super.getTableCellRendererComponent(
			 	table, value, isSelected, hasFocus,
					row, column);
		label.setBackground(Color.lightGray);
		label.setBorder(BorderFactory.createLineBorder(java.awt.Color.gray));
		label.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 15));
		label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		return label;
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
    
    //mini-hack with description column
    private boolean descriptionEnabled = true;
    
    protected void importString(JComponent c, String str) {
        SummaryTable target = (SummaryTable)c;
        
        TableModel tableModel= target.getModel();
        DefaultTableModel model = (DefaultTableModel)tableModel;

        String[] values = str.split("\n");
        
        for (int i = 0; i < values.length; i++) {
            model.addColumn(values[i]);
            
            if (descriptionEnabled) {
                //remove description column (index = 0)
                target.removeColumnAndData(0);
                descriptionEnabled = false;
            }
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
