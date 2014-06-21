package com.marati.marbuilder;

import java.io.*;
import java.util.*;
import javax.jms.*;
import nu.xom.ParsingException;
import org.apache.log4j.Logger;

import gen.DocUtil;
/**
 *
 * @author Марат
 */
public class ReportTopicListener implements MessageListener {
    private MARmq messageQueue;
    private String projectPath;
    private TreeMap<String, ArrayList<String>> expectedColumns;
    private Boolean arrivalSign = false;
    private String addingRow = null;
    //private DocUtil docUtil;
    private final static String topicName = null;
    private final static Logger logger = Logger.getLogger(ServiceTopicListener.class);

    public ReportTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        projectPath = projPath;
        //docUtil = messageQueue.getDocUtil();
    }
    
    public void expectedColumns(TreeMap<String, ArrayList<String>> excected) {
        expectedColumns = excected;
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
            
            ArrayList<String> tepmValues = expectedColumns.get(columnName);

            String[] valuesArray = values.split("\\,");
            for (String value: valuesArray)
                tepmValues.add(value);

            
            Boolean arrivalSign = false;
            for (Map.Entry<String, ArrayList<String>> entryExpected: expectedColumns.entrySet()) {
                if (entryExpected.getValue().isEmpty()) {
                    arrivalSign = false;
                    break;
                }
                else
                    arrivalSign = true;

            }
            
            //if (arrivalSign)
                //вызов add row expectedColumns
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
