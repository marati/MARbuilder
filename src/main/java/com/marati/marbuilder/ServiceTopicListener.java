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
public class ServiceTopicListener implements MessageListener {
    private MARmq messageQueue;
    private String projectPath;
    private static Logger logger = Logger.getLogger(ServiceTopicListener.class);

    public ServiceTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        projectPath = projPath;
    }
    
    public void onMessage(Message msg) {
        TextMessage textMessage = (TextMessage)msg;
        
        try {
            String ip = msg.getStringProperty("ip");
            
            logger.info("receive message: [IP " + ip + "], " +
                        "[ID " + msg.getJMSMessageID() + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");
            
            String command = textMessage.getText();
            
            if (command.equals("GET")) {
                logger.info("scheme property: " + msg.getStringProperty("scheme"));
                logger.info("columns property: " + msg.getStringProperty("columns"));
            }
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
