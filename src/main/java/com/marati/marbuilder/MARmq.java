package com.marati.marbuilder;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.log4j.Logger;

//import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;
import java.math.BigInteger;

//import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import java.io.BufferedReader;
//import java.io.InputStreamReader;

import nu.xom.ParsingException;
import gen.JTableGen;

import com.marati.marbuilder.JdbcConnects;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;


/**
 *
 * @author marat
 */
public class MARmq {
    
    private static ActiveMQConnectionFactory connectionFactory = null;
    private static Connection connection = null; 
    private static Session session;
    private static Destination destination; 
    private static Logger logger = Logger.getLogger(MARmq.class);
    
    private final static String topic = "MARtopic";
    private final JTableGen tablesGen;
    private static String projectPath = null;
    private static ArrayList<String> messageIds;
    private static final java.sql.Connection sqliteCon = JdbcConnects.getSqliteConnection();
    
    public MARmq(JTableGen tablesGenner) {
        tablesGen = tablesGenner;
        
        createMappingScheme();
    }
    
    private void createMappingScheme() {
        try {
            Statement createStatement = sqliteCon.createStatement();

            String createQuery = 
                    "CREATE TABLE scheme_mapping (" +
                    "id int primary key not null," +
                    "ip text not null," +
                    "scheme_name text not null," +
                    "file_name text not null," +
                    "columns text not null)";
            
            createStatement.executeUpdate(createQuery);
            createStatement.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }
                        
    }
    
    public void updateProjectPath(String path) {
        projectPath = path;
        messageIds = new ArrayList<String>();
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
            }
            
            return true;
        } catch (JMSException ex) {
            logger.error(ex);
            return false;
        }
    }
    
    private static Destination getDestinationTopic() {
        try {
            return session.createTopic(topic);
        } catch (JMSException ex) {
            logger.error(ex);
            return null;
        }
    }
    
    private static String getIp() throws IOException {
        URL anyUrl = new URL("http://mgupi.ru");
        BufferedReader in = null;
        
        try {
            in = new BufferedReader(new InputStreamReader(
                    anyUrl.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }
    
    public void sendFile(String filePath, String rootElementName) throws IOException {
        if (Connected()) {
            destination = getDestinationTopic();
            
            if (destination != null) {
                try {
                    MessageProducer producer = session.createProducer(destination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    File fileToSending = new File(filePath);
                    InputStream is = new FileInputStream(fileToSending);
                    
                    long length = fileToSending.length();
                    if (length > Integer.MAX_VALUE) {
                        logger.debug("Файл очень большой");
                    }
                    
                    BytesMessage bytesMessage = session.createBytesMessage();
                    
                    //write ip & filename properties
                    bytesMessage.setStringProperty("ip", getIp());
                    bytesMessage.setStringProperty("scheme_name", rootElementName);
                    bytesMessage.setStringProperty("filename", fileToSending.getName());
                    
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
                        messageIds.add(md5file);
                        bytesMessage.setStringProperty("md5", md5file);
                        System.out.println("send bytes, messageIds: " + messageIds.toString());
                        
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
    
//    public void receiveOldMessages() {
//        
//    }
    
    public void activateReceiver() {
        if (Connected()) {
            destination = getDestinationTopic();
            
            if (destination != null) {
                try {
                    //MessageConsumer consumer = session.createConsumer(destination);
                    //consumer.setMessageListener(new Listener());
                    ActiveMQTopic topic = (ActiveMQTopic)destination;
                    MessageConsumer consumer = session.createDurableSubscriber(topic, "subFromPath("+projectPath+")");
                    consumer.setMessageListener(new Listener());
                } catch (JMSException ex) {
                    logger.error(ex);
                }
            }
        } else {
            logger.info("Соединение закрыто");
        }
    }
    
    public String getReceiveIds() {
        //return messageIds.toString();
        ByteArrayOutputStream messagesBuffer = new ByteArrayOutputStream();
        
        try {
            ObjectOutputStream outputMessages = new ObjectOutputStream(messagesBuffer);
            outputMessages.writeObject(messageIds);
            outputMessages.close();
            
            //messagesStream
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
    }
    
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                logger.info(ex);
            }
        }
    }


    public class Listener implements MessageListener {
        public void onMessage(Message msg) {
            BytesMessage bytesMessage = (BytesMessage)msg;

            try {
                String ip = msg.getStringProperty("ip");
                String fileName = msg.getStringProperty("filename");
                String schemeName = msg.getStringProperty("scheme_name");
                
                logger.info("receive message: [IP " + ip + "], " +
                            "[scheme name " + schemeName + "], " +
                            "[file name " + fileName + "], " +
                            "[ID " + msg.getJMSMessageID() + "]");
                
                //своё сообщение не принимаем
                if (messageIds.contains(msg.getStringProperty("md5")))
                    return;
                
                byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);

                int dotPos = fileName.lastIndexOf(".");
                
                String xsdDir = null;
                String extention = fileName.substring(dotPos);
                if (extention.equals(".xsd")) {
                    xsdDir = new String(projectPath + File.separator + "xsd");
                } else {
                    logger.error("fail parse file: " + extention);
                    return;
                }

                File receivedFile = new File(xsdDir + File.separator + fileName);

                FileOutputStream fos = new FileOutputStream(receivedFile);
                try {
                    fos.write(bytes);
                    logger.info("write in file " + receivedFile.getAbsolutePath());
                    fos.close();
                } catch (IOException ex) {
                    logger.error(ex.toString());
                }

                HashMap<String, ArrayList<String>> dirsAndTheirFiles = new HashMap<String, ArrayList<String>>();
                ArrayList<String> filesNameWithoutExt = new ArrayList<String>();
                
                filesNameWithoutExt.add( fileName.substring(0, dotPos) );
                
                dirsAndTheirFiles.put(xsdDir, filesNameWithoutExt);
                
                tablesGen.createTablesFromXsd(dirsAndTheirFiles);

            } catch (JMSException ex) {
                logger.error(ex);
            } catch (FileNotFoundException ex) {
                logger.error(ex);
            } catch (ParsingException ex) {
                logger.error(ex);
            } catch (IOException ex) {
                logger.error(ex);
            }
        }
    }

}
