package com.marati.marbuilder;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Марат
 */

class MARDefaultTableModel extends DefaultTableModel {
    
    private Map<String, ArrayList<String>> mappingWithSchemes = new HashMap<String, ArrayList<String>>();
    
    public Vector getColumnIdentifiers() {
        return columnIdentifiers;
    }
    
    public void setMapiingSchema(String schemeName, String columnName) {
        if (mappingWithSchemes.containsKey(schemeName)) {
            for (Map.Entry<String, ArrayList<String>> entrySchemas: mappingWithSchemes.entrySet()) {
                if (schemeName.equals(entrySchemas.getKey())) {
                    ArrayList<String> columns = entrySchemas.getValue();
                    columns.add(columnName);

                    entrySchemas.setValue(columns);
                }
            }
        } else {
            ArrayList<String> columns = new ArrayList<String>();
            columns.add(columnName);
            
            mappingWithSchemes.put(schemeName, columns);
        }
    }
    
    /*public String getSchemeNameByIndex(int indexColumn) {
        return
                mappingWithSchemes.containsKey(indexColumn) ?
                mappingWithSchemes.get(indexColumn) :
                new String();
    }*/
    
    public Map<String, ArrayList<String>> getChoosedColumns() {
        return mappingWithSchemes;
    }
}

public class SummaryTable extends JTable {
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
