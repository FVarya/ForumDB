package ru.mail.park.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.Forum.Forum;
import ru.mail.park.Forum.ForumService;
import ru.mail.park.Thread.Thread;
import ru.mail.park.Thread.ThreadService;
import ru.mail.park.User.User;
import ru.mail.park.User.UserService;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.PrepareQuery;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Варя on 21.03.2017.
 */
@SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "MethodParameterNamingConvention", "LocalVariableNamingConvention"})
@Service
@Transactional
public class PostService extends DBConnect {
    private Double marker;
    private ZonedDateTime moment = ZonedDateTime.now();;

    @Autowired
    public PostService(DataSource dataSource) {
        DBConnect.dataSource = dataSource;
    }

    private Post getPost(BigDecimal id) throws SQLException{
        return PrepareQuery.execute("SELECT M.* FROM Message AS M " +
                        "WHERE message_id = ?",
                preparedStatement -> {
                    preparedStatement.setBigDecimal(1, id);
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    resultSet.next();
                    final String s = resultSet.getString(5).replace(' ', 'T') + ":00";
                    final Post post1 = new Post(resultSet.getInt(2), resultSet.getString(3),
                            resultSet.getString(4), s, resultSet.getBoolean(6),
                            resultSet.getBigDecimal(7));
                    post1.setId(resultSet.getBigDecimal(1));
                    post1.setForum(resultSet.getString(9));
                    //post1.setPath((Integer[])resultSet.getArray(8).getArray());
                    return post1;
                });
    }

    private ResponseEntity getPostsSort(String query, Integer id) throws SQLException{
        return PrepareQuery.execute(query,
                preparedStatement -> {
                    preparedStatement.setInt(1, id);
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    final ObjectMapper mapp = new ObjectMapper();
                    final ObjectNode node = mapp.createObjectNode();
                    final ArrayNode arrayNode = mapp.createArrayNode();
                    node.put("marker", "marker");
                    while (resultSet.next()) {
                        final String s = resultSet.getString(2).replace(' ', 'T') + ":00";
                        final Post post = new Post(resultSet.getInt(8),
                                resultSet.getString(6), resultSet.getString(1),
                                s, resultSet.getBoolean(5), resultSet.getBigDecimal(7));
                        post.setForum(resultSet.getString(3));
                        post.setId(resultSet.getBigDecimal(4));
                        arrayNode.add(post.getPostJson());
                    }
                    node.set("posts", arrayNode);
                    return new ResponseEntity(node, HttpStatus.OK);
                });
    }

