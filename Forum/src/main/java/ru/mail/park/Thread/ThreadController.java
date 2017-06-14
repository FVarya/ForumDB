package ru.mail.park.Thread;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mail.park.Error.Error;
import javax.validation.constraints.NotNull;
/**
 * Created by Варя on 17.03.2017.
 */
@SuppressWarnings({"unused", "MethodParameterNamingConvention"})
@RestController
public class ThreadController {

    private final ThreadService threadService;

    @PostMapping("/api/forum/{slug}/create")
    public ResponseEntity createThread(@PathVariable("slug") String slug, @RequestBody Thread body){
        return threadService.createThread(slug, body);
    }

    @PostMapping("/api/thread/{slug or id}/vote")
    public ResponseEntity voteThread(@PathVariable("slug or id") String slug_or_id, @RequestBody  Like like) {
        final Integer id = getInt(slug_or_id);
        return threadService.voice(slug_or_id, id, like);
    }

    @GetMapping("/api/thread/{slug or id}/details")
    public ResponseEntity getThread(@PathVariable("slug or id") String slug_or_id){
        final Integer id = getInt(slug_or_id);
        final Thread thread = threadService.getFullThread(slug_or_id, id);
        if(thread == null) {
            return new ResponseEntity(Error.getErrorJson("Thread not Found"), HttpStatus.NOT_FOUND);
        }
        else return new ResponseEntity(thread.getThreadJson(), HttpStatus.OK);
    }

    @PostMapping("/api/thread/{slug or id}/details")
    public ResponseEntity setThread(@PathVariable("slug or id") String slug_or_id, @RequestBody  Thread thread) {
        final Integer id = getInt(slug_or_id);
        return threadService.changeThread(slug_or_id, id, thread);
    }

    private Integer getInt(@PathVariable("slug or id") String slug_or_id) {
        try {
            return Integer.parseInt(slug_or_id);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public ThreadController(@NotNull ThreadService threadService){
        this.threadService = threadService;
    }

}
