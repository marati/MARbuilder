package com.marati.marbuilder;

//import java.io.*;
import java.util.*;
import javax.jms.*;
//import nu.xom.ParsingException;
import org.apache.log4j.Logger;

import gen.DocUtil;
/**
 *
 * @author Марат
 */
public class ReportTopicListener implements MessageListener {
    private MARmq messageQueue;
    private String projectPath;
    private TreeMap<String, ArrayList<String>> expectedColumns =
            new TreeMap<String, ArrayList<String>>();
    private Boolean arrivalSign = false;
    private String addingRow = null;
    private DocUtil docUtil;
    private final static String topicName = null;
    private final static Logger logger = Logger.getLogger(ReportTopicListener.class);

    public ReportTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        docUtil = messageQueue.getDocUtil();
        
        projectPath = projPath;
    }
    
    public void setExpectedColumns(ArrayList<String> columns) {
        for (String columnName : columns) {
            ArrayList<String> stubList = new ArrayList<String>();
            expectedColumns.put(columnName, stubList);
        }
    }
    
    public void onMessage(Message msg) {
        TextMessage textMessage = (TextMessage)msg;
        
        try {
            String ip = msg.getStringProperty("ip");
            String messageId = msg.getJMSMessageID();
            
            logger.info("receive message: [IP " + ip + "], " +
                        "[ID " + messageId + "], " +
                        "[Destination " + msg.getJMSDestination() + "]");
            
            //String myIpAddr = messageQueue.getIp();
            //если совпадают, то отколняем сооющение; своего не надо
            /*if (myIpAddr.equals(ip)) {
                logger.info("receive my message: IP sender = IP receiver [" +
                        ip + " = " + myIpAddr + "]");
                return;
            } else {
                logger.info("receive message: IP sender != IP receiver [" +
                        ip + " != " + myIpAddr + "]");
            }*/
            
            String schemaName = msg.getStringProperty("scheme");
            String columnName = msg.getStringProperty("column");
            String rawValues = msg.getStringProperty("values");
            //String columns = mapMessage.getString(columnName);
            
            logger.info("receive MapMessage");
            logger.info("scheme property: " + schemaName);
            logger.info("column name: " + columnName);
            logger.info("values: " + rawValues);
            
            logger.info("map after adding: " + expectedColumns.toString());
            ArrayList<String> tepmValues = expectedColumns.get(columnName);
            //удаляем заглушечные значения
            //tepmValues.clear();
            
            String values = rawValues.substring(1, rawValues.length() - 1);
            String[] valuesArray = values.split("\\,");
            logger.info(String.format("adding values %s in map; column - %s",
                    Arrays.asList(valuesArray).toString(), columnName));
            tepmValues.addAll(Arrays.asList(valuesArray));
            
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
