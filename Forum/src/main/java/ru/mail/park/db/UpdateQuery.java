package ru.mail.park.db;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Варя on 14.03.2017.
 */
public class UpdateQuery {

    public static int execute(String query) throws SQLException{
        Connection connection = DBConnect.getConnection();
        Statement statement = connection.createStatement();
        return statement.executeUpdate(query);

    }
}
