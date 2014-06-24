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
    private ArrayList<String> expectedColumns = new ArrayList<String>();
    //private TreeMap<String, ArrayList<String>> expectedColumnsWithValues =
            //new TreeMap<String, ArrayList<String>>();
    private Boolean arrivalSign = false;
    private String addingRow = null;
    private DocUtil docUtil;
    //private final static String topicName = null;
    private final static Logger logger = Logger.getLogger(ReportTopicListener.class);

    public ReportTopicListener(MARmq mq, String projPath) {
        messageQueue = mq;
        docUtil = messageQueue.getDocUtil();
        
        projectPath = projPath;
    }
    
    public void setExpectedColumns(ArrayList<String> columns) {
        for (String columnName : columns) {
            if (!columnName.isEmpty())
                expectedColumns.add(columnName);
        //expectedColumns.addAll(columns);
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
            
            String schemaName = msg.getStringProperty("scheme");
            String columnName = msg.getStringProperty("column");
            String rawValues = msg.getStringProperty("values");
            
            logger.info("receive MapMessage");
            logger.info("scheme property: " + schemaName);
            logger.info("column name: " + columnName);
            logger.info("values: " + rawValues);
            
            logger.info("map after adding: " + expectedColumns.toString());
            
            String command = textMessage.getText();
            logger.info("comand: " + command);
            if (command.equals("SEND")) {
                String values = rawValues.substring(1, rawValues.length() - 1);
                String[] valuesArray = values.split("\\,\\s*");
                logger.info(String.format("adding values %s in map; column -%s",
                        Arrays.asList(valuesArray).toString(), columnName));

                ArrayList<String> expectedValues = new ArrayList<String>();
                expectedValues.addAll(Arrays.asList(valuesArray));

                docUtil.addRowsInSummaryTable(columnName, expectedValues);
            } else if (command.equals("UPD")) {
                logger.info("UPDATE msg receive");
            }
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
