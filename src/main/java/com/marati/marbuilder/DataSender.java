package com.marati.marbuilder;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.jms.*;
import org.apache.log4j.Logger;

import static com.marati.marbuilder.MARmq.getDestinationTopic;
import static com.marati.marbuilder.MARmq.getIp;

/**
 *
 * @author Марат
 */
public class DataSender {
    private MARmq mqManager;
    private static String xsdTopicName;
    private static Logger logger = Logger.getLogger(DataSender.class);
    
    public DataSender(MARmq messageQueue, String xsdName) {
        mqManager = messageQueue;
        xsdTopicName = xsdName;
    }
    
    public void sendXsdFile(String filePath, String rootElementName) throws IOException {
        if (mqManager.Connected()) {
            Destination xsdDestination = mqManager.getDestinationTopic(xsdTopicName);
            
            if (xsdDestination != null) {
                try {
                    MessageProducer producer = mqManager.getSession().createProducer(xsdDestination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    File fileToSending = new File(filePath);
                    InputStream is = new FileInputStream(fileToSending);
                    
                    long length = fileToSending.length();
                    if (length > Integer.MAX_VALUE) {
                        logger.debug("Файл очень большой");
                    }
                    
                    BytesMessage bytesMessage = mqManager.getSession().createBytesMessage();
                    
                    //write ip & filename properties
                    String ip = getIp();
                    bytesMessage.setStringProperty("ip", ip);
                    bytesMessage.setStringProperty("scheme_name", rootElementName);
                    bytesMessage.setStringProperty("filename", fileToSending.getName());
                    
                    byte[] bytes = new byte[(int)length];
                    int offset = 0;
                    int numRead = 0;
                    
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");

                        while (offset < bytes.length && numRead >= 0) {
                            numRead = is.read(bytes, offset, bytes.length - offset);

                            bytesMessage.writeBytes(bytes, offset, bytes.length - offset);
                            md.update(bytes, offset, bytes.length - offset);

                            offset += numRead;
                        }

                        if (offset < bytes.length) {
                            throw new IOException("Не удалось прочитать файл " + fileToSending.getName());
                        }

                        String md5file = new BigInteger(1, md.digest()).toString(16);
                        MARmqDatabase.saveMessageId(md5file);
                        bytesMessage.setStringProperty("md5", md5file);
                        
                        logger.info("preparation to send message, ID: " + md5file);
                        logger.info(
                                String.format("properties: ip (%s), scheme_name (%s), filename (%s)",
                                ip, rootElementName, fileToSending.getName()));
                        
                    } catch (final NoSuchAlgorithmException e) {
                        logger.error(e.toString());
                    }

                    is.close();
                    
                    producer.send(bytesMessage);
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
        } else {
            logger.info("Соединение закрыто");
        }
    }
}
