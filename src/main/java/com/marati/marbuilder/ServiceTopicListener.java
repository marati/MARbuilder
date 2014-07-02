package com.marati.marbuilder;

import java.util.*;
import javax.jms.*;

import org.apache.log4j.Logger;
/**
 *
 * @author Марат
 */
public class ServiceTopicListener implements MessageListener {
    private static MARmq messageQueue;
    private final static String serviceTopic = "ServiceTopic";
    private final static Logger logger = Logger.getLogger(ServiceTopicListener.class);

    public ServiceTopicListener(MARmq mq) {
        messageQueue = mq;
    }
    
    public void onMessage(Message msg) {
        TextMessage textMessage = (TextMessage)msg;
        
        try {
            String ip = msg.getStringProperty("destination_ip");
            String messageId = msg.getJMSMessageID();
            
            logger.info("receive message: [IP " + ip + "], " +
                        "[ID " + messageId + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");
            
            final String myIpAddr = messageQueue.getIp();

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
                String responseCommand = "SEND";
                
                String schemeName = msg.getStringProperty("scheme");
                String rawColumnsStr = msg.getStringProperty("columns");
                String columnsStr = rawColumnsStr.substring(1, rawColumnsStr.length() - 1);
                
                logger.info("receive GET");
                logger.info("scheme property: " + schemeName);
                logger.info("columns property: " + columnsStr);
                
                String fileName = messageQueue.getAttributeFromDatabase("file_name", schemeName);
                
                Destination reportDestination = msg.getJMSReplyTo();
                logger.info("destination sender: " + msg.getJMSReplyTo().toString());
                
                String fullDestinationTopic = reportDestination.toString();
                String destinationTopic = fullDestinationTopic.replace("topic://", "");
                
                //запоминаем кому и куда посылаем сообщение
                MARmqDatabase.saveSourceMapping(
                        schemeName, columnsStr.toString(), destinationTopic);
                
                DataSender.sendXmlDataMessage(responseCommand, reportDestination,
                        schemeName, fileName, columnsStr);
                
                logger.info("send data message to topic: " + fullDestinationTopic);
            }
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
