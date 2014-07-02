package com.marati.marbuilder;

import java.util.*;
import javax.jms.*;
import org.apache.log4j.Logger;

import gen.DocUtil;
/**
 *
 * @author Марат
 */
public class ReportTopicListener implements MessageListener {
    private static MARmq messageQueue;
    private ArrayList<String> expectedColumns = new ArrayList<String>();
    //private TreeMap<String, ArrayList<String>> expectedColumnsWithValues =
            //new TreeMap<String, ArrayList<String>>();
    private Boolean arrivalSign = false;
    private final DocUtil docUtil;
    private final static Logger logger = Logger.getLogger(ReportTopicListener.class);

    public ReportTopicListener(MARmq mq) {
        messageQueue = mq;
        docUtil = messageQueue.getDocUtil();
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
            
            String command = msg.getStringProperty("command");
            String schemaName = msg.getStringProperty("scheme");
            String columnName = msg.getStringProperty("column");
            
            logger.info("receive MapMessage");
            logger.info("scheme property: " + schemaName);
            logger.info("column name: " + columnName);
            
            logger.info("comand: " + command);
//            if (command.equals("SEND")) {
//                
//            } else if (command.equals("UPD")) {
//                
//            }

            String rawValues = textMessage.getText();
            String[] valuesArray = rawValues.split("\\|\\s*");

            ArrayList<String> expectedValues = new ArrayList<String>();
            expectedValues.addAll(Arrays.asList(valuesArray));

            docUtil.addRowsInSummaryTable(columnName, expectedValues);

            //messageQueue.getSession().close();
            //messageQueue.getConnection().close();
            
        } catch (JMSException ex) {
            logger.error(ex);
        }
    }
}
