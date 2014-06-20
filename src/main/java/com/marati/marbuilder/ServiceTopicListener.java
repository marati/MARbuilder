package com.marati.marbuilder;

import java.io.*;
import java.util.*;
import javax.jms.*;
import nu.xom.ParsingException;
import org.apache.log4j.Logger;
/**
 *
 * @author Марат
 */
public class ServiceTopicListener implements MessageListener {
    private MARmq messageQueue;
    private String projectPath;
    private final static String serviceTopic = "ServiceTopic";
    private final static Logger logger = Logger.getLogger(ServiceTopicListener.class);

    public ServiceTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        projectPath = projPath;
    }
    
    public void onMessage(Message msg) {
        TextMessage textMessage = (TextMessage)msg;
        
        try {
            String ip = msg.getStringProperty("ip");
            String messageId = msg.getJMSMessageID();
            
            logger.info("receive message: [IP " + ip + "], " +
                        "[ID " + messageId + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");
            
            String myIpAddr = messageQueue.getIp();
            //если совпадают, то отколняем сооющение; своего не надо
            if (myIpAddr.equals(ip)) {
                logger.info("receive my message: IP sender = IP receiver [" + myIpAddr + "]");
                return;
            }
            
            String command = textMessage.getText();
            
            if (command.equals("GET")) {
                String schemaName = msg.getStringProperty("scheme");
                String rawColumnsStr = msg.getStringProperty("columns");
                String columnsStr = rawColumnsStr.substring(1, rawColumnsStr.length() - 1).
                        replace(" ", "");
                
                logger.info("receive GET");
                logger.info("scheme property: " + schemaName);
                logger.info("columns property: " + columnsStr);
                
                String fileName = messageQueue.getAttributeFromDatabase("file_name", schemaName);
                
                Map<String, ArrayList<String>> dataFromXml =
                        messageQueue.createXmlData(fileName, columnsStr);
                
                
                Destination reportDestionation = msg.getJMSDestination();
                
                if (reportDestionation != null) {
                    MessageProducer producer = messageQueue.getSession().createProducer(reportDestionation);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    //String messageText = "SEND";
                    
                    for (Map.Entry<String, ArrayList<String>> entryData: dataFromXml.entrySet()) {
                        MapMessage sendMessage = messageQueue.getSession().createMapMessage();
                        sendMessage.setJMSCorrelationID(messageId);
                        
                        String columnName = entryData.getKey();
                        String columnValues = entryData.getValue().toString();
                        sendMessage.setString(columnName, columnValues);
                        
                        sendMessage.setStringProperty("schema", schemaName);
                        sendMessage.setStringProperty("column", columnName);
                        sendMessage.setStringProperty("ip", myIpAddr);
                        
                        logger.info("preparation to send data [tableColumn: " + columnName + "]");
                        
                        producer.send(sendMessage);
                    }
                }
            }
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
