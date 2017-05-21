package ru.mail.park.Forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.Thread.Thread;
import ru.mail.park.User.User;
import ru.mail.park.User.UserService;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.PrepareQuery;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Варя on 14.03.2017.
 */

@SuppressWarnings("JDBCResourceOpenedButNotSafelyClosed")
@Service
@Transactional
public class ForumService extends DBConnect {

    @Autowired
    public ForumService(DataSource dataSource) {
        DBConnect.dataSource = dataSource;
    }


    public Forum getForumInfo(String slug) {
        try {
            return PrepareQuery.execute("SELECT slug, title, nickname FROM Forum " +
                            "JOIN FUser ON admin = nickname WHERE lower(slug) = lower(?) ",
                    prepareStatement -> {
                        prepareStatement.setString(1, slug);
                        final ResultSet resultSet = prepareStatement.executeQuery();
                        resultSet.next();
                        return new Forum(resultSet.getString(1),
                                resultSet.getString(2), resultSet.getString(3));
                    });
        } catch (SQLException n) {
            n.printStackTrace();
            return null;
        }
    }

    public Forum getFullForum(String slug) {
        final Forum forum;
        if ((forum = getForumInfo(slug)) == null) {
            return null;
        }
        try {
            return PrepareQuery.execute("SELECT COUNT(message_id), COUNT(DISTINCT thread_id) FROM Thread " +
                            "LEFT JOIN  Message USING(thread_id) WHERE forum = ? ",
                    prepareStatement -> {
                        prepareStatement.setString(1, slug);
                        final ResultSet resultSet = prepareStatement.executeQuery();
                        resultSet.next();
                        forum.setPosts(BigDecimal.valueOf(resultSet.getInt(1)));
                        forum.setThreads(BigDecimal.valueOf(resultSet.getInt(2)));
                        return forum;
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public ResponseEntity createForum(Forum body) {
        final User user;
        final UserService userService = new UserService(dataSource);
        if ((user = userService.getUserInfo(body.getAdmin())) == null)
            return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
        try {
            final Forum forum = getForumInfo(body.getSlug());
            if (forum == null) {
                return PrepareQuery.execute("INSERT INTO Forum VALUES ( ?,?,?)",
                        prepareStatement -> {
                            prepareStatement.setString(1, body.getSlug());
                            prepareStatement.setString(2, body.getTitle());
                            prepareStatement.setString(3, user.getNickname());
                            prepareStatement.executeUpdate();
                            final BigDecimal nul = new BigDecimal(0);
                            body.setPosts(nul);
                            body.setThreads(nul);
                            body.setAdmin(user.getNickname());
                            return new ResponseEntity(body.getForumJson(), HttpStatus.CREATED);
                        });
            }
            return new ResponseEntity(forum.getForumJson(), HttpStatus.CONFLICT);
        } catch (SQLException s) {
            return new ResponseEntity(Error.getErrorJson("Something go wrong"), HttpStatus.EXPECTATION_FAILED);
        }

    }


    public ResponseEntity userList(String slug, Double limit, String since, Boolean desc) {
        if (getForumInfo(slug) == null) {
            return new ResponseEntity(Error.getErrorJson("Forum not found"), HttpStatus.NOT_FOUND);
        }
        try {
            String strSince = "";
            if (since != null)
                if (desc)
                    strSince = " and U.nickname < '" + since + "' ";
                else strSince = " and U.nickname > '" + since + "' ";
            String strLimit = "";
            if (limit != null)
                strLimit = " LIMIT " + limit.intValue();
            String strSort = " ASC ";
            if (desc)
                strSort = " DESC ";
            return PrepareQuery.execute("SELECT DISTINCT U.* FROM Thread AS T " +
                            "LEFT JOIN Message  AS M ON M.thread_id = T.thread_id " +
                            "JOIN FUser AS U ON (U.nickname = M.author OR U.nickname = T.author) " +
                            " WHERE lower(T.forum) = lower(?) " + strSince + " ORDER BY U.nickname "
                            + strSort + strLimit,
                    preparedStatement -> {
                        preparedStatement.setString(1, slug);
                        final ResultSet result = preparedStatement.executeQuery();
                        final ObjectMapper mapp = new ObjectMapper();
                        final ArrayNode arrayNode = mapp.createArrayNode();
                        while (result.next()) {
                            final User user = new User(result.getString(1), result.getString(2)
                                    , result.getString(3), result.getString(4));
                            arrayNode.add(user.getUserJson());
                        }
                        return new ResponseEntity(arrayNode, HttpStatus.OK);
                    });
        } catch (SQLException n) {
            n.printStackTrace();
            final ObjectMapper mapp = new ObjectMapper();
            final ArrayNode arrayNode = mapp.createArrayNode();
            return new ResponseEntity(arrayNode, HttpStatus.OK);
        }
    }


    public ResponseEntity threadList(String slug, Double limit, String since, Boolean desc) {
        final Forum forum;
        if ((forum = getForumInfo(slug)) == null) {
            return new ResponseEntity(Error.getErrorJson("Forum not found"), HttpStatus.NOT_FOUND);
        }
        try {
            String strSince = "";
            if (since != null) {
                if (desc)
                    strSince = "  and create_date <= '" + since + '\'';
                else strSince = "  and create_date >= '" + since + '\'';
            }
            String strLimit = "";
            if (limit != null)
                strLimit = " LIMIT " + limit;
            String strSort = "ASC";
            if (desc)
                strSort = " DESC";
            return PrepareQuery.execute("SELECT * FROM Thread WHERE forum = ?"
                            + strSince + " ORDER BY create_date " + strSort + strLimit,
                    preparedStatement -> {
                        preparedStatement.setString(1, forum.getSlug());
                        final ResultSet resultSet = preparedStatement.executeQuery();
                        final ObjectMapper mapp = new ObjectMapper();
                        final ArrayNode arrayNode = mapp.createArrayNode();
                        while (resultSet.next()) {
                            final String s = resultSet.getString(3).replace(' ', 'T') + ":00";
                            final Thread thread = new Thread(resultSet.getString(2),
                                    resultSet.getString(1), resultSet.getString(5),
                                    resultSet.getString(4), s);
                            thread.setForum(forum.getSlug());
                            thread.setId(resultSet.getInt(7));
                            arrayNode.add(thread.getThreadJson());
                        }
                        return new ResponseEntity(arrayNode, HttpStatus.OK);
                    });
        } catch (SQLException n) {
            n.printStackTrace();
            final ObjectMapper mapp = new ObjectMapper();
            final ObjectNode node = mapp.createObjectNode();
            node.putNull("users");
            return new ResponseEntity(node, HttpStatus.OK);
        }
    }
}
