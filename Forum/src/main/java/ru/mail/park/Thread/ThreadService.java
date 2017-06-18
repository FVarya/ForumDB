package ru.mail.park.Thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.Forum.Forum;
import ru.mail.park.Forum.ForumService;
import ru.mail.park.User.User;
import ru.mail.park.User.UserService;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.PrepareQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Created by Варя on 16.03.2017.
 */
@SuppressWarnings({"MethodParameterNamingConvention", "JDBCResourceOpenedButNotSafelyClosed", "LocalVariableNamingConvention"})
@Service
@Transactional
public class ThreadService extends DBConnect {
    ForumService forumService;
    @Autowired
    public ThreadService(DataSource dataSource, ForumService forumService) {
        DBConnect.dataSource = dataSource;
        this.forumService = forumService;
    }

    public Thread getFullThread(String slug, Integer t_id){
        try {
            String req = "lower(slug) = lower(?)";
            if (t_id != null)
                req = "thread_id = ?";
            return PrepareQuery.execute("SELECT *  FROM Thread " +
                            " WHERE " + req ,
                    preparedStatement -> {
                        if (t_id != null) {
                            preparedStatement.setInt(1, t_id);
                        } else preparedStatement.setString(1, slug);
                        final ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        final String s = resultSet.getString(3).replace(' ', 'T') + ":00";
                        final Thread thread = new Thread(resultSet.getString(2),
                                resultSet.getString(1), resultSet.getString(5),
                                resultSet.getString(4), s);
                        thread.setVotes(resultSet.getInt(8));
                        thread.setForum(resultSet.getString(6));
                        thread.setId(resultSet.getInt(7));
                        return thread;
                    });
        } catch (SQLException e) {
            return null;
        }
    }


    private void updForumThreads(String slug) throws SQLException {
        PrepareQuery.execute("UPDATE Forum SET threads = threads + 1 WHERE lower(slug) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, slug);
                    preparedStatement.executeUpdate();
                    return null;
                });
    }

    public ResponseEntity createThread(String slug, Thread body) {
        final Forum forum;
        if ((forum = forumService.getForumInfo(slug)) == null)
            return new ResponseEntity(Error.getErrorJson("Forum not found"), HttpStatus.NOT_FOUND);
        final UserService userService = new UserService(dataSource);
        final User user;
        if ((user = userService.getUserInfo(body.getAuthor())) == null)
            return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
        try {
            final Thread responseEntity = getFullThread(body.getSlug(), null);
            if (responseEntity == null) {
                PrepareQuery.execute("INSERT INTO Thread (author, slug, message, title, forum, create_date) " +
                                "VALUES (?,?,?,?,?,?) RETURNING thread_id",
                        preparedStatement -> {
                            preparedStatement.setString(1, user.getNickname());
                            preparedStatement.setString(2, body.getSlug());
                            preparedStatement.setString(3, body.getMessage());
                            preparedStatement.setString(4, body.getTitle());
                            preparedStatement.setString(5, forum.getSlug());
                            if (body.getCreated() == null) {
                                final ZonedDateTime zonedDateTime = ZonedDateTime.now();
                                preparedStatement.setTimestamp(6, Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
                            } else {
                                final Timestamp t = new Timestamp(body.getCreated().getLong(ChronoField.INSTANT_SECONDS) * 1000 + body.getCreated().getLong(ChronoField.MILLI_OF_SECOND));
                                preparedStatement.setTimestamp(6, t);
                            }
                            final ResultSet resultSet = preparedStatement.executeQuery();
                            resultSet.next();
                            body.setId(resultSet.getInt(1));
                            updForumThreads(slug);
                            return null;
                        });
                body.setForum(forum.getSlug());
                return new ResponseEntity(body.getThreadJson(), HttpStatus.CREATED);
            }
            return new ResponseEntity(responseEntity.getThreadJson(), HttpStatus.CONFLICT);
        } catch (SQLException n) {
            n.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Something go wrong"), HttpStatus.EXPECTATION_FAILED);
        }
    }

    private int updTreadVotes(Thread thread, int voice) throws SQLException {
        return PrepareQuery.execute("UPDATE Thread SET votes = ? " +
                        "WHERE thread_id = ? RETURNING votes;",
                preparedStatement -> {
                    preparedStatement.setInt(1, voice + thread.getVotes());
                    preparedStatement.setInt(2, thread.getId());
                    preparedStatement.executeQuery();
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    resultSet.next();
                    return resultSet.getInt(1);
                });
    }

    private int updTreadVotes(Thread thread) throws SQLException {
        return PrepareQuery.execute("UPDATE Thread SET votes = (SELECT SUM(mark) FROM Tlike " +
                        "WHERE thread_id = ?) WHERE thread_id = ? RETURNING votes;",
                preparedStatement -> {
                    preparedStatement.setInt(1, thread.getId());
                    preparedStatement.setInt(2, thread.getId());
                    preparedStatement.executeQuery();
                    final ResultSet resultSet = preparedStatement.executeQuery();
                    resultSet.next();
                    return resultSet.getInt(1);
                });
    }

    public ResponseEntity voice(String thread_slug, Integer thread_id, Like body) {
        final Thread thread;
        if((thread = getFullThread(thread_slug, thread_id)) == null)
            return new ResponseEntity(Error.getErrorJson("Thread not found"), HttpStatus.NOT_FOUND);
        final UserService userService = new UserService(dataSource);
        if(userService.getUserInfo(body.getAuthor()) == null) {
            return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
        }
        try {
            final int num = PrepareQuery.execute("INSERT INTO TLike (author, thread_id, mark) " +
                            "VALUES(?,?,?) ON CONFLICT(author, thread_id) " +
                            "DO UPDATE SET mark = excluded.mark ",
                    preparedStatement -> {
                        preparedStatement.setString(1, body.getAuthor());
                        preparedStatement.setInt(2, thread.getId());
                        preparedStatement.setInt(3, body.getVoice());
                        return preparedStatement.executeUpdate();
                    });
            if(num == 0)
                thread.setVotes(updTreadVotes(thread, body.getVoice()));
            else thread.setVotes(updTreadVotes(thread));
        } catch (SQLException q) {
            q.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
        }
        return new ResponseEntity(thread.getThreadJson(), HttpStatus.OK);
    }

    public ResponseEntity changeThread(String slug, Integer id, Thread body) {
        final Thread thread;
        if((thread = getFullThread(slug, id)) == null)
            return new ResponseEntity(Error.getErrorJson("Thread not found"), HttpStatus.NOT_FOUND);
        try {
            return PrepareQuery.execute("UPDATE Thread SET (message, title) = (?,?) " +
                            "WHERE thread_id = ?",
                    ps2 -> {
                        if (body.getMessage() == null) {
                            ps2.setString(1, thread.getMessage());
                        } else {
                            ps2.setString(1, body.getMessage());
                            thread.setMessage(body.getMessage());
                        }
                        if (body.getTitle() == null) {
                            ps2.setString(2, thread.getTitle());
                        } else {
                            ps2.setString(2, body.getTitle());
                            thread.setTitle(body.getTitle());
                        }
                        ps2.setInt(3, thread.getId());
                        ps2.executeUpdate();
                        return new ResponseEntity(thread.getThreadJson(), HttpStatus.OK);
                    });
        } catch (SQLException n) {
            return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
        }
    }

}
