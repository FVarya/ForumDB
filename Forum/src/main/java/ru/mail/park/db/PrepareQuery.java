package ru.mail.park.db;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.sql.Statement;

/**
 * Created by Варя on 14.03.2017.
 */
public class PrepareQuery {


    public static <T> T execute(String query, PrepStatement<T> st) throws SQLException {
        Connection connection = DBConnect.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            final T result = st.getStatement(preparedStatement);
            return result;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
}

