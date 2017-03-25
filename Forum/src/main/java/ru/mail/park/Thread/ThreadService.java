package ru.mail.park.Thread;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.User.UserService;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.PrepareQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;

/**
 * Created by Варя on 16.03.2017.
 */
@Service
@Transactional
public class ThreadService extends DBConnect {

    @Autowired
    public ThreadService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Thread getThreadInfo(String slug, Integer t_id) {
        try {
            String req = "lower(T.slug) = lower(?)";
            if (t_id != null)
                req = "T.thread_id = ?";
            return PrepareQuery.execute("SELECT T.*, SUM(L.mark), F.slug  FROM Thread AS T " +
                            " LEFT JOIN TLike  AS L ON L.thread_id = T.thread_id JOIN Forum AS F " +
                            "ON F.slug = T.forum" +
                            " WHERE " + req +
                            " GROUP BY T.thread_id, F.slug",
                    preparedStatement -> {
                        if (t_id != null) {
                            preparedStatement.setInt(1, t_id);
                        } else preparedStatement.setString(1, slug);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        String s = resultSet.getString(3).replace(' ', 'T') + ":00";
                        System.out.println(s);
                        Thread thread = new Thread(resultSet.getString(2),
                                resultSet.getString(1), resultSet.getString(5),
                                resultSet.getString(4), s);
                        thread.setVotes(resultSet.getInt(8));
                        thread.setForum(resultSet.getString(9));
                        thread.setId(resultSet.getInt(7));
                        return thread;
                        //return new ResponseEntity(thread.getThreadJson(), HttpStatus.OK);
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
            //return new ResponseEntity(Error.getErrorJson("Thread not Found"), HttpStatus.NOT_FOUND);
        }
    }


    public ResponseEntity createThread(String slug, Thread body) {
        String forum;
        String nickname;
        try {
            forum = PrepareQuery.execute("SELECT slug FROM Forum WHERE lower(slug) = lower(?)",
                    preparedStatement -> {
                        preparedStatement.setString(1, slug);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        return resultSet.getString(1);
                    });
            nickname = PrepareQuery.execute("SELECT nickname FROM FUser WHERE lower(nickname) = lower(?)",
                    preparedStatement -> {
                        preparedStatement.setString(1, body.getAuthor());
                        ResultSet resultSet = preparedStatement.executeQuery();
                        resultSet.next();
                        return resultSet.getString(1);
                    });

            try {
                Thread responseEntity = getThreadInfo(body.getSlug(), null);
                if (responseEntity == null) {
                    PrepareQuery.execute("INSERT INTO Thread (author, slug, message, title, forum, create_date) " +
                                    "VALUES (?,?,?,?,?,?) RETURNING thread_id",
                            preparedStatement -> {
                                preparedStatement.setString(1, nickname);
                                preparedStatement.setString(2, body.getSlug());
                                preparedStatement.setString(3, body.getMessage());
                                preparedStatement.setString(4, body.getTitle());
                                preparedStatement.setString(5, forum);
                                if (body.getCreated() == null) {
                                    ZonedDateTime zonedDateTime = ZonedDateTime.now();
                                    //body.setCreated(zonedDateTime);
                                    preparedStatement.setTimestamp(6, Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
                                } else {
                                    Timestamp t = Timestamp.valueOf(body.getCreated().toLocalDateTime());
                                    //Timestamp t = new Timestamp(body.getCreated().getLong(ChronoField.INSTANT_SECONDS)*1000+ body.getCreated().getLong(ChronoField.MILLI_OF_SECOND));
                                    preparedStatement.setTimestamp(6, t);
                                }
                                ResultSet resultSet = preparedStatement.executeQuery();
                                resultSet.next();
                                body.setId(resultSet.getInt(1));
                                return null;
                            });
                    body.setForum(forum);
                    return new ResponseEntity(body.getThreadJson(), HttpStatus.CREATED);
                }
                return new ResponseEntity(responseEntity.getThreadJson(), HttpStatus.CONFLICT);
            } catch (SQLException n) {
                n.printStackTrace();
                return new ResponseEntity(Error.getErrorJson("Something go wrong"), HttpStatus.EXPECTATION_FAILED);

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Forum or User not found"), HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity voice(String thread_slug, Integer thread_id, Like body) {
        Integer t_id;
        try {
            String req = "lower(slug) = lower(?)";
            if (thread_id != null)
                req = "thread_id = ?";
            t_id = PrepareQuery.execute("SELECT thread_id FROM Thread WHERE " + req,
                    preparedStatement -> {
                        if (thread_id != null) {
                            preparedStatement.setInt(1, thread_id);
                        } else preparedStatement.setString(1, thread_slug);
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (!resultSet.next())
                            throw new SQLException("Thread not found");
                        return resultSet.getInt(1);
                    });
            UserService userService = new UserService(this.dataSource);
            if(userService.getUserInfo(body.getAuthor()) == null) {
                throw new SQLException("User not found");
            }
            try {
                int num = PrepareQuery.execute("UPDATE TLike SET mark = ? WHERE author = ? and thread_id = ?",
                        preparedStatement -> {
                            preparedStatement.setString(2, body.getAuthor());
                            preparedStatement.setInt(3, t_id);
                            preparedStatement.setInt(1, body.getVoice());
                            int col = preparedStatement.executeUpdate();
                            return col;
                        });
                if (num == 0) {
                    PrepareQuery.execute("INSERT INTO TLike values(?, ?, ?)",
                            preparedStatement -> {
                                preparedStatement.setString(1, body.getAuthor());
                                preparedStatement.setInt(2, t_id);
                                preparedStatement.setInt(3, body.getVoice());
                                preparedStatement.executeUpdate();
                                return null;
                            });
                }
            } catch (SQLException q) {
                q.printStackTrace();
                return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
            }
            return new ResponseEntity(getThreadInfo(thread_slug, t_id).getThreadJson(), HttpStatus.OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson(e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity changeThread(String slug, Integer id, Thread body) {
        Thread thread = getThreadInfo(slug, id);
        if (thread != null) {
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
                n.printStackTrace();
                return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
            }
        } else {
            return new ResponseEntity(Error.getErrorJson("Thread not found"), HttpStatus.NOT_FOUND);
        }
    }
}
