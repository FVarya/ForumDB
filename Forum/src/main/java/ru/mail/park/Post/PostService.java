package ru.mail.park.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
import ru.mail.park.db.SelectQuery;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Created by Варя on 21.03.2017.
 */
@SuppressWarnings({"JDBCResourceOpenedButNotSafelyClosed", "MethodParameterNamingConvention", "LocalVariableNamingConvention"})
@Service
@Transactional
public class PostService extends DBConnect {
    private Double marker;
    private JdbcTemplate template;

    //@Autowired
    public PostService(DataSource dataSource) {
        DBConnect.dataSource = dataSource;
    }

    @Autowired
    public PostService(DataSource dataSource, JdbcTemplate template) {
        DBConnect.dataSource = dataSource;
        this.template = template;
    }

    private static final RowMapper<Post> postMapper = (rs, num) -> {
        final String s = rs.getString(5).replace(' ', 'T') + ":00";
        Post post = new Post(rs.getInt(2), rs.getString(3),
                rs.getString(4), s, rs.getBoolean(6),
                rs.getInt(7));
        post.setId(rs.getInt(1));
        post.setForum(rs.getString(9));
        return post;
    };

//    private Post getPost(Integer id) throws SQLException{
//        return PrepareQuery.execute("SELECT * FROM Message " +
//                        "WHERE message_id = ?",
//                preparedStatement -> {
//                    preparedStatement.setInt(1, id);
//                    final ResultSet resultSet = preparedStatement.executeQuery();
//                    resultSet.next();
//                    final String s = resultSet.getString(5).replace(' ', 'T') + ":00";
//                    final Post post1 = new Post(resultSet.getInt(2), resultSet.getString(3),
//                            resultSet.getString(4), s, resultSet.getBoolean(6),
//                            resultSet.getInt(7));
//                    post1.setId(resultSet.getInt(1));
//                    post1.setForum(resultSet.getString(9));
//                    return post1;
//                });
//    }

