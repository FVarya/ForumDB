package ru.mail.park.Post;


import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import ru.mail.park.Error.Error;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Created by Варя on 21.03.2017.
 */
@SuppressWarnings({"MethodParameterNamingConvention", "unused"})
@RestController
public class PostController {

    private final PostService postService;

    @PostMapping("/api/thread/{slug or id}/create")
    public ResponseEntity createThread(@PathVariable("slug or id") String slug_or_id,
                                       @RequestBody Post[] posts) {
        Integer id = null;
        try {
            id = Integer.parseInt(slug_or_id);
        }
        catch (NumberFormatException ignored){
        }
        if(posts == null || posts.length == 0){
            return new ResponseEntity(null, NOT_FOUND);
        }
        final ArrayNode arrayNode;
        try{
            arrayNode = postService.createPosts(slug_or_id, id, posts);
        }
        catch (SQLException e){
            e.printStackTrace();
            if(e.getMessage().equals("Not found")){
                return new ResponseEntity(Error.getErrorJson("Not found"), NOT_FOUND);
            }
            else return new ResponseEntity(Error.getErrorJson("Parent not found"), HttpStatus.CONFLICT);
        }
        return new ResponseEntity(arrayNode, HttpStatus.CREATED);
    }

    @GetMapping("api/thread/{slug or id}/posts")
    public ResponseEntity getPosts(@PathVariable("slug or id") String slug_or_id,
                                   @RequestParam(value = "limit", required = false) Integer limit ,
                                   @RequestParam(value = "marker", required = false) Integer marker,
                                   @RequestParam(value = "desc", required = false) boolean desc,
                                   @RequestParam(value = "sort", required = false) String sort){

        final Integer offset = marker == null ? 0 : marker;
        Integer id = null;
        try {
            id = Integer.parseInt(slug_or_id);
        }
        catch (NumberFormatException ignored){
        }
        return postService.getPostInfo(slug_or_id, sort, id, limit, offset , desc);
    }


    @GetMapping("api/post/{id}/details")
    public ResponseEntity getPost(@PathVariable("id")Integer id,
                                  @RequestParam(value = "related", required = false) String[] related){
        return postService.getPostRelated(id, related);
    }

    @PostMapping("api/post/{id}/details")
    public ResponseEntity setPost(@PathVariable("id")Integer id, @RequestBody Post body){
        return postService.setPost(id, body);
    }

    public PostController(@NotNull PostService postService){
        this.postService = postService;
    }

}
