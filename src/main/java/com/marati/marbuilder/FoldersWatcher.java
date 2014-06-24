package com.marati.marbuilder;

import java.io.*;
import java.nio.charset.Charset;

import java.util.*;
import org.apache.log4j.Logger;
/*import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;*/

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;

import gen.ParseException;
import gen.XsdGen;
import gen.DocUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import nu.xom.ParsingException;
import org.apache.commons.vfs2.FileSystemException;
/**
 *
 * @author marat
 */

public class FoldersWatcher {
    
    private String workingPath = "";
    private final XsdGen xsdGen;
    private final DocUtil tablesGen;
    private static MARmq messageQueue;
    private static final Logger logger = Logger.getLogger(FoldersWatcher.class);
    
    //private static DefaultFileMonitor fm = null;
    
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
    
    public void mainThreadUpdFunc(String fileName) {
        messageQueue.updatedData(fileName);
    }
    
    public void setWatching(/*String[] dirsName*/) {
        
        Executor runner = Executors.newFixedThreadPool(1);
        runner.execute(new Runnable() {

        @Override
        public void run() {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                
                Path dir = Paths.get(workingPath + File.separator + "xml");
                WatchKey key = dir.register(watcher, ENTRY_MODIFY);
                
                for (;;) {
                    key = watcher.take();
                    
                    for (WatchEvent<?> event: key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        
                        WatchEvent<Path> ev = (WatchEvent<Path>)event;
                        Path fileName = ev.context();
                        
                        logger.info("file Update : " + fileName.toString());
                        
                        mainThreadUpdFunc(fileName.toString());
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                        
                } catch (FileSystemException ex) {
                    logger.error(ex);
                } catch (IOException ex) {
                    logger.error(ex);
                } catch (InterruptedException ex) {
                    logger.error(ex);
                }
            }
        });
    }
    
    public void checkWorkingDir(String workingDir) throws ParseException, IOException, ParsingException {
        if (workingPath.equals(workingDir))
            return;
            
        workingPath = workingDir;
        
        messageQueue.updateProjectPath(workingPath);
        messageQueue.activateReceiver();
        
        //makes new folders
        String[] dirs = {
            "xml/xsd/", "xlc/xsd/", "xsd"
        };
        
        for (int i = 0; i < dirs.length; ++i) {
            File subWorkingDir = new File(
                    workingPath + File.separator + dirs[i]
            );
            subWorkingDir.mkdirs();
        }
        
        String[] extDirs = {"xml", "xlc"};
        setWatching(/*extDirs*/);
        checkCurrentExtDir(extDirs);
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
    
    /*static class DirsListener implements FileListener {  
        private static int counter = 0;
        
        public void fileCreated(FileChangeEvent fileChangeEvent)
                        throws Exception {
                    System.out.println("file created : "
                            + fileChangeEvent.getFile().getName());
        }

        public void fileDeleted(FileChangeEvent fileChangeEvent)
                throws Exception {
            System.out.println("file deleted : "
                    + fileChangeEvent.getFile().getName());
        }

        public void fileChanged(FileChangeEvent fileChangeEvent)
                throws Exception {
            String fileName = fileChangeEvent.getFile().getName().getBaseName();
            System.out.println(String.format(
                    "File [%s] changed event from [%s], counter[%s]", fileChangeEvent
                            .getFile().getName(), this, ++counter));
            
            synchronized (this) { //notify waiting thread
                this.notifyAll();
                this.wait(7000);
                messageQueue.updatedData(fileName);
            }
            
        }
    }*/
}
