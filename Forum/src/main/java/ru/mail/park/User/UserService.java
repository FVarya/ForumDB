package ru.mail.park.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.db.*;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ru.mail.park.Error.Error;
import javax.sql.DataSource;



/**
 * Created by Варя on 11.03.2017.
 */

@SuppressWarnings("JDBCResourceOpenedButNotSafelyClosed")
@Service
@Transactional
public class UserService extends DBConnect{

    @Autowired
    public UserService(DataSource dataSource){
        DBConnect.dataSource = dataSource;
    }

    public ResponseEntity createUser(User body){
        try {
            return PrepareQuery.execute("SELECT * FROM FUser AS U " +
                            "WHERE lower(U.email) = lower(?) OR lower(nickname) = lower(?)",
                    preparedStatement -> {
                        preparedStatement.setString(1, body.getEmail());
                        preparedStatement.setString(2, body.getNickname());
                        final ResultSet resultSet = preparedStatement.executeQuery();
                        if(!resultSet.next()){
                            throw new SQLException("New User");
                        }

                        final ObjectMapper mapp = new ObjectMapper();
                        final ArrayNode arrayNode = mapp.createArrayNode();
                        do {
                            final User user = new User(resultSet.getString(1), resultSet.getString(2)
                                    , resultSet.getString(3), resultSet.getString(4));
                            arrayNode.add(user.getUserJson());
                        } while (resultSet.next());
                        return new ResponseEntity(arrayNode, HttpStatus.CONFLICT);
                    });
        }
        catch (SQLException e){
            try {
                return PrepareQuery.execute("INSERT INTO FUser VALUES ( ?,?,?,?) ",
                        preparedStatement -> {
                            preparedStatement.setString(1, body.getNickname());
                            preparedStatement.setString(2, body.getEmail());
                            preparedStatement.setString(3, body.getFullname());
                            preparedStatement.setString(4,body.getAbout());
                            preparedStatement.executeUpdate();
                            return new ResponseEntity(body.getUserJson(), HttpStatus.CREATED);
                        });
            }
            catch (SQLException n){
                return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
            }
        }
    }


    public User getUserInfo (String login){
        try{
            return PrepareQuery.execute("SELECT * FROM FUser WHERE " +
                            "lower(nickname) = lower(?)",
                    prepareStatement -> {
                        prepareStatement.setString(1, login);
                        final ResultSet result = prepareStatement.executeQuery();
                        result.next();
                        return new User(result.getString(1),result.getString(2),
                                result.getString(3), result.getString(4));
                    });
        }
        catch (SQLException e){
            return null;
        }
    }

    public ResponseEntity  changeUserInfo(String login, User body){
        final User user;
        if((user = getUserInfo(login)) == null)
            return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
        if(user.getAbout().equals(body.getAbout()) &&
                user.getEmail().equals(body.getEmail()) &&
                user.getFullname().equals(body.getFullname()))
            return new ResponseEntity(user.getUserJson(), HttpStatus.OK);
        try {
            return PrepareQuery.execute("UPDATE FUser SET (about, email,fullname) = (?,?,?) " +
                            "WHERE lower(nickname) = lower(?)",
                    ps2 ->{
                        if(body.getAbout() == null) {
                            ps2.setString(1, user.getAbout());
                            body.setAbout(user.getAbout());
                        }
                        else ps2.setString(1, body.getAbout());
                        if(body.getEmail() == null){
                            ps2.setString(2, user.getEmail());
                            body.setEmail(user.getEmail());
                        }
                        else ps2.setString(2, body.getEmail());
                        if(body.getFullname() == null){
                            ps2.setString(3, user.getFullname());
                            body.setFullname(user.getFullname());
                        }
                        else ps2.setString(3, body.getFullname());
                        ps2.setString(4, login);
                        ps2.executeUpdate();
                        body.setNickname(user.getNickname());
                        return new ResponseEntity(body.getUserJson(), HttpStatus.OK);
                    });
        } catch (SQLException n) {
            return new ResponseEntity(Error.getErrorJson("Conflict data"), HttpStatus.CONFLICT);
        }
    }
}
