package ru.mail.park.Post;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Created by Варя on 21.03.2017.
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "MethodParameterNamingConvention"})
public class Post {
    private BigDecimal id;
    private Integer thread_id;
    private BigDecimal parent = new BigDecimal(0);
    private String message;
    private String author;
    private String forum;
    private ZonedDateTime created;
    private Boolean is_edit = false;
    private Integer[] Path;
    private ObjectMapper map = new ObjectMapper();

    @JsonCreator
    public Post(@JsonProperty("thread") Integer thread_id, @JsonProperty("message") String message,
                  @JsonProperty("author") String author, @JsonProperty("created") String created,
                  @JsonProperty("isEdited") Boolean edited, @JsonProperty("parent") BigDecimal parent){
        this.thread_id = thread_id;
        this.message = message;
        this.author = author;
        if(created != null)
            this.created = ZonedDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        else this.created = null;
        if(edited != null) {this.is_edit = edited; }
        if(parent != null) { this.parent = parent; }
    }

    public BigDecimal getParent (){ return parent; }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public ZonedDateTime getCreated(){return created;}

    public String getCreate(){return created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}

    public String getMessage () {return this.message;}

    public String getAuthor (){ return this.author;}

    public Integer getThread_id() {return  this.thread_id; }

    public String getForum(){return  this.forum; }

    public void setForum (String forum) {this.forum = forum; }

    public void setId (BigDecimal id) {this.id = id; }

    public BigDecimal getId() {
        return id;
    }

    public void setPath(Integer[] path) {
        Path = path;
    }

    public Integer[] getPath() {
        return Path;
    }

    public void setMessage(String message){ this.message = message; }

    public void setIs_edit() {this.is_edit = true;}

    public void setThread_id (Integer thread_id) {this.thread_id = thread_id; }

    @JsonIgnore
    public ObjectNode getPostJson(){
        final ObjectNode node = map.createObjectNode();
        node.put("author", this.author);
        if(created != null) node.put("created", this.created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        node.put("forum", this.forum);
        node.put("id", this.id.intValue());
        node.put("isEdited", this.is_edit);
        node.put("message", this.message);
        node.put("thread", this.thread_id);
        if(parent.intValue() != 0) node.put("parent", this.parent.intValue());
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Post post = (Post) o;
        return Objects.equals(post.id, id);
    }
}
