package com.marati.marbuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.log4j.Logger;

import gen.ParseException;
import gen.XsdGen;
import gen.DocUtil;
import nu.xom.ParsingException;

import com.marati.marbuilder.MARmq;
/**
 *
 * @author marat
 */
public class FoldersWatcher {
    
    private String workingPath = "";
    private final XsdGen xsdGen;
    private final DocUtil tablesGen;
    private final MARmq messageQueue;
    private static final Logger logger = Logger.getLogger(FoldersWatcher.class);
    
    public FoldersWatcher(DocUtil tablesGenner) {
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
            String currentExtDir = workingPath + File.separator + dir;
            
            String xsdDir = workingPath + File.separator + "xsd";
            
            String[] filesNameWithExt = getWorkingFiles(dir);
            if (filesNameWithExt.length == 0)
                continue;
            
            ArrayList<String> indexFilesWithoutExt = new ArrayList<String>();
            
            String[] allXsdFiles = getWorkingFiles("xsd");
            ArrayList<String> allXsdCollection = new ArrayList<String>();
            allXsdCollection.addAll(Arrays.asList(allXsdFiles));
            
            //Проверить, существует ли xsd; если нет - создать
            for (String fileName : filesNameWithExt) {
                
                int dotPos = fileName.lastIndexOf(".");
                String fileNameWithoutExt = fileName.substring(0, dotPos);
                
                String currentXsdFile = fileNameWithoutExt + ".xsd";
                if (!allXsdCollection.contains(currentXsdFile)) {
                    File xsdFile = new File(
                            xsdDir + File.separator + currentXsdFile
                    );
                    logger.info("File " + xsdFile.getAbsolutePath() + " not found, create XSD");
                    
                    File xmlFile = new File(currentExtDir + File.separator + fileNameWithoutExt + ".xml");
                    OutputStream os = new FileOutputStream(xsdFile);
                    String rootElementName = xsdGen.parse(xmlFile).write(os, Charset.forName("UTF-8"));
                    
                    messageQueue.sendFile(xsdFile.getAbsolutePath(), rootElementName);
                    

                }
                
            }
            
            //refact: создать перем. currentXsdDir (для xml и xsd)

            //то, что не отправляли в очередь, добавляем в виджет
            for (String xsdFile : allXsdFiles) {
                int dotPos = xsdFile.lastIndexOf(".");
                String xsdFileWithoutExt = xsdFile.substring(0, dotPos);
                
                indexFilesWithoutExt.add(xsdFileWithoutExt);
            }
            
            logger.info("index xsd :" + indexFilesWithoutExt.toString());
            dirsAndTheirFiles.put(xsdDir, indexFilesWithoutExt);
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
        //checkDownloadXsd();
    }
    
    /*public String getReceivedMessageIdsByString() {
        return messageQueue.getReceiveIds();
    }
    
    public void setReceivedMessageIds(String messageIds) {
        messageQueue.setReceiveIds(messageIds);
    }*/
    
    public void buildReport(String reportName, Map<String, ArrayList<String>> choosedColumns) {
        messageQueue.buildReport(reportName, choosedColumns);
    }
}
