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
    private DocUtil docUtil;
    private final static String topicName = null;
    private final static Logger logger = Logger.getLogger(ReportTopicListener.class);

    public ReportTopicListener(MARmq mq, String projPath, TreeMap<String, ArrayList<String>> expected) {
        messageQueue = mq;
        docUtil = messageQueue.getDocUtil();
        
        projectPath = projPath;
        expectedColumns = expected;
    }
    
    /*public void setExpectedColumns(TreeMap<String, ArrayList<String>> excected) {
        expectedColumns = excected;
    }*/
    
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
                logger.info("receive my message: IP sender = IP receiver [" +
                        ip + " = " + myIpAddr + "]");
                return;
            } else {
                logger.info("receive message: IP sender != IP receiver [" +
                        ip + " != " + myIpAddr + "]");
            }
            
            String schemaName = msg.getStringProperty("scheme");
            String columnName = msg.getStringProperty("column");
            String rawValues = msg.getStringProperty("values");
            //String columns = mapMessage.getString(columnName);
            
            logger.info("receive MapMessage");
            logger.info("scheme property: " + schemaName);
            logger.info("column name: " + columnName);
            logger.info("values: " + rawValues);
            
            ArrayList<String> tepmValues = expectedColumns.get(columnName);
            
            logger.info("adding values in map; column: " + columnName);

            String values = rawValues.substring(1, rawValues.length() - 1).
                        replace(" ", "");
            String[] valuesArray = values.split("\\,");
            for (String value: valuesArray)
                tepmValues.add(value);

            
            Boolean arrivalSign = false;
            for (Map.Entry<String, ArrayList<String>> entryExpected: expectedColumns.entrySet()) {
                if (entryExpected.getValue().isEmpty()) {
                    logger.info("emptyValues in column:" + entryExpected.getKey());
                    arrivalSign = false;
                    break;
                }
                else {
                    logger.info("not empty values in column:" + entryExpected.getKey());
                    logger.info("values: " + entryExpected.getValue().toString());
                    arrivalSign = true;
                }

            }
            
            if (arrivalSign)
                docUtil.addRowsInSummaryTable(expectedColumns);
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
