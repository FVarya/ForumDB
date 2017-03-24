package ru.mail.park.db;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.*;

/**
 * Created by Варя on 11.03.2017.
 */

public abstract class DBConnect {

    protected static DataSource dataSource;


    /*public static Statement getStatement(){
        Statement statement = null;
        try {
            statement = connection.createStatement();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return statement;
    }

    public static PreparedStatement getPreparedStatement(String query){
        PreparedStatement preparedStatement = null;
        try {
            Connection c = ds.getConnection();
            preparedStatement = c.prepareStatement(query);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return preparedStatement;
    }*/

    public static Connection getConnection(){
        return DataSourceUtils.getConnection(dataSource);
    }

}