    @SuppressWarnings("OverlyComplexMethod")
    public ResponseEntity getPostInfo(String slug, Integer id, Double limit, String mark, Boolean desc, String sort) {
        try {
            final ThreadService threadService = new ThreadService(dataSource);
            final Thread thread;
            if((thread = threadService.getThreadInfo(slug, id)) == null)
                throw new SQLException("Thread not found");
            String strSort = " ASC ";
            if (desc)
                strSort = " DESC ";
            String strMarker = "";
            if (mark != null) {
                strMarker = " OFFSET " + this.marker.intValue();
            }
            String strLimit = "";
            if (limit != null) {
                strLimit = " LIMIT " + limit.intValue();
                if (mark != null)
                    this.marker += limit;
                else this.marker = limit;
            }
            if (sort == null) sort = "flat";
            final String req = " M.thread_id = ?";
            switch (sort) {
                case "tree":
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id " +
                                    "FROM Message M " +
                                    "WHERE " + req +
                                    "ORDER BY M.path " + strSort + strLimit + strMarker,
                            thread.getId());
                case "parent_tree":
                    final String s = "M.thread_id = " + thread.getId();
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id " +
                                    "FROM Message M " +
                                    "WHERE M.path[1] IN ( " +
                                    "SELECT M.message_id FROM Message AS M " +
                                    "WHERE M.parent_id = 0 AND " + s +
                                    " ORDER BY M.message_id " + strSort + strLimit + strMarker +
                                    ") AND " + req +
                                    " ORDER BY M.path " + strSort,
                            thread.getId());
                default:
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id " +
                                    " FROM Message AS M  " +
                                    " WHERE " + req + " ORDER BY M.create_date, M.message_id " + strSort + strLimit + strMarker,
                            thread.getId());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Forum not Found"), HttpStatus.NOT_FOUND);
        }
    }

    public void addForumUsers(String forum,Post[] posts) throws SQLException {
        PrepareQuery.execute("INSERT INTO Forum_users (nickname, forum) " +
                        "VALUES (?,?)",
                preparedStatement -> {
                    for (Post post : posts) {
                        preparedStatement.setString(1, post.getAuthor());
                        preparedStatement.setString(2, forum);
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                    return null;
                });
    }

    private void updForumMessgs(String slug, int num) throws SQLException {
        PrepareQuery.execute("UPDATE Forum SET messages = messages + ?" +
                        " WHERE lower(slug) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(2, slug);
                    preparedStatement.setInt(1, num);
                    preparedStatement.executeUpdate();
                    return null;
                });
    }

    private List<Post> checkParents(Post[] body, Thread thread) throws SQLException {
        final BigDecimal[] unique =
                Arrays.stream(body).map(Post::getParent).distinct().toArray(BigDecimal[]::new);
        final List<Post> parents = new ArrayList<>();
        for(BigDecimal parent: unique){
            if (parent.intValue() != 0)
                parents.add(getParent(parent, thread));
        }
        return parents;
    }

    public ArrayNode createPosts(String thread_slug, Integer thread_id, Post[] body) throws SQLException{
        final ObjectMapper mapp = new ObjectMapper();
        final ArrayNode arrayNode = mapp.createArrayNode();
        final ZonedDateTime postTime;
        if(ZonedDateTime.now().getLong(ChronoField.INSTANT_SECONDS) -
                moment.getLong(ChronoField.INSTANT_SECONDS) <= 120)
            postTime = moment;
        else postTime = ZonedDateTime.now();
        moment = postTime;

        final ThreadService threadService = new ThreadService(dataSource);
        final Thread thread;
        if((thread = threadService.getThreadInfo(thread_slug, thread_id)) == null)
            throw new SQLException("Not found");

        final UserService userService = new UserService(dataSource);
        if(userService.getUserInfo(body[0].getAuthor()) == null)
            throw new SQLException("Not found");

        final List<Post> parents = checkParents(body, thread);
        return PrepareQuery.execute("INSERT INTO Message (thread_id, message, author, create_date, parent_id," +
                        " path, forum ) " +
                        "VALUES (?,?,?,?,?,array_append(?, " +
                        "currval('message_message_id_seq')::INT8), ?) ", Statement.RETURN_GENERATED_KEYS,
                preparedStatement -> {
                    for (Post post: body) {
                        preparedStatement.setInt(1, thread.getId());
                        preparedStatement.setString(2, post.getMessage());
                        preparedStatement.setString(3, post.getAuthor());
                        preparedStatement.setBigDecimal(5, post.getParent());
                        for(Post parent: parents){
                            if(parent.getId().equals(post.getParent())) {
                                preparedStatement.setArray(6,
                                        getConnection().createArrayOf("int8", parent.getPath()));
                                break;
                            }
                        }
                        if(post.getParent().equals(BigDecimal.valueOf(0)))
                            preparedStatement.setArray(6,null);
                        preparedStatement.setString(7, thread.getForum());
                        if (post.getCreated() == null) {
                            post.setCreated(postTime);
                            final Timestamp t = new Timestamp(post.getCreated().getLong(ChronoField.INSTANT_SECONDS) * 1000
                                    + post.getCreated().getLong(ChronoField.MILLI_OF_SECOND));
                            preparedStatement.setTimestamp(4, t);
                        } else {
                            final Timestamp t = Timestamp.valueOf(post.getCreated().toLocalDateTime());
                            preparedStatement.setTimestamp(4, t);
                        }
                        preparedStatement.addBatch();
                        post.setForum(thread.getForum());
                        post.setThread_id(thread.getId());
                    }
                    final ArrayNode an = getJsonNodes(body, arrayNode, preparedStatement);
                    addForumUsers(thread.getForum(), body);
                    updForumMessgs(thread.getForum(), body.length);
                    return an;
                });
    }

    private ArrayNode getJsonNodes(Post[] body, ArrayNode arrayNode,
                                   PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.executeBatch();
        final ResultSet rs = preparedStatement.getGeneratedKeys();
        int i = 0;
        while (rs.next()) {
            final BigDecimal id = rs.getBigDecimal(1);
            body[i].setId(id);
            arrayNode.add(body[i].getPostJson());
            i++;
        }
        return arrayNode;
    }


    private Post getParent(BigDecimal parent, Thread thread) throws SQLException {
        return PrepareQuery.execute("SELECT * FROM Message " +
                        "WHERE message_id = ? and thread_id = ?",
                preparedStatement -> {
                    preparedStatement.setBigDecimal(1, parent);
                    preparedStatement.setInt(2, thread.getId());
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    if (!resultSet.next())
                        throw new SQLException("Message not found");
                    final String s = resultSet.getString(5).replace(' ', 'T') + ":00";
                    final Post post = new Post(resultSet.getInt(2), resultSet.getString(3),
                            resultSet.getString(4), s, resultSet.getBoolean(6),
                            resultSet.getBigDecimal(7));
                    post.setId(resultSet.getBigDecimal(1));
                    post.setForum(resultSet.getString(9));
                    post.setPath((Integer[])resultSet.getArray(8).getArray());
                    return post;
                });
    }

    @SuppressWarnings("OverlyComplexMethod")
    public ResponseEntity getPostRelated(BigDecimal id, String[] related) {
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();
        boolean user = false;
        boolean forum = false;
        boolean thread = false;
        if(related != null) {
            for (String value : related) {
                if (!user && value.equals("user")) {
                    user = true;
                }
                if (!forum && value.equals("forum")) {
                    forum = true;
                }
                if (!thread && value.equals("thread")) {
                    thread = true;
                }
            }
        }
        try {
            final Post post = getPost(id);
            node.set("post", post.getPostJson());
            if (user) {
                final UserService userService = new UserService(dataSource);
                final User user1 = userService.getUserInfo(post.getAuthor());
                node.set("author", user1.getUserJson());
            }
            if (forum) {
                final ForumService forumService = new ForumService(dataSource);
                final Forum forum1 = forumService.getFullForum(post.getForum());
                node.set("forum", forum1.getForumJson());

            }
            if (thread) {
                final ThreadService threadService = new ThreadService(dataSource);
                final Thread thread1 = threadService.getFullThread(null, post.getThread_id());
                node.set("thread", thread1.getThreadJson());
            }
            return new ResponseEntity(node, HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity setPost(BigDecimal id, Post body) {
        try {
            final Post post = getPost(id);
            if (body.getMessage() != null && !post.getMessage().equals(body.getMessage())) {
                return PrepareQuery.execute("UPDATE Message SET (message, is_edit) = (?, true) " +
                                "WHERE message_id = ?",
                        preparedStatement -> {
                            preparedStatement.setString(1, body.getMessage());
                            preparedStatement.setBigDecimal(2, id);
                            preparedStatement.executeUpdate();
                            post.setMessage(body.getMessage());
                            post.setIs_edit();
                            return new ResponseEntity(post.getPostJson(), HttpStatus.OK);
                        });
            }
            return new ResponseEntity(post.getPostJson(), HttpStatus.OK);
        } catch (SQLException n) {
            n.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
        }
    }
}
