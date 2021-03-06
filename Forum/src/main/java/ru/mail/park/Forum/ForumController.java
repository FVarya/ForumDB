package ru.mail.park.Forum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mail.park.Error.Error;
import static org.springframework.http.HttpStatus.NOT_FOUND;


/**
 * Created by Варя on 14.03.2017.
 */

@SuppressWarnings("unused")
@RestController
public class ForumController {
    private final ForumService forumService;

    @Autowired
    public ForumController(ForumService forumService){
        this.forumService = forumService;
    }

    @PostMapping("/api/forum/create")
    public ResponseEntity createForum(@RequestBody Forum body){
        return forumService.createForum(body);
    }

    @GetMapping("/api/forum/{slug}/details")
    public ResponseEntity getForumInfo(@PathVariable("slug") String slug){
        final Forum forum;
        if((forum = forumService.getForumInfo(slug)) != null) {
            return new ResponseEntity(forum.getForumJson(), HttpStatus.OK);
        }
        return new ResponseEntity(Error.getErrorJson("Forum not found"), NOT_FOUND);
    }

    @GetMapping("/api/forum/{slug}/users")
    public ResponseEntity getUserForum(@PathVariable("slug") String slug,
                                   @RequestParam(value = "limit", required = false) Integer limit ,
                                   @RequestParam(value = "since", required = false) String since,
                                   @RequestParam(value = "desc", required = false) boolean desc){
        if (forumService.getForumInfo(slug) == null) {
            return new ResponseEntity(Error.getErrorJson("Forum not found"), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(forumService.userList(slug, limit, since, desc));
    }

    @GetMapping("/api/forum/{slug}/threads")
    public ResponseEntity getThreadForum(@PathVariable("slug") String slug,
                                   @RequestParam(value = "limit", required = false) Integer limit ,
                                   @RequestParam(value = "since", required = false) String since,
                                   @RequestParam(value = "desc", required = false)  Boolean desc){
        if (desc == null) {
            desc = false;
        }
        return forumService.threadList(slug, limit, since, desc);
    }
}
