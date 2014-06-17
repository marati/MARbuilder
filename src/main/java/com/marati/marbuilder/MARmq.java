package com.marati.marbuilder;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;

import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.*;
import java.math.BigInteger;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.marati.marbuilder.MarForm;
import gen.JTableGen;
import java.util.HashMap;
import nu.xom.ParsingException;


/**
 *
 * @author marat
 */
public class MARmq {
    
    private static ActiveMQConnectionFactory connectionFactory = null;
    private static Connection connection = null; 
    private static Session session;
    private static Destination destination; 
    private final static String topic = "MARtopic";
    private final JTableGen tablesGen;
    private static String projectPath = null;
    private static ArrayList<String> messageIds;
    
    public MARmq(JTableGen tablesGenner) {
        tablesGen = tablesGenner;
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
                
                ActiveMQTopic topic = (ActiveMQTopic)getDestinationTopic();

                MessageConsumer consumer = session.createDurableSubscriber(topic, "subFromPath("+projectPath+")");
                consumer.setMessageListener(new Listener());
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
                    
                    File fileToSending = new File(filePath);
                    InputStream is = new FileInputStream(fileToSending);
                    
                    long length = fileToSending.length();
                    if (length > Integer.MAX_VALUE) {
                        Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, "Файл очень большой");
                    }
                    
                    BytesMessage bytesMessage = session.createBytesMessage();
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
                        Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, e.toString());
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
    
//    public void receiveOldMessages() {
//        
//    }
    
    public void activateReceiver() {
        if (Connected()) {
            destination = getDestinationTopic();
            
            if (destination != null) {
                try {
                    MessageConsumer consumer = session.createConsumer(destination);
                    consumer.setMessageListener(new Listener());
                } catch (JMSException ex) {
                    Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, "Соединение закрыто");
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
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            inputMessages.close();
            messagesBuffer.close();
        } catch (IOException ex) {
            Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                inputMessages.close();
            } catch (IOException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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


    public class Listener implements MessageListener {
        public void onMessage(Message msg) {
            BytesMessage bytesMessage = (BytesMessage)msg;

            try {
                System.out.println("msg receive " + 
                        msg.getStringProperty("filename") + " " + msg.getJMSMessageID());
                //своё сообщение не принимаем
                if (messageIds.contains(msg.getStringProperty("md5")))
                    return;
                
                byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);
                
                String fileName = new String(msg.getStringProperty("filename"));
                int dotPos = fileName.lastIndexOf(".");
                
                String xsdDir = null;
                String extention = fileName.substring(dotPos);
                if (extention.equals("xsd")) {
                    xsdDir = new String(projectPath + File.separator + "xsd");
                } else {
                    Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, "fail parse file: " + extention);
                    return;
                }

                

                File receivedFile = new File(xsdDir + fileName);

                FileOutputStream fos = new FileOutputStream(receivedFile);
                try {
                    fos.write(bytes);
                    System.out.println("write in file from MARmq");
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
                }

                HashMap<String, ArrayList<String>> dirsAndTheirFiles = new HashMap<String, ArrayList<String>>();
                ArrayList<String> filesNameWithoutExt = new ArrayList<String>();
                
                filesNameWithoutExt.add( fileName.substring(0, dotPos) );
                
                dirsAndTheirFiles.put(xsdDir, filesNameWithoutExt);
                
                tablesGen.createTablesFromXsd(dirsAndTheirFiles);

            } catch (JMSException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParsingException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MARmq.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
