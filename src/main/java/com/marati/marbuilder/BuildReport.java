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
                Destination currentTopicDestination = mqManager.getDestinationTopic(topicName);
                
                ReportTopicListener reportListener = mqManager.subscribeToTopic(topicName);
                
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
                        getMessage.setJMSReplyTo(currentTopicDestination);

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
    
    public void updateReport(String fileName) {
        if (mqManager.Connected()) {
            try {
                //берём из scheme_mapping имя файла
                String schemeName = MARmqDatabase.getAttributeByFileName("scheme_name", fileName);
                
                String columns = MARmqDatabase.getAttributeSourceByScheme("columns", schemeName);
                String topicName = MARmqDatabase.getAttributeSourceByScheme("destination_topic", schemeName);
                
                Destination currentTopicDestination = mqManager.getDestinationTopic(topicName);
                
                if (currentTopicDestination != null) {
                    MessageProducer producer = mqManager.getSession().createProducer(currentTopicDestination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    String messageText = "UPD";
                    
                    //получаем данные
                    Map<String, ArrayList<String>> dataFromXml =
                            mqManager.createXmlData(fileName, columns);
                    
                    for (Map.Entry<String, ArrayList<String>> entryData: dataFromXml.entrySet()) {
                        TextMessage sendMessage = mqManager.getSession().createTextMessage();
                        
                        String columnName = entryData.getKey();
                        String columnValues = entryData.getValue().toString();
                        //sendMessage.setString(columnName, columnValues);
                        
                        sendMessage.setStringProperty("scheme", schemeName);
                        sendMessage.setStringProperty("column", columnName);
                        sendMessage.setStringProperty("ip", mqManager.getIp());
                        sendMessage.setStringProperty("values", columnValues);
                        
                        sendMessage.setText(messageText);
                        
                        logger.info("preparation to update data [tableColumn: " + columnName + "]");
                        
                        producer.send(sendMessage);
                    }
                }
                
            } catch (JMSException ex) {
                logger.error(ex);
            }
        }
    }
}
