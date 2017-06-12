package ru.mail.park.Post;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import ru.mail.park.Error.Error;
import ru.mail.park.Thread.Thread;

/**
 * Created by Варя on 21.03.2017.
 */
@SuppressWarnings({"MethodParameterNamingConvention", "unused"})
@RestController
public class PostController {

    private final PostService postService;

    @PostMapping("/api/thread/{slug or id}/create")
    public ResponseEntity createThread(@PathVariable("slug or id") String slug_or_id,
                                       @RequestBody /*List<Post> posts*/Post[] posts) {
        Integer id = null;
        try {
            id = Integer.parseInt(slug_or_id);
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }
        if(posts == null || posts.length == 0){
            return new ResponseEntity(null, HttpStatus.NOT_FOUND);
        }
        final ArrayNode arrayNode;
        try{
            arrayNode = postService.createPosts(slug_or_id, id, posts);
        }
        catch (SQLException e){
            e.printStackTrace();
            if(e.getMessage().equals("Not found")){
                return new ResponseEntity(Error.getErrorJson("Not found"), HttpStatus.NOT_FOUND);
            }
            else return new ResponseEntity(Error.getErrorJson("Parent not found"), HttpStatus.CONFLICT);
        }
        return new ResponseEntity(arrayNode, HttpStatus.CREATED);
    }

    @GetMapping("api/thread/{slug or id}/posts")
    public ResponseEntity getPosts(@PathVariable("slug or id") String slug_or_id,
                                   @RequestParam(value = "limit", required = false) Double limit ,
                                   @RequestParam(value = "marker", required = false) String marker,
                                   @RequestParam(value = "desc", required = false) String desc,
                                   @RequestParam(value = "sort", required = false) String sort){
        boolean s = false;
        if(desc != null && desc.equals("true")){
            s = true;
        }
        Integer id = null;
        try {
            id = Integer.parseInt(slug_or_id);
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }
        return postService.getPostInfo(slug_or_id, id, limit, marker, s, sort);
    }

    @GetMapping("api/post/{id}/details")
    public ResponseEntity getPost(@PathVariable("id")BigDecimal id,
                                  @RequestParam(value = "related", required = false) String[] related){
        return postService.getPostRelated(id, related);
    }

    @PostMapping("api/post/{id}/details")
    public ResponseEntity setPost(@PathVariable("id")BigDecimal id, @RequestBody Post body){
        return postService.setPost(id, body);
    }

    public PostController(@NotNull PostService postService){
        this.postService = postService;
    }
}
