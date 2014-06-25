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
import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nu.xom.ParsingException;

import com.marati.marbuilder.MARmqDatabase;
import gen.DocUtil;


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
    
    public MARmq(DocUtil util) {
        docUtil = util;
    }
    
    public DocUtil getDocUtil() {
        return docUtil;
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

    public static Destination getDestinationTopic(String topicName) {
        try {
            return session.createTopic(topicName);
        } catch (JMSException ex) {
            logger.error(ex);
            return null;
        }
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
            
            //создание Listener'a у mainTopic'а
            mainDestination = getDestinationTopic(xsdTopic);
            
            if (mainDestination != null) {
                try {
                    //MessageConsumer consumer = session.createConsumer(destination);
                    //consumer.setMessageListener(new Listener());
                    ActiveMQTopic topic = (ActiveMQTopic)mainDestination;
                    MessageConsumer consumer = session.createDurableSubscriber(
                            topic,
                            "mainSubFromPath("+projectPath+")");
                    consumer.setMessageListener(new XsdTopicListener(this, projectPath));
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
            
            //создание Listener'a у serviceTopic'а
            Destination serviceDestination = getDestinationTopic(serviceTopic);
            if (serviceDestination != null) {
                try {
                    ActiveMQTopic topic = (ActiveMQTopic)serviceDestination;
                    MessageConsumer consumer = session.createDurableSubscriber(
                            topic,
                            "serviceSubFromPath("+projectPath+")");
                    serviceListener = new ServiceTopicListener(this, projectPath);
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
        ReportTopicListener reportListener = new ReportTopicListener(this, projectPath);
        
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
                if (!netintName.contains("Wireless") &&  !netintName.contains("lan"))
                    continue;
                else
                    breakCycle = true;
                
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    Pattern ip = Pattern.compile("/[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
                    String inetAddr = inetAddress.toString();
                    
                    if (ip.matcher(inetAddr).matches()) {
                        ipv4 = inetAddr.substring(1, inetAddr.length());
                        logger.info("IP: " + ipv4);
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
    
    public void sendFile(String filePath, String rootElementName) throws IOException {
        if (Connected()) {
            mainDestination = getDestinationTopic(xsdTopic);
            
            if (mainDestination != null) {
                try {
                    MessageProducer producer = session.createProducer(mainDestination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    File fileToSending = new File(filePath);
                    InputStream is = new FileInputStream(fileToSending);
                    
                    long length = fileToSending.length();
                    if (length > Integer.MAX_VALUE) {
                        logger.debug("Файл очень большой");
                    }
                    
                    BytesMessage bytesMessage = session.createBytesMessage();
                    
                    //write ip & filename properties
                    String ip = getIp();
                    bytesMessage.setStringProperty("ip", ip);
                    bytesMessage.setStringProperty("scheme_name", rootElementName);
                    bytesMessage.setStringProperty("filename", fileToSending.getName());
                    
                    //MARmqDatabase.saveMapping("NOT_RECEIVE", ip, rootElementName, fileToSending.getName());
                    
                    byte[] bytes = new byte[(int)length];
                    int offset = 0;
                    int numRead = 0;
                    
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");

                        while (offset < bytes.length && numRead >= 0) {
                            numRead = is.read(bytes, offset, bytes.length - offset);

                            bytesMessage.writeBytes(bytes, offset, bytes.length - offset);
                            md.update(bytes, offset, bytes.length - offset);

                            offset += numRead;
                        }

                        if (offset < bytes.length) {
                            throw new IOException("Не удалось прочитать файл " + fileToSending.getName());
                        }

                        String md5file = new BigInteger(1, md.digest()).toString(16);
                        marDatabase.saveMessageId(md5file);
                        bytesMessage.setStringProperty("md5", md5file);
                        
                        logger.info("preparation to send message, ID: " + md5file);
                        logger.info(
                                String.format("properties: ip (%s), scheme_name (%s), filename (%s)",
                                ip, rootElementName, fileToSending.getName()));
                        
                    } catch (final NoSuchAlgorithmException e) {
                        logger.error(e.toString());
                    }

                    is.close();
                    
                    producer.send(bytesMessage);
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
        } else {
            logger.info("Соединение закрыто");
        }
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
        
        if (buildReport != null)
            buildReport.updateReport(fileName);
    }

}
