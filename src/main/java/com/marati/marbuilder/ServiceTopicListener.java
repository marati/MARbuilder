package com.marati.marbuilder;

import java.util.*;
import javax.jms.*;

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
    
    public void sendMessage() {
        
    }
    
    public void onMessage(Message msg) {
        TextMessage textMessage = (TextMessage)msg;
        
        try {
            String ip = msg.getStringProperty("destination_ip");
            String messageId = msg.getJMSMessageID();
            
            logger.info("receive message: [IP " + ip + "], " +
                        "[ID " + messageId + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");
            
            String myIpAddr = messageQueue.getIp();

            if (myIpAddr == null) {
                logger.error("IP address not defined");
                return;
            }
            //если совпадают, то принимаем
            if (myIpAddr.equals(ip)) {
                logger.info("receive my message: IP sender = IP receiver [" + myIpAddr + "]");
            } else {
                logger.info("receive message: IP sender != IP receiver [" +
                        ip + " != " + myIpAddr + "]");
                return;
            }
            
            String command = textMessage.getText();
            
            if (command.equals("GET")) {
                
                String schemeName = msg.getStringProperty("scheme");
                String rawColumnsStr = msg.getStringProperty("columns");
                String columnsStr = rawColumnsStr.substring(1, rawColumnsStr.length() - 1);
                
                logger.info("receive GET");
                logger.info("scheme property: " + schemeName);
                logger.info("columns property: " + columnsStr);
                
                String fileName = messageQueue.getAttributeFromDatabase("file_name", schemeName);
                
                Map<String, ArrayList<String>> dataFromXml =
                        messageQueue.createXmlData(fileName, columnsStr);
                
                Destination reportDestination = msg.getJMSReplyTo();
                logger.info("destination sender: " + msg.getJMSReplyTo().toString());
                
                String fullDestinationTopic = reportDestination.toString();
                String destinationTopic = fullDestinationTopic.replace("topic://", "");
                
                //запоминаем кому и куда посылаем сообщение
                MARmqDatabase.saveSourceMapping(
                        schemeName, columnsStr.toString(), destinationTopic);
                
                if (messageQueue.Connected()) {
                
                    if (reportDestination != null) {
                        MessageProducer producer = messageQueue.getSession().createProducer(reportDestination);
                        producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                        String messageText = "SEND";

                        for (Map.Entry<String, ArrayList<String>> entryData: dataFromXml.entrySet()) {
                            TextMessage sendMessage = messageQueue.getSession().createTextMessage();
                            sendMessage.setJMSCorrelationID(messageId);

                            String columnName = entryData.getKey();
                            String columnValues = entryData.getValue().toString();
                            //sendMessage.setString(columnName, columnValues);

                            sendMessage.setStringProperty("scheme", schemeName);
                            sendMessage.setStringProperty("column", columnName);
                            sendMessage.setStringProperty("ip", myIpAddr);
                            sendMessage.setStringProperty("values", columnValues);

                            sendMessage.setText(messageText);

                            logger.info("preparation to send data [tableColumn: " + columnName + "]");
                            logger.info("save dest: " + destinationTopic);

                            producer.send(sendMessage);
                        }
                        
                        //после отправки ответа клиентам - закрываем соединение
                        messageQueue.getSession().close();
                        
                    }
                }
            }
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
