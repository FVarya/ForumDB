package ru.mail.park.db;

import java.sql.*;
import java.sql.Statement;

/**
 * Created by Варя on 14.03.2017.
 */
public class SelectQuery {

    public static <T>  T execute(String query, Result<T> result) throws SQLException{
        Connection connection = DBConnect.getConnection();
        Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(query);
        final T r = result.getResult(resultSet);
        return r;

    }
}
