package com.marati.marbuilder;

import java.awt.datatransfer.*;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
//import static javax.swing.TransferHandler.COPY_OR_MOVE;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

//my
import com.marati.marbuilder.SummaryTable;
import com.marati.marbuilder.MARDefaultTableModel;

/**
 *
 * @author Марат
 */
public class TableTransferHandler extends TransferHandler {
    
    //mini-hack with description column
    private boolean descriptionEnabled = true;
    
    protected void importString(JComponent c, String str) {
        SummaryTable target = (SummaryTable)c;
        
        TableModel tableModel= target.getModel();
        MARDefaultTableModel model = (MARDefaultTableModel)tableModel;
        
        String[] tableNameAndValues = str.split("\\|");
        String tableName = tableNameAndValues[0];
        String rawValues = tableNameAndValues[1];
        
        String[] values = rawValues.split("\n");
        
        for (int i = 0; i < values.length; i++) {
            model.addColumn(values[i]);
            
            //last added id - model.getColumnCount()-1
            model.setMappingScheme(tableName, values[i], model.getColumnCount()-1);
            
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
