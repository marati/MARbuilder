package com.marati.marbuilder;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.net.*;

import nu.xom.ParsingException;

import gen.DocUtil;
import java.util.logging.Level;


/**
 *
 * @author marat
 */
public class MARmq {
    
    private static ActiveMQConnectionFactory connectionFactory = null;
    private static Connection connection = null; 
    private static Session session;
    private static Destination mainDestination; 
    private static ServiceTopicListener serviceListener = null;
    private static BuildReport buildReport = null;
    
    private static Logger logger = Logger.getLogger(MARmq.class);
    private final MARmqDatabase marDatabase = new MARmqDatabase();
    
    private final static String xsdTopic = "XsdTopic";
    private final static String serviceTopic = "ServiceTopic";
    private static String projectPath = null;
    private final DocUtil docUtil;
    private final DataSender dataSender;
    
    public MARmq(DocUtil util) {
        docUtil = util;
        dataSender = new DataSender(this, xsdTopic);
    }
    
    public DocUtil getDocUtil() {
        return docUtil;
    }
    
    public DataSender getDataSender() {
        return dataSender;
    }
    
    public void updateProjectPath(String path) {
        projectPath = path;
        docUtil.setProjectPath(path);
    }
    
    private static ActiveMQConnectionFactory getConnectionFactory(){
        return new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, 
                                             ActiveMQConnection.DEFAULT_PASSWORD, 
                                             "failover://tcp://localhost:61616");
    }
    
    public /*static*/ Boolean Connected() {
        try {
            if (connection == null) {
                connectionFactory = getConnectionFactory();
                connection = connectionFactory.createConnection();
                
                int clientId = (int) (1 + Math.random() * 100);
                String clientIdentificator = new String("ID: "+clientId+" PATH: ("+projectPath+")");
                connection.setClientID("MARbuilder"+clientIdentificator);
                connection.start();
                
                //сессия без транзакций
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            } else {
                connection.start();
                
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            }
            
            return true;
        } catch (JMSException ex) {
            logger.error(ex);
            return false;
        }
    }
    
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                logger.info(ex);
            }
        }
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public static Session getSession() {
        return session;
    }

    public Destination getDestinationTopic(String topicName) {
        Destination currentDestination = null;
        
        try {
            if (Connected()) {
                currentDestination = session.createTopic(topicName);
            }
        } catch (JMSException ex) {
            logger.error(ex);
        }
        
        return currentDestination;
    }
    
    /*public static Destination getDestinationQueue(String queueName) {
        try {
            return session.createQueue(queueName);
        } catch (JMSException ex) {
            logger.error(ex);
            return null;
        }
    }*/
    
    public void activateReceiver() {
        if (Connected()) {
            mainDestination = getDestinationTopic(xsdTopic);
            
            if (mainDestination != null) {
                try {
                    ActiveMQTopic topic = (ActiveMQTopic)mainDestination;
                    MessageConsumer consumer = session.createDurableSubscriber(
                            topic,
                            "mainSubFromPath("+projectPath+")");
                    consumer.setMessageListener(new XsdTopicListener(this, projectPath));
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
            
            Destination serviceDestination = getDestinationTopic(serviceTopic);
            if (serviceDestination != null) {
                try {
                    ActiveMQTopic topic = (ActiveMQTopic)serviceDestination;
                    MessageConsumer consumer = session.createDurableSubscriber(
                            topic,
                            "serviceSubFromPath("+projectPath+")");
                    serviceListener = new ServiceTopicListener(this);
                    consumer.setMessageListener(serviceListener);
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
        } else {
            logger.info("Соединение закрыто");
        }
    }
    
    public ReportTopicListener subscribeToTopic(String topicName
            /*TreeMap<String, ArrayList<String>> expectedColumns*/) {
        ReportTopicListener reportListener = new ReportTopicListener(this);
        
        if (Connected()) {
            
            Destination topicDestination = getDestinationTopic(topicName);
            if (topicDestination != null) {
                try {
                    ActiveMQTopic topic = (ActiveMQTopic)topicDestination;
                    String nameSubscriber = topicName + "SubFromPath("+projectPath+")";
                    
                    MessageConsumer consumer = session.createDurableSubscriber(
                            topic, nameSubscriber);
                    consumer.setMessageListener(reportListener);
                    
                    logger.info("subscribe to topic: " + topicName);
                    logger.info("subscriber: " + nameSubscriber);
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
        }
        
        return reportListener;
    }
    
    public static String getIp() {
        String ipv4 = null;
        Enumeration<NetworkInterface> nets;
        
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            
            Boolean breakCycle = false; 
            
            for (NetworkInterface netint : Collections.list(nets)) {
                
                String netintName = netint.getDisplayName();
                if (netintName.contains("Loopback"))
                    continue;
                
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    Pattern ip = Pattern.compile("/[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
                    String inetAddr = inetAddress.toString();
                    
                    if (ip.matcher(inetAddr).matches()) {
                        ipv4 = inetAddr.substring(1, inetAddr.length());
                        logger.info("IP: " + ipv4);
                        breakCycle = true;
                        break;
                    }
                }
                
                if (breakCycle)
                    break;
            }
            
            
        } catch (SocketException ex) {
            logger.error(ex);
        }
        
        return ipv4;
    }
    
    /*сериализация messageIds пока не нужна, сделан переход к хранению в БД
    public String getReceiveIds() {
        ByteArrayOutputStream messagesBuffer = new ByteArrayOutputStream();
        
        try {
            ObjectOutputStream outputMessages = new ObjectOutputStream(messagesBuffer);
            outputMessages.writeObject(messageIds);
            outputMessages.close();
        } catch (IOException ex) {
           logger.error(ex);
        }
        
        return messagesBuffer.toString();
    }
    
    public void setReceiveIds(String receivedIds) {
        ObjectInputStream inputMessages = null;
        try {
            ByteArrayInputStream messagesBuffer = new ByteArrayInputStream(receivedIds.getBytes());
            inputMessages = new ObjectInputStream(messagesBuffer);
            
            try {
                messageIds = (ArrayList)inputMessages.readObject();
            } catch (ClassNotFoundException ex) {
                logger.error(ex);
            }
            
            inputMessages.close();
            messagesBuffer.close();
        } catch (IOException ex) {
            logger.error(ex);
        } finally {
            try {
                inputMessages.close();
            } catch (IOException ex) {
                logger.error(ex);
            }
        }
    }*/
    

    
    /*public void saveMapping(String messageId, String ip, String schemeName, String fileName) {
        marDatabase.saveMapping(messageId, ip, schemeName, fileName);
    }*/
    
    public Boolean messageContains(String id) {
        return marDatabase.messageIdContains(id);
    }
    
    public String getAttributeFromDatabase(String attribute, String schemeName) {
        return MARmqDatabase.getAttributeBySchemeName(attribute, schemeName);
    }
    
    public void buildReport(String reportName, Map<String, ArrayList<String>> choosedColumns) {
        buildReport = new BuildReport(this);
        buildReport.buildReport(reportName, choosedColumns);
    }
    
    public void schemeMessageReceived(String xsdDir, String fileName) {
        HashMap<String, ArrayList<String>> dirsAndTheirFiles = new HashMap<String, ArrayList<String>>();

        ArrayList<String> filesNameWithoutExt = new ArrayList<String>();
        int dotPos = fileName.lastIndexOf(".");
        filesNameWithoutExt.add( fileName.substring(0, dotPos) );

        dirsAndTheirFiles.put(xsdDir, filesNameWithoutExt);
        try {
            docUtil.createTablesFromXsd(dirsAndTheirFiles);
        } catch (ParsingException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
    
    public Map<String, ArrayList<String>> createXmlData(String fileName, String strColumns) {
        return docUtil.createSerializableXmlData(fileName, strColumns);
    }
    
    public void updatedData(String fileName) {
        logger.info("update file : " + fileName);
        
        int dotPos = fileName.lastIndexOf(".");
        String fileNameWithoutExt = fileName.substring(0, dotPos);
        
        dataSender.sendUpdateData(fileNameWithoutExt+".xsd");
    }

}
