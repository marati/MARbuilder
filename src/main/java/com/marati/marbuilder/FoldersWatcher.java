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

/**
 *
 * @author marat
 */
public class FoldersWatcher {
    
    private String workingPath;
    private final XsdGen xsdGen;
    private final JTableGen tablesGen;
    //private static final String XSD_PATH = ""
    
    public FoldersWatcher(String absoluteWorkingPath, JTableGen tablesGenner) {
        workingPath = absoluteWorkingPath;
        xsdGen = new XsdGen();
        tablesGen = tablesGenner;
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
    private void checkCurrentExtDir(String[] dirsName) throws ParseException, IOException {
        Map<String, List<String>> dirsAndTheirFiles = new HashMap<String, List<String>>();
        
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
                    System.out.println("file not exist " + currentFile.toString());
                    OutputStream os = new FileOutputStream(currentFile);
                    System.out.println(currentFile.toString());
                    xsdGen.parse(xmlFile).write(os, Charset.forName("UTF-8"));
                }
            }
        }
        
        
    }
    
    public void checkWorkingDir() throws ParseException, IOException {
        if (workingPath.isEmpty())
            return;
        
        //makes new folders
        String[] dirs = {
            "xml/uploads/", "xml/xsd/", "xlc/uploads/", "xlc/xsd/"
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
}
