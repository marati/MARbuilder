package com.marati.marbuilder;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.marati.marbuilder.MarForm;
import gen.ParseException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author marat
 */
public class MARbuilder {

    private static void createAndShowGUI() throws ParseException, IOException {
        //Create main frame
        JFrame mainFrame = new MarForm("MARbuilder");

        //Display the window.
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
    
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                try {
                    createAndShowGUI();
                } catch (ParseException ex) {
                    Logger.getLogger(MARbuilder.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MARbuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
}
