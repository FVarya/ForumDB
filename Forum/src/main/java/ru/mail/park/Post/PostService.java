package ru.mail.park.Post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Created by Варя on 21.03.2017.
 */
@Service
@Transactional
public class PostService extends DBConnect {
    private Double marker;
    private ZonedDateTime moment;

    @Autowired
    public PostService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private Post getPost(BigDecimal id) throws SQLException{
        return PrepareQuery.execute("SELECT M.*, F.slug FROM public.\"Message\" AS M " +
                        "JOIN public.\"Thread\" AS T USING(thread_id) " +
                        "JOIN public.\"Forum\" AS F ON F.slug = T.forum " +
                        "WHERE message_id = ?",
                preparedStatement -> {
                    preparedStatement.setBigDecimal(1, id);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    resultSet.next();
                    String s = resultSet.getString(5).replace(' ', 'T') + ":00";
                    Post post1 = new Post(resultSet.getInt(2), resultSet.getString(3),
                            resultSet.getString(4), s, resultSet.getBoolean(6),
                            resultSet.getBigDecimal(7));
                    post1.setId(resultSet.getBigDecimal(1));
                    post1.setForum(resultSet.getString(8));
                    return post1;
                });
    }

    private ResponseEntity getPostsSort(String query, Integer id, String slug) throws SQLException{
        return PrepareQuery.execute(query,
                preparedStatement -> {
                    if (id != null) {
                        preparedStatement.setInt(1, id);
                    } else preparedStatement.setString(1, slug);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    final ObjectMapper mapp = new ObjectMapper();
                    final ObjectNode node = mapp.createObjectNode();
                    final ArrayNode arrayNode = mapp.createArrayNode();
                    node.put("marker", "marker");
                    while (resultSet.next()) {
                        String s = resultSet.getString(2).replace(' ', 'T') + ":00";
                        Post post = new Post(resultSet.getInt(8),
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

    public ResponseEntity getPostInfo(String slug, Integer id, Double limit, String mark, Boolean desc, String sort) {
        String strSort = "ASC";
        if (desc)
            strSort = "DESC";
        String sort1 = "";
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
        if (sort.equals("parent_tree"))
            sort1 = "ORDER BY M.message_id " + strSort + strLimit + strMarker;
        else if (sort.equals("tree")) {
            strSort += strLimit + strMarker;
        }
        try {
            String req = "lower(T.slug) = lower(?)";
            if (id != null)
                req = " T.thread_id = ?";
            ThreadService threadService = new ThreadService(this.dataSource);
            if(threadService.getThreadInfo(slug, id) == null)
                throw new SQLException("Forum not found");
            if (sort.equals("parent_tree") || sort.equals("tree")) {
                return getPostsSort("WITH RECURSIVE rtree (author, create_date, forum, id, isEdit, message" +
                        ", parent, thread, path ) AS (" +
                        " (SELECT M.author, M.create_date, F.slug, M.message_id, M.is_edit, M.message, M.parent_id, " +
                        "M.thread_id, array[M.message_id] " +
                        "FROM public.\"Message\" AS M " +
                        "JOIN public.\"Thread\"  AS T USING(thread_id) JOIN public.\"Forum\" AS F " +
                        "ON F.slug = T.forum" +
                        " WHERE " + req + " and M.parent_id is null " + sort1 +
                        ") UNION ALL" +
                        " SELECT Mm.author, Mm.create_date, Ff.slug, Mm.message_id, Mm.is_edit, " +
                        "Mm.message, Mm.parent_id, Mm.thread_id, " +
                        "array_append(path, Mm.message_id) " +
                        "FROM public.\"Message\" AS Mm " +
                        "JOIN public.\"Thread\"  AS Tt USING(thread_id) JOIN public.\"Forum\" " +
                        "AS Ff ON Ff.slug = Tt.forum " +
                        "JOIN rtree AS rt ON rt.id = Mm.parent_id ) " +
                        "SELECT r.*, array_to_string(path, '.') as path1 FROM rtree AS r " +
                        "ORDER BY path " + strSort,
                        id, slug);
            } else {
                return getPostsSort("SELECT M.author, M.create_date, F.slug, M.message_id, M.is_edit, " +
                        "M.message, M.parent_id,  M.thread_id " +
                        " FROM public.\"Message\" AS M  " +
                        " JOIN public.\"Thread\"  AS T USING(thread_id) JOIN public.\"Forum\" AS F " +
                        " ON F.slug = T.forum" +
                        " WHERE " + req + " ORDER BY M.create_date, M.message_id " + strSort + strLimit + strMarker,
                        id, slug);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Forum not Found"), HttpStatus.NOT_FOUND);
        }
    }


    public Post createPost(String thread_slug, Integer thread_id, Post body) throws SQLException {

        String req = "lower(slug) = lower(?)";
        if (thread_id != null)
            req = "thread_id = ?";
        final int t_id = PrepareQuery.execute("SELECT thread_id FROM public.\"Thread\" WHERE " + req,
                preparedStatement -> {
                    if (thread_id != null) {
                        preparedStatement.setInt(1, thread_id);
                    } else preparedStatement.setString(1, thread_slug);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (!resultSet.next())
                        throw new SQLException("Not found");
                    return resultSet.getInt(1);
                });
        if (body.getParent().intValue() != 0) {
            PrepareQuery.execute("SELECT M.message_id FROM public.\"Message\" AS M " +
                            "JOIN public.\"Thread\" as T USING(thread_id) " +
                            "WHERE M.message_id = ? and T.thread_id = ?",
                    preparedStatement -> {
                        preparedStatement.setBigDecimal(1, body.getParent());
                        preparedStatement.setInt(2, t_id);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (!resultSet.next())
                            throw new SQLException("Message not found");
                        return resultSet.getString(1);
                    });
        }
        final String forum = PrepareQuery.execute("SELECT F.slug FROM " +
                        "public.\"Thread\" as T  " +
                        "JOIN public.\"Forum\" as F ON T.forum = F.slug WHERE T.thread_id = ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, t_id);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (!resultSet.next())
                        throw new SQLException("Not found");
                    return resultSet.getString(1);
                });
        UserService userService = new UserService(this.dataSource);
        if(userService.getUserInfo(body.getAuthor()) == null){
            throw new SQLException("Not found");
        }
        try {
            return PrepareQuery.execute("INSERT INTO public.\"Message\" (thread_id, message, author, create_date, parent_id) " +
                            "VALUES (?,?,?,?,?) RETURNING message_id",
                    preparedStatement -> {
                        preparedStatement.setInt(1, t_id);
                        preparedStatement.setString(2, body.getMessage());
                        preparedStatement.setString(3, body.getAuthor());
                        if (body.getParent().intValue() == 0) {
                            preparedStatement.setNull(5, -5);
                        } else preparedStatement.setBigDecimal(5, body.getParent());
                        if (body.getCreated() == null) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.now();
                            if (moment != null && zonedDateTime.getLong(ChronoField.SECOND_OF_DAY) - moment.getLong(ChronoField.SECOND_OF_DAY) <= 120 ) {
                                zonedDateTime = moment;
                            } else moment = zonedDateTime;
                            body.setCreated(zonedDateTime);
                            preparedStatement.setTimestamp(4, Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
                        } else {
                            Timestamp t = Timestamp.valueOf(body.getCreated().toLocalDateTime());
                            preparedStatement.setTimestamp(4, t);
                        }
                        body.setForum(forum);
                        body.setThread_id(t_id);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        body.setId(resultSet.getBigDecimal(1));
                        return body;
                    });
        } catch (SQLException n) {
            n.printStackTrace();
            return null;
        }
    }

    public ResponseEntity getPostRelated(BigDecimal id, String[] related) {
        Post post;
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
            post = getPost(id);
            node.set("post", post.getPostJson());
            if (user) {
                UserService userService = new UserService(this.dataSource);
                User user1 = userService.getUserInfo(post.getAuthor());
                node.set("author", user1.getUserJson());
            }
            if (forum) {
                ForumService forumService = new ForumService(this.dataSource);
                Forum forum1 = forumService.getForumInfo(post.getForum());
                node.set("forum", forum1.getForumJson());

            }
            if (thread) {
                final ThreadService threadService = new ThreadService(this.dataSource);
                final Thread thread1 = threadService.getThreadInfo(null, post.getThread_id());
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
            Post post = getPost(id);
            if (body.getMessage() != null && !post.getMessage().equals(body.getMessage())) {
                return PrepareQuery.execute("UPDATE public.\"Message\" SET (message, is_edit) = (?, true) " +
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