    public Post getPost(int id){
        try{
            final String sql = "SELECT * FROM message WHERE message_id = ?;";
            return template.queryForObject(sql, postMapper, id);
        } catch (Exception e){
            //e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getPostsSort(String query, Integer id, Integer limit, Integer offset, boolean flag) throws SQLException{
        return PrepareQuery.execute(query,
                preparedStatement -> {
                    preparedStatement.setInt(1, id);
                    preparedStatement.setInt(2, limit);
                    preparedStatement.setInt(3, offset);
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    final ObjectMapper mapp = new ObjectMapper();
                    final ObjectNode node = mapp.createObjectNode();
                    final ArrayNode arrayNode = mapp.createArrayNode();
                    ArrayList<Integer> p1 = new ArrayList<>();
                    while (resultSet.next()) {
                        final String s = resultSet.getString(2).replace(' ', 'T') + ":00";
                        final Post post = new Post(resultSet.getInt(8),
                                resultSet.getString(6), resultSet.getString(1),
                                s, resultSet.getBoolean(5), resultSet.getInt(7));
                        post.setForum(resultSet.getString(3));
                        post.setId(resultSet.getInt(4));
                        if (flag) {
                            post.setPath((Integer[]) resultSet.getArray(9).getArray());
                            p1.add(post.getPath()[0]);
                        }
                        arrayNode.add(post.getPostJson());
                    }
                    Integer i = offset + arrayNode.size();
                    if(flag){
                        List<Integer> u = p1.stream().distinct().collect(Collectors.toList());
                        i = offset + u.size();
                    }
                    node.put("marker", i.toString());
                    node.set("posts", arrayNode);
                    return new ResponseEntity(node, HttpStatus.OK);
                });
    }

    @SuppressWarnings("OverlyComplexMethod")
    public ResponseEntity getPostInfo(String slug, String sort,
                                      Integer id, Integer limit, Integer offset, boolean desc) {
        try {
            final ThreadService threadService = new ThreadService(dataSource);
            final Thread thread;
            if((thread = threadService.getFullThread(slug, id)) == null)
                throw new SQLException("Thread not found");
            if (sort == null) sort = "flat";
            final String descOrAsc = (desc ? "DESC " : "ASC ");
            switch (sort) {
                case "tree":
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id " +
                                    "FROM Message M " +
                                    "WHERE thread_id = ? ORDER BY path " +
                                    (desc ? "DESC " : "ASC ") + "LIMIT ? OFFSET ?;",
                            thread.getId(), limit, offset, false);
                case "parent_tree":
                    final String s = "M.thread_id = " + thread.getId();
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id, path " +
                                    "FROM Message M " +
                                    "WHERE M.path[1] IN ( " +
                                    "SELECT M.message_id FROM Message AS M " +
                                    "WHERE parent_id = 0 AND " +
                                    "thread_id = ? ORDER BY message_id " +
                                    (desc ? "DESC " : "ASC ") + " LIMIT ? OFFSET ?)"+
                                    "AND thread_id  = " + thread.getId() +" ORDER BY path " + descOrAsc + " , message_id " + descOrAsc,
                            thread.getId(), limit, offset, true);
                default:
                    return getPostsSort("SELECT M.author, M.create_date, M.forum, M.message_id, M.is_edit, " +
                                    "M.message, M.parent_id,  M.thread_id " +
                                    " FROM Message AS M  " +
                                    " WHERE thread_id = ? ORDER BY create_date "
                                    + descOrAsc + ", message_id " + descOrAsc + "LIMIT ? OFFSET ?;",
                            thread.getId(), limit, offset, false);
            }
        } catch (SQLException e) {
            return new ResponseEntity(Error.getErrorJson("Forum not Found"), HttpStatus.NOT_FOUND);
        }
    }

    public void addForumUsers(String forum,Post[] posts) throws SQLException {
        PrepareQuery.execute("INSERT INTO Forum_users (nickname, forum) " +
                        "VALUES (?,?)",// ON CONFLICT (nickname, forum) DO NOTHING",
                preparedStatement -> {
                    for(Post post: posts) {
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

    public List<Post> checkParents(Post[] body, Thread thread) throws SQLException {
        final Integer[] unique =
            Arrays.stream(body).map(Post::getParent).distinct().toArray(Integer[]::new);
        final List<Post> parents = new ArrayList<>();
        for(Integer parent: unique){
            if (parent != 0)
                parents.add(getParent(parent, thread));
        }
        return parents;
    }

    private List<Integer> messIdSeq(int num) throws SQLException {
        return PrepareQuery.execute("SELECT nextval('message_message_id_seq') from generate_series(1, ?);",
                preparedStatement->{
                    preparedStatement.setInt(1, num);
                    final List<Integer> arr = new ArrayList<>();
                    final ResultSet result = preparedStatement.executeQuery();
                    while (result.next())
                        arr.add(result.getInt(1));
                    return arr;
                });
    }


    public ArrayNode createPosts(String thread_slug, Integer thread_id, Post[] body) throws SQLException{
        final ObjectMapper mapp = new ObjectMapper();
        final ArrayNode arrayNode = mapp.createArrayNode();
        final ZonedDateTime postTime = ZonedDateTime.now();

        final ThreadService threadService = new ThreadService(dataSource);
        final Thread thread;
        if((thread = threadService.getFullThread(thread_slug, thread_id)) == null)
            throw new SQLException("Not found");

        final UserService userService = new UserService(dataSource);
        if(userService.getUserInfo(body[0].getAuthor()) == null)
            throw new SQLException("Not found");

        final List<Post> parents = checkParents(body, thread);
        return PrepareQuery.execute("INSERT INTO Message (thread_id, message, author, create_date, parent_id," +
                        " path, forum, message_id ) " +
                        "VALUES (?,?,?,?,?,array_append(?, ?::INT8), ?, ? )",
                preparedStatement -> {
                    final List<Integer> ids = messIdSeq(body.length);
                    int i = 0;
                    for (Post post: body) {
                        preparedStatement.setInt(1, thread.getId());
                        preparedStatement.setString(2, post.getMessage());
                        preparedStatement.setString(3, post.getAuthor());
                        preparedStatement.setInt(5, post.getParent());
                        for(Post parent: parents){
                            if(parent.getId().equals(post.getParent())) {
                                preparedStatement.setArray(6,
                                        getConnection().createArrayOf("int8", parent.getPath()));
                                break;
                            }
                        }
                        if(post.getParent() == 0)
                            preparedStatement.setArray(6,null);
                        preparedStatement.setString(8, thread.getForum());
                        if (post.getCreated() == null) {
                            post.setCreated(postTime);
                            final Timestamp t = new Timestamp(post.getCreated().getLong(ChronoField.INSTANT_SECONDS) * 1000
                                    + post.getCreated().getLong(ChronoField.MILLI_OF_SECOND));
                            preparedStatement.setTimestamp(4, t);
                        } else {
                            final Timestamp t = Timestamp.valueOf(post.getCreated().toLocalDateTime());
                            preparedStatement.setTimestamp(4, t);
                        }
                        post.setId(ids.get(i++));
                        preparedStatement.setInt(7, post.getId());
                        preparedStatement.setInt(9, post.getId());
                        preparedStatement.addBatch();
                        post.setForum(thread.getForum());
                        post.setThread_id(thread.getId());
                        arrayNode.add(post.getPostJson());
                    }
                    preparedStatement.executeBatch();
                    addForumUsers(thread.getForum(), body);
                    updForumMessgs(thread.getForum(), body.length);
                    return arrayNode;
                });
    }


    private Post getParent(Integer parent, Thread thread) throws SQLException {
        return PrepareQuery.execute("SELECT * FROM Message " +
                        "WHERE message_id = ? and thread_id = ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, parent);
                    preparedStatement.setInt(2, thread.getId());
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    if (!resultSet.next())
                        throw new SQLException("Message not found");
                    final String s = resultSet.getString(5).replace(' ', 'T') + ":00";
                    final Post post = new Post(resultSet.getInt(2), resultSet.getString(3),
                            resultSet.getString(4), s, resultSet.getBoolean(6),
                            resultSet.getInt(7));
                    post.setId(resultSet.getInt(1));
                    post.setForum(resultSet.getString(9));
                    post.setPath((Integer[])resultSet.getArray(8).getArray());
                    return post;
                });
    }

    @SuppressWarnings("OverlyComplexMethod")
    public ResponseEntity getPostRelated(Integer id, String[] related) {
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
        //try {
            final Post post = getPost(id);
            if(post == null)
                return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
            node.set("post", post.getPostJson());
            if (user) {
                final UserService userService = new UserService(dataSource);
                final User user1 = userService.getUserInfo(post.getAuthor());
                node.set("author", user1.getUserJson());
            }
            if (forum) {
                final ForumService forumService = new ForumService(dataSource);
                final Forum forum1 = forumService.getForumInfo(post.getForum());
                node.set("forum", forum1.getForumJson());

            }
            if (thread) {
                final ThreadService threadService = new ThreadService(dataSource);
                final Thread thread1 = threadService.getFullThread(null, post.getThread_id());
                node.set("thread", thread1.getThreadJson());
            }
            return new ResponseEntity(node, HttpStatus.OK);
//        } catch (SQLException e) {
//            return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
//        }
    }

    public ResponseEntity setPost(Integer id, Post body) {
        try {
            final Post post = getPost(id);
            if(post == null)
                return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
            if (body.getMessage() != null && !post.getMessage().equals(body.getMessage())) {
                return PrepareQuery.execute("UPDATE Message SET (message, is_edit) = (?, true) " +
                                "WHERE message_id = ?",
                        preparedStatement -> {
                            preparedStatement.setString(1, body.getMessage());
                            preparedStatement.setInt(2, id);
                            preparedStatement.executeUpdate();
                            post.setMessage(body.getMessage());
                            post.setIs_edit();
                            return new ResponseEntity(post.getPostJson(), HttpStatus.OK);
                        });
            }
            return new ResponseEntity(post.getPostJson(), HttpStatus.OK);
        } catch (SQLException n) {
            return new ResponseEntity(Error.getErrorJson("Post not found"), HttpStatus.NOT_FOUND);
        }
    }

}
