package ru.mail.park.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Варя on 14.03.2017.
 */
public interface Result<T> {
    T getResult(ResultSet resultSet) throws SQLException;
}
