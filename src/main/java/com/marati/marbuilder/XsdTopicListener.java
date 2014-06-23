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
public class XsdTopicListener implements MessageListener {

    private MARmq messageQueue;
    private String projectPath;
    private static Logger logger = Logger.getLogger(XsdTopicListener.class);

    public XsdTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        projectPath = projPath;
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
                        "[ID " + msg.getJMSMessageID() + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");

            //mini-hardcode: своё сообщение не принимаем
            /*if (messageQueue.messageContains(msg.getStringProperty("md5"))) {
                System.out.println("своё сообщение пришло" + msg.getStringProperty("md5"));
                return;
            }*/

            //сохраняем записаь о пришедшем сообщении в БД
            MARmqDatabase.saveMapping(msg.getJMSMessageID(), ip, schemeName, fileName);

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

            messageQueue.schemeMessageReceived(xsdDir, fileName);

        } catch (JMSException ex) {
            logger.error(ex);
        } catch (FileNotFoundException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
}
