package ru.mail.park.Thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Варя on 11.03.2017.
 */
@SuppressWarnings("unused")
public class Thread {
    private String slug;
    private String message;
    private String title;
    private String author;
    private String forum;
    private Integer id;
    private Integer votes;
    private ZonedDateTime created;
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
            this.created = ZonedDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

    public ZonedDateTime getCreated(){return created;}

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

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public Integer getVotes() {
        return votes;
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
        if(votes != null) node.put("votes", this.votes);
        return node;
    }
}
