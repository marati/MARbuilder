package com.marati.marbuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import gen.ParseException;
import gen.XsdGen;
import gen.JTableGen;
import nu.xom.ParsingException;

import com.marati.marbuilder.MARmq;
/**
 *
 * @author marat
 */
public class FoldersWatcher {
    
    private String workingPath = "";
    private final XsdGen xsdGen;
    private final JTableGen tablesGen;
    private final MARmq messageQueue;
    
    public FoldersWatcher(JTableGen tablesGenner) {
        xsdGen = new XsdGen();
        tablesGen = tablesGenner;
        
        messageQueue = new MARmq(tablesGen);
    }
    
    private String[] getWorkingFiles(String dirName) {
        File currentWorkingDir = new File(
                workingPath + File.separator + dirName
        );
        
        return currentWorkingDir.list(new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return fileName.endsWith(".xml") ||
                       fileName.endsWith(".xlc") ||
                       fileName.endsWith(".xsd");
            }
        });
    }
    
    //Вызывается при запуске программы, для проверки на созданные xsd
    private void checkCurrentExtDir(String[] dirsName) throws ParseException, IOException, ParsingException {
        HashMap<String, ArrayList<String>> dirsAndTheirFiles = new HashMap<String, ArrayList<String>>();
        
        for (String dir : dirsName) {
            String currentExtDir = new String(
                    workingPath + File.separator + dir
            );
            
            String xsdDir = new String(
                    currentExtDir + File.separator + "xsd"
            );
            
            File xmlFile = null;
            ArrayList<String> filesNameWithoutExt = new ArrayList<String>();
            String[] filesNameWithExt = getWorkingFiles(dir);
            
            if (filesNameWithExt.length == 0)
                continue;
            
            for (String fileName : filesNameWithExt) {
                xmlFile = new File(currentExtDir + File.separator + fileName);
                
                int dotPos = fileName.lastIndexOf(".");
                filesNameWithoutExt.add( fileName.substring(0, dotPos) );
            }
            
            dirsAndTheirFiles.put(xsdDir, filesNameWithoutExt);
            
            //Проверить, существует ли xsd; если нет - создать
            for (String fileName : filesNameWithoutExt) {
                File currentFile = new File(
                        xsdDir + File.separator + fileName + ".xsd"
                );
                
                System.out.println("working file: " + currentFile.toString());
                
                if (currentFile.length() == 0) {
                    OutputStream os = new FileOutputStream(currentFile);
                    xsdGen.parse(xmlFile).write(os, Charset.forName("UTF-8"));
                    
                    messageQueue.sendFile(currentFile.getAbsolutePath());
                }
                
            }
        }
        
        tablesGen.createTablesFromXsd(dirsAndTheirFiles);
    }
    
    public void checkWorkingDir(String workingDir) throws ParseException, IOException, ParsingException {
        if (workingPath.equals(workingDir))
            return;
            
        workingPath = workingDir;
        
        messageQueue.updateProjectPath(workingPath);
        messageQueue.activateReceiver();
        
        //makes new folders
        String[] dirs = {
            "xml/uploads/", "xml/xsd/", "xlc/uploads/", "xlc/xsd/", "xsd"
        };
        
        for (int i = 0; i < dirs.length; ++i) {
            File subWorkingDir = new File(
                    workingPath + File.separator + dirs[i]
            );
            subWorkingDir.mkdirs();
        }
        
        String[] extDirs = {"xml", "xlc"};
        checkCurrentExtDir(extDirs);
    }
    
    public String getReceivedMessageIdsByString() {
        return messageQueue.getReceiveIds();
    }
    
    public void setReceivedMessageIds(String messageIds) {
        messageQueue.setReceiveIds(messageIds);
    }
}
