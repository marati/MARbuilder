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
    }
    
    private void createMappingSchemeTable() {
        try {
            Statement createStatement = sqliteCon.createStatement();

            String createQuery = 
                    "CREATE TABLE scheme_mapping (" +
                    "id integer primary key autoincrement not null," +
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
    
    public void saveMapping(String ip, String schemeName, String fileName) {
        try {
            String insertQuery = 
                    "INSERT INTO scheme_mapping (ip, scheme_name, file_name)" +
                    "VALUES (?, ?, ?)";
            
            PreparedStatement ps = sqliteCon.prepareStatement(insertQuery);
            ps.setString(1, ip);
            ps.setString(2, schemeName);
            ps.setString(3, fileName);
            ps.executeUpdate();
            
            String loggerInfo = String.format("insert into scheme_mapping [%s, %s, %s]",
                                              ip, schemeName, fileName);
            logger.info(loggerInfo);
            ps.close();
        } catch (SQLException ex) {
            logger.info(ex);
        }  
    }
    
    public void saveMessageId(String id) {
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
