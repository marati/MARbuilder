package com.marati.marbuilder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Марат
 */

class MARDefaultTableModel extends DefaultTableModel {
    
    private Map<String, ArrayList<String>> mappingWithSchemes = new HashMap<String, ArrayList<String>>();
    private Map<String, Integer> mappingColumnsAndIds = new HashMap<String, Integer>();
    
    public Vector getColumnIdentifiers() {
        return columnIdentifiers;
    }
    
    public void setMappingScheme(String schemeName, String columnName, int index) {
        if (mappingWithSchemes.containsKey(schemeName)) {
            //refact: mappingWithSchemes.get, убрать массив
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
        
        if (!mappingColumnsAndIds.containsKey(columnName)) {
            System.out.println("set mapping: column" + columnName + ", index " + index);
            mappingColumnsAndIds.put(columnName, index);
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
    
    // Map<String, ArrayList<String>> : String - columnName, int - index
    public int getIndexByColumnName(String columnName) {
        System.out.println("mapping: " + mappingColumnsAndIds.toString());
        System.out.println("get column:" + columnName);
        return mappingColumnsAndIds.get(columnName);
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
