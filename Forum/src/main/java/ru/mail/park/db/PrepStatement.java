package ru.mail.park.db;

import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Варя on 14.03.2017.
 */
public interface PrepStatement<T> {

    T getStatement(PreparedStatement preparedStatement) throws SQLException, InvocationTargetException, IllegalAccessException;
}
