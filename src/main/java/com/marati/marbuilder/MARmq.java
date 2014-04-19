package com.marati.marbuilder;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.awt.event.*;
import java.io.*;
import java.util.logging.*;

import com.marati.marbuilder.MarForm;


/**
 *
 * @author marat
 */
public class MARmq {
    
    //private MarForm mainForm;
    
    private static ActiveMQConnectionFactory connectionFactory = null;
    private static Connection connection = null; 
    private static Session session;
    private static Destination destination; 
    private final static String topic = "MARtopic";
    private String projectPath;
    
    public MARmq(String path) {
        projectPath = path;
        //activateReceiver();
    }
    
    private static ActiveMQConnectionFactory getConnectionFactory(){
        return new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, 
                                             ActiveMQConnection.DEFAULT_PASSWORD, 
                                             "failover://tcp://localhost:61616");
    }
    
    public static Boolean Connected() {
        try {
            if (connection == null) {
                connectionFactory = getConnectionFactory();
                connection = connectionFactory.createConnection();
                connection.start();
                
                //сессия без транзакций
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            } else {
                connection.start();
            }
            
            return true;
        } catch (JMSException ex) {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private static Destination getDestinationTopic() {
        try {
            return session.createTopic(topic);
        } catch (JMSException ex) {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public void sendFile(String filePath) throws IOException {
        if (Connected()) {
            destination = getDestinationTopic();
            
            if (destination != null) {
                try {
                    MessageProducer producer = session.createProducer(destination);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    
                    BytesMessage bytesMessage = session.createBytesMessage();
                    
                    //TextMessage message = session.createTextMessage("test MSG");
                    File fileToSending = new File(filePath);
                    InputStream is = new FileInputStream(fileToSending);
                    
                    long length = fileToSending.length();
                    if (length > Integer.MAX_VALUE) {
                        Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, "Файл очень большой");
                    }
                    
                    byte[] bytes = new byte[(int)length];

                    int offset = 0;
                    int numRead = 0;
                    while (offset < bytes.length && numRead >= 0) {
                        numRead = is.read(bytes, offset, bytes.length - offset);
                        bytesMessage.writeBytes(bytes, offset, bytes.length - offset);
                        offset += numRead;
                    }

                    if (offset < bytes.length) {
                        throw new IOException("Не удалось прочитать файл " + fileToSending.getName());
                    }

                    is.close();
                    
                    producer.send(bytesMessage);
                } catch (JMSException ex) {
                    Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, "Соединение закрыто");
        }
    }
    
    public void activateReceiver() {
        //вызывать этот метод из конструктора
        if (Connected()) {
            destination = getDestinationTopic();
            
            if (destination != null) {
                try {
                    MessageConsumer consumer = session.createConsumer(destination);
                    consumer.setMessageListener(new MessageListener() {
                    public void onMessage(Message msg) {
                        BytesMessage bytesMessage = (BytesMessage)msg;
                        try {
                            byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
                            bytesMessage.readBytes(bytes);
                            
                            File receivedFile = new File(projectPath + File.separator + "ex.xsd");
                            
                            FileOutputStream fos = new FileOutputStream(receivedFile);
                            try {
                                fos.write(bytes);
                                System.out.println("write in file from MARmq");
                                fos.close();
                            } catch (IOException ex) {
                                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            //подумать над тем, как его назвать, 
                        } catch (JMSException ex) {
                            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    });
                } catch (JMSException ex) {
                    Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, "Соединение закрыто");
        }
    }
    
    public void updateProjectPath(String path) {
        projectPath = path;
    }
    
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
