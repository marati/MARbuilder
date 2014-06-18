package com.marati.marbuilder;

import java.sql.*;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 *
 * @author Марат
 */
public class JdbcConnects {
    private static Connection sqliteCon = null;
    private static Logger logger = Logger.getLogger(JdbcConnects.class);
    
    public static Connection getSqliteConnection() {
        if (sqliteCon != null) {
            return sqliteCon;
        } else {
            try {
                String driver = "org.sqlite.JDBC";
                Class.forName(driver);
                
                sqliteCon = DriverManager.getConnection("jdbc:sqlite:mapping.db");
            } catch (Exception e) {
                logger.error(e);
            }
            
            return sqliteCon;
        }
    }
}
