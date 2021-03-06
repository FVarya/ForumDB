package ru.mail.park.User;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import ru.mail.park.Error.Error;

import java.util.List;


/**
 * Created by Варя on 12.03.2017.
 */

@SuppressWarnings("unused")
@RestController
public class UserController {

    private final UserService userService;

    @PostMapping("/api/user/{nickname}/create")
    public ResponseEntity createUser(@PathVariable("nickname") String nickname, @RequestBody User body){
        body.setNickname(nickname);
        return userService.createUser(body);
    }

    @GetMapping("/api/user/{nickname}/profile")
    public ResponseEntity getUserInfo(@PathVariable("nickname") String nickname){
        final User user;
        if((user = userService.getUserInfo(nickname)) != null) {
            return new ResponseEntity(user.getUserJson(), HttpStatus.OK);
        }
        else return new ResponseEntity(Error.getErrorJson("User not found"), HttpStatus.NOT_FOUND);
    }

    @PostMapping("/api/user/{nickname}/profile")
    public ResponseEntity setUserInfo(@PathVariable("nickname") String nikname, @RequestBody User body){
        return userService.changeUserInfo(nikname, body);
    }

    public UserController(@NotNull UserService userService){
        this.userService = userService;
    }
}
