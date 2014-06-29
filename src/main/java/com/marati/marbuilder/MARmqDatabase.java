package com.marati.marbuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import com.marati.marbuilder.JdbcConnects;
/**
 *
 * @author Марат
 */
public class MARmqDatabase {
    private static final java.sql.Connection sqliteCon = JdbcConnects.getSqliteConnection();
    private static Logger logger = Logger.getLogger(MARmqDatabase.class);
    
    public MARmqDatabase() {
        createMappingSchemeTable();
        createMessagesTable();
        createSourceMappingTable();
    }
    
    private void createMappingSchemeTable() {
        try {
            Statement createStatement = sqliteCon.createStatement();

            String createQuery = 
                    "CREATE TABLE scheme_mapping (" +
                    "id integer primary key autoincrement not null," +
                    "message_id text not null," +
                    "ip text not null," +
                    "scheme_name text not null," +
                    "file_name text not null)";
                    //"columns text not null)";
            
            createStatement.executeUpdate(createQuery);
            createStatement.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }               
    }
    
    private void createMessagesTable() {
        try {
            Statement createStatement = sqliteCon.createStatement();

            String createQuery = 
                    "CREATE TABLE messages (" +
                    "id integer primary key autoincrement not null," +
                    "message_id text not null)";

            createStatement.executeUpdate(createQuery);
            createStatement.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
    }
    
    private void createSourceMappingTable() {
        try {
            Statement createStatement = sqliteCon.createStatement();

            String createQuery = 
                    "CREATE TABLE source_mapping (" +
                    "id integer primary key autoincrement not null," +
                    "scheme_name text not null," +
                    "columns text not null," +
                    "destination_topic text not null)" ;
                    
            createStatement.executeUpdate(createQuery);
            createStatement.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
    }
    
    public static String getAttributeBySchemeName(String attribute, String schemeName) {
        String returnAttribute = null;
        
        try {
            String selectQuery = "SELECT " + attribute + " FROM scheme_mapping WHERE scheme_name = (?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(selectQuery);
            ps.setString(1, schemeName);
            
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                returnAttribute = rs.getString(attribute);
            }
            
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
        
        return returnAttribute;
    }
    
    //refact
    public static String getAttributeByFileName(String attribute, String fileName) {
        String returnAttribute = null;
        
        try {
            String selectQuery = "SELECT " + attribute + " FROM scheme_mapping WHERE file_name = (?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(selectQuery);
            ps.setString(1, fileName);
            
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                returnAttribute = rs.getString(attribute);
            }
            
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
        
        return returnAttribute;
    }
    
    public static void saveMapping(String messageId, String ip, String schemeName, String fileName) {
        try {
            String insertQuery = 
                    "INSERT INTO scheme_mapping (message_id, ip, scheme_name, file_name)" +
                    "VALUES (?, ?, ?, ?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(insertQuery);
            ps.setString(1, messageId);
            ps.setString(2, ip);
            ps.setString(3, schemeName);
            ps.setString(4, fileName);
            ps.executeUpdate();
            
            String loggerInfo = String.format("insert into scheme_mapping [%s, %s, %s, %s]",
                                              messageId, ip, schemeName, fileName);
            logger.info(loggerInfo);
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
    }
    
    public static void saveMessageId(String id) {
        try {
            String insertQuery = 
                    "INSERT INTO messages (message_id)" +
                    "VALUES (?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(insertQuery);
            ps.setString(1, id);
            ps.executeUpdate();
            
            String loggerInfo = String.format("insert into messages [%s]", id);
            logger.info(loggerInfo);
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        } 
    }
    
    public static void saveSourceMapping(String schemeName, String columns, String destinationTopic) {
        try {
            String insertQuery = 
                    "INSERT INTO source_mapping (scheme_name, columns, destination_topic)" +
                    "VALUES (?, ?, ?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(insertQuery);
            ps.setString(1, schemeName);
            ps.setString(2, columns);
            ps.setString(3, destinationTopic);
            ps.executeUpdate();
            
            String loggerInfo = String.format("insert into source_mapping [%s, %s, %s]",
                    schemeName, columns, destinationTopic);
            logger.info(loggerInfo);
            
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        } 
    }
    
    public static String getAttributeSourceByScheme(String attribute, String schemeName) {
        String returnAttribute = null;
        
        try {
            String selectQuery = 
                    String.format("SELECT %s FROM source_mapping WHERE scheme_name = (?)", attribute);
            
            PreparedStatement ps = sqliteCon.prepareStatement(selectQuery);
            ps.setString(1, schemeName);
            
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                returnAttribute = rs.getString(attribute);
            }
            
            ps.close();
            
        } catch (SQLException ex) {
            logger.info(ex);
        }  
        
        return returnAttribute;
    }
    
    public Boolean messageIdContains(String id) {
        Boolean contains = false;
        
        try {
            String insertQuery = "SELECT count(id) FROM messages WHERE message_id = (?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(insertQuery);
            ps.setString(1, id);
            
            ResultSet rs = ps.executeQuery();
            while ( rs.next() ) {
                int message_id = rs.getInt("count(id)");
                
                if (message_id != 0)
                    contains = true;
            }
            
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        } 
        
        return contains;
    }
}
