package com.marati.marbuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.jms.*;
import nu.xom.ParsingException;
import org.apache.log4j.Logger;

/**
 *
 * @author Марат
 */
public class MessageQueueListener implements MessageListener {

    private MARmq messageQueue;
    private static Logger logger = Logger.getLogger(MessageQueueListener.class);

    public MessageQueueListener(MARmq mq) {
        messageQueue = mq;
    }

    public void onMessage(Message msg) {
        BytesMessage bytesMessage = (BytesMessage)msg;

        try {
            String ip = msg.getStringProperty("ip");
            String schemeName = msg.getStringProperty("scheme_name");
            String fileName = msg.getStringProperty("filename");

            logger.info("receive message: [IP " + ip + "], " +
                        "[scheme name " + schemeName + "], " +
                        "[file name " + fileName + "], " +
                        "[ID " + msg.getJMSMessageID() + "]");

            //своё сообщение не принимаем
            if (messageIds.contains(msg.getStringProperty("md5")))
                return;

            //сохраняем записаь о пришедшем сообщении в БД
            messageQueue.saveMapping(ip, schemeName, fileName);

            byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
            bytesMessage.readBytes(bytes);

            int dotPos = fileName.lastIndexOf(".");

            String xsdDir = null;
            String extention = fileName.substring(dotPos);
            if (extention.equals(".xsd")) {
                xsdDir = new String(projectPath + File.separator + "xsd");
            } else {
                logger.error("fail parse file: " + extention);
                return;
            }

            File receivedFile = new File(xsdDir + File.separator + fileName);

            FileOutputStream fos = new FileOutputStream(receivedFile);
            try {
                fos.write(bytes);
                logger.info("write in file " + receivedFile.getAbsolutePath());
                fos.close();
            } catch (IOException ex) {
                logger.error(ex.toString());
            }

            HashMap<String, ArrayList<String>> dirsAndTheirFiles = new HashMap<String, ArrayList<String>>();
            ArrayList<String> filesNameWithoutExt = new ArrayList<String>();

            filesNameWithoutExt.add( fileName.substring(0, dotPos) );

            dirsAndTheirFiles.put(xsdDir, filesNameWithoutExt);

            tablesGen.createTablesFromXsd(dirsAndTheirFiles);

        } catch (JMSException ex) {
            logger.error(ex);
        } catch (FileNotFoundException ex) {
            logger.error(ex);
        } catch (ParsingException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
}
