package com.marati.marbuilder;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.jms.*;
import org.apache.log4j.Logger;

/**
 *
 * @author Марат
 */
public class BuildReport {
    
    private static MARmq mqManager;
    private final static Logger logger = Logger.getLogger(BuildReport.class);
    private final static String serviceTopic = "ServiceTopic";
    
    public BuildReport(MARmq mesageQueue) {
        mqManager = mesageQueue;
    }
    
    public void buildReport(String reportName, Map<String, ArrayList<String>> choosedColumns) {
        if (mqManager.Connected()) {
            try {
                String clientid = mqManager.getConnection().getClientID();
                String topicName = reportName + "_From_" + clientid;
                Destination currentTopicDestionation = mqManager.getDestinationTopic(topicName);
                
                ReportTopicListener reportListener = mqManager.subscribeToTopic(topicName);
                
                //refact: точка-точка
                Destination serviceDestination = mqManager.getDestinationTopic(serviceTopic);
                //Destination serviceDestination = mqManager.getDestinationQueue(
                        //String.format("build_%s_From_%s", reportName, clientid));
                
                if (serviceDestination != null) {
                    MessageProducer producer = mqManager.getSession().createProducer(serviceDestination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    //TreeMap<String, ArrayList<String>> columnsAndEmptyValues = new TreeMap<String, ArrayList<String>>();
                    
                    String messageText = "GET";
                    //рассылка GET сообщений всем клиентам с запросом колонок
                    for (Map.Entry<String, ArrayList<String>> entryChoosed: choosedColumns.entrySet()) {
                        String messageId = MARmqDatabase.getAttributeBySchemeName("message_id", entryChoosed.getKey());
                        String destinationIp = MARmqDatabase.getAttributeBySchemeName("ip", entryChoosed.getKey());
                        
                        TextMessage getMessage = mqManager.getSession().createTextMessage();
                        getMessage.setText(messageText);
                        //ответить на сообщение (ID из БД)
                        getMessage.setJMSCorrelationID(messageId);
                        //указание топика, на который должны будут прислать ответ клиенту-инициатору
                        getMessage.setJMSReplyTo(currentTopicDestionation);

                        getMessage.setStringProperty("destination_ip", destinationIp);
                        getMessage.setStringProperty("scheme", entryChoosed.getKey());
                        ArrayList<String> columns = entryChoosed.getValue();
                        getMessage.setStringProperty("columns", columns.toString());

                        System.out.print("[schema name: "+entryChoosed.getKey()+"] =>");
                        //System.out.println(entryChoosed.getValue().toString());
                        reportListener.setExpectedColumns(columns);
                        
                        producer.send(getMessage);
                    }
                    
                }
            } catch (JMSException ex) {
                logger.error(ex);
            }
            
        }
    }
}
