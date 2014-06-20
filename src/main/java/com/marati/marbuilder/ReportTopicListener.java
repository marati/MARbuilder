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
public class ReportTopicListener implements MessageListener {
    private MARmq messageQueue;
    private String projectPath;
    private final static String topicName = null;
    private final static Logger logger = Logger.getLogger(ServiceTopicListener.class);

    public ReportTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        projectPath = projPath;
    }
    
    public void onMessage(Message msg) {
        TextMessage mapMessage = (TextMessage)msg;
        
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
            
            String schemaName = msg.getStringProperty("scheme");
            String columnName = msg.getStringProperty("column");
            String values = msg.getStringProperty("values");
            //String columns = mapMessage.getString(columnName);
            
            logger.info("receive MapMessage");
            logger.info("scheme property: " + schemaName);
            logger.info("column name: " + columnName);
            logger.info("values: " + values);
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
