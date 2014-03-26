package marbuilder;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import marbuilder.MarForm;

/**
 *
 * @author marat
 */
public class MARbuilder {

    private static void createAndShowGUI() {
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
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
    }
    
}
