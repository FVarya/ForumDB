package ru.mail.park.Forum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.Thread.Thread;
import ru.mail.park.User.User;
import ru.mail.park.User.UserService;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.PrepareQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Варя on 14.03.2017.
 */

@SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "ConstantNamingConvention"})
@Service
@Transactional
public class ForumService extends DBConnect {
    JdbcTemplate template;

    @Autowired
    public ForumService(DataSource dataSource, JdbcTemplate template) {
        DBConnect.dataSource = dataSource;
        this.template = template;
    }

    public ForumService(DataSource dataSource) {
        DBConnect.dataSource = dataSource;
    }


    private static final RowMapper<Forum> forumMapper = (resultSet, rowNum) -> {
        final Forum forum = new Forum(resultSet.getString(1),
                resultSet.getString(2), resultSet.getString(3));
        forum.setPosts(resultSet.getInt(4));
        forum.setThreads(resultSet.getInt(5));
        return forum;
    };

    public Forum getForumInfo(String slug) {
        try{
            final String sql = String.format("SELECT slug, title, admin, messages, threads FROM Forum WHERE lower(slug) = lower('%s')", slug);
            return template.queryForObject(sql, forumMapper);
        }
        catch (DataAccessException ignored){}
        return null;
    }


    public ResponseEntity createForum(Forum body) {
        final User user;
        final UserService userService = new UserService(dataSource);
        if ((user = userService.getUserInfo(body.getAdmin())) == null)
            return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
        final Forum forum;
        if ((forum = getForumInfo(body.getSlug())) != null) {
            return new ResponseEntity(forum.getForumJson(), HttpStatus.CONFLICT);
        }
        try {
            return PrepareQuery.execute("INSERT INTO Forum VALUES ( ?,?,?)",
                    prepareStatement -> {
                        prepareStatement.setString(1, body.getSlug());
                        prepareStatement.setString(2, body.getTitle());
                        prepareStatement.setString(3, user.getNickname());
                        prepareStatement.executeUpdate();
                        body.setAdmin(user.getNickname());
                        return new ResponseEntity(body.getForumJson(), HttpStatus.CREATED);
                    });
        } catch (SQLException s) {
            return new ResponseEntity(Error.getErrorJson("Something go wrong"), HttpStatus.EXPECTATION_FAILED);
        }

    }


    private static final RowMapper<User> userMapper = (result, rowNum) -> new User(result.getString(1), result.getString(2),
            result.getString(3),result.getString(4));

    static final String getUserListSQL = "SELECT * FROM FUser WHERE nickname IN (SELECT nickname FROM Forum_users WHERE LOWER(forum) = LOWER(?))";
    public List<User> userList(String slug, Integer limit, String since, Boolean desc) {
        final StringBuilder sb = new StringBuilder(getUserListSQL);
        final List<Object> args = new ArrayList<>();
        args.add(slug);
        if (since != null) {
            if (desc)
                sb.append(" and LOWER(nickname COLLATE \"ucs_basic\") < LOWER(? COLLATE \"ucs_basic\") ");
            else sb.append(" and LOWER(nickname COLLATE \"ucs_basic\") > LOWER(? COLLATE \"ucs_basic\") ");
            args.add(since);
        }
        if(!desc) sb.append(" ORDER BY LOWER(nickname COLLATE \"ucs_basic\") ASC ");
        else sb.append("  ORDER BY LOWER(nickname COLLATE \"ucs_basic\") DESC ");
        if (limit != null) {
            sb.append(" LIMIT ");
            sb.append(limit);
        }
        return template.query(sb.toString(), userMapper, args.toArray());

    }


    public ResponseEntity threadList(String slug, Integer limit, String since, Boolean desc) {
        final Forum forum;
        if ((forum = getForumInfo(slug)) == null) {
            return new ResponseEntity(Error.getErrorJson("Forum not found"), HttpStatus.NOT_FOUND);
        }
        try {
            String strSince = "";
            if (since != null) {
                if (desc)
                    strSince = String.format(" AND create_date <= '%s'", since);
                else strSince = String.format(" AND create_date >= '%s'", since);
            }
            String strLimit = "";
            if (limit != null)
                strLimit = String.format(" LIMIT '%s'; ", limit);
            String strSort = " ORDER BY create_date ";
            if (desc)
                strSort = " ORDER BY create_date DESC ";
            return PrepareQuery.execute("SELECT * FROM Thread WHERE lower(forum) = lower(?)"
                            + strSince + strSort + strLimit,
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
                            thread.setVotes(resultSet.getInt(8));
                            arrayNode.add(thread.getThreadJson());
                        }
                        return new ResponseEntity(arrayNode, HttpStatus.OK);
                    });
        } catch (SQLException n) {
            final ObjectMapper mapp = new ObjectMapper();
            final ObjectNode node = mapp.createObjectNode();
            node.putNull("users");
            return new ResponseEntity(node, HttpStatus.OK);
        }
    }

}
