package ru.mail.park.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mail.park.Error.Error;
import ru.mail.park.db.DBConnect;
import ru.mail.park.db.SelectQuery;
import ru.mail.park.db.UpdateQuery;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by Варя on 24.03.2017.
 */
@Service
@Transactional
public class ServiceService extends DBConnect {

    @Autowired
    public ServiceService(DataSource dataSource){this.dataSource = dataSource; }

    public ResponseEntity serviceStatus(){
        try {
            int user = SelectQuery.execute("SELECT count(nickname) FROM public.\"User\"",
                    resultSet -> {
                        resultSet.next();
                        return resultSet.getInt(1);
                    });
            int forum = SelectQuery.execute("SELECT count(slug) FROM public.\"Forum\"",
                    resultSet -> {
                        resultSet.next();
                        return resultSet.getInt(1);
                    });
            int thread = SelectQuery.execute("SELECT count(thread_id) FROM public.\"Thread\"",
                    resultSet -> {
                        resultSet.next();
                        return resultSet.getInt(1);
                    });
            int post = SelectQuery.execute("SELECT count(message_id) FROM public.\"Message\"",
                    resultSet -> {
                        resultSet.next();
                        return resultSet.getInt(1);
                    });
            Servicee servicee = new Servicee(forum, post, thread, user);
            return new ResponseEntity(servicee.getServiceJson(), HttpStatus.OK);
        }
        catch (SQLException e){
            e.printStackTrace();
            return new ResponseEntity(Error.getErrorJson("Something gone wrong"), HttpStatus.EXPECTATION_FAILED);
        }
    }

    public ResponseEntity serviceClear(){
        try {
            UpdateQuery.execute("DELETE FROM public.\"Like\"");
            UpdateQuery.execute("DELETE FROM public.\"Message\"");
            UpdateQuery.execute("DELETE FROM public.\"Thread\"");
            UpdateQuery.execute("DELETE FROM public.\"Forum\"");
            UpdateQuery.execute("DELETE FROM public.\"User\"");
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }
}