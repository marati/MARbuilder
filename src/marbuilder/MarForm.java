package marbuilder;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.JFileChooser.*;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author marat
 */
public class MarForm extends JPanel
                        implements ActionListener {
    JButton openFileButton;
    JLabel locationLabel;
    JFileChooser fileChooser;
    
    public MarForm() {
        super(new BorderLayout());
        
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        openFileButton = new JButton("Выберите папку для Вашего проекта...");
        openFileButton.addActionListener(this);
        
        locationLabel = new JLabel("Рабочая директория: "); //TODO: при загрузке брать из настроек
        
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.Y_AXIS));
        startPanel.add(openFileButton);
        startPanel.add(locationLabel);
        
        add(startPanel, BorderLayout.PAGE_START);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openFileButton) {
            System.out.println("openfb");
            int returnValue = fileChooser.showOpenDialog(MarForm.this);
            
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                locationLabel.setText(locationLabel.getText() + selectedFile.getName());
            }
        }
    }

}
