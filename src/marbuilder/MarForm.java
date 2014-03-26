package marbuilder;

import java.io.*;
import java.util.prefs.Preferences;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.WindowAdapter;
import javax.swing.*;
import javax.swing.JFileChooser.*;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author marat
 */


public class MarForm extends JFrame
                        implements ActionListener {
    JButton openFileButton;
    JLabel captionLocationLabel;
    JLabel locationLabel;
    JFileChooser fileChooser;
    
    private static final String LOCATION = "location";
    
    public MarForm(String title) {
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
        
        captionLocationLabel = new JLabel("Рабочая директория:");
        locationLabel = new JLabel("не задана");
        
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Box labelsBox = new Box(BoxLayout.X_AXIS);
        labelsBox.add(captionLocationLabel);
        labelsBox.add(locationLabel);
        
        mainPanel.add(BorderLayout.WEST, openFileButton);
        mainPanel.add(BorderLayout.WEST, labelsBox);
        
        add(mainPanel, BorderLayout.CENTER);
        
        readSettings();
    }
    
    private void readSettings() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String location = prefs.get(LOCATION, "не задана");
        System.out.println(location);
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

}
