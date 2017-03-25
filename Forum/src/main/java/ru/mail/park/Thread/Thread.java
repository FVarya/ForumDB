package ru.mail.park.Thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Formatter;

/**
 * Created by Варя on 11.03.2017.
 */
public class Thread {
    private String slug;
    private String message;
    private String title;
    private String author;
    private String forum;
    private Integer id;
    private Integer votes;
    private LocalDateTime created;
    private ObjectMapper map = new ObjectMapper();

    @JsonCreator
    public Thread(@JsonProperty("slug") String slug, @JsonProperty("author") String author,
            @JsonProperty("title") String title, @JsonProperty("message") String message,
                  @JsonProperty("created") String created){
        this.slug = slug;
        this.author = author;
        this.title = title;
        this.message = message;
        if(created != null)
            this.created = LocalDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME);
        else this.created = null;
    }

    public String getSlug(){
        return slug;
    }

    public String getAuthor(){
        return author;
    }

    public String getTitle(){
        return title;
    }

    public String getMessage(){
        return message;
    }

    public LocalDateTime getCreated(){return created;}

    public void setForum(String forum) {
        this.forum = forum;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setVotes(Integer votes) {
        this.votes = votes;
    }

    public void setMessage (String message) { this.message = message; }

    public void setTitle (String title) { this.title = title; }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Integer getId(){return this.id; }

    public ObjectNode getThreadJson(){
        final ObjectNode node = map.createObjectNode();
        node.put("author", this.author);
        if(created != null) node.put("created", this.created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("forum", this.forum);
        node.put("id", this.id);
        node.put("message", this.message);
        node.put("title", this.title);
        if(slug != null) node.put("slug", this.slug);
        if(node != null) node.put("votes", this.votes);
        return node;
    }
}
