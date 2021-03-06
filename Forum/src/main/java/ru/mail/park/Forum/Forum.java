package ru.mail.park.Forum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Created by Варя on 14.03.2017.
 */
@SuppressWarnings("unused")
public class Forum {
    private String slug;
    private String title;
    private String admin;

    private Integer posts;
    private Integer threads;

    private ObjectMapper map = new ObjectMapper();

    @JsonCreator
    public Forum(@JsonProperty("slug") String slug, @JsonProperty("title") String title,
                 @JsonProperty("user") String admin){
        this.slug = slug;
        this.title = title;
        this.admin = admin;
    }

    public String getSlug(){
        return slug;
    }

    public String getTitle(){
        return title;
    }

    public String getAdmin(){
        return admin;
    }

    public void setSlug(String slug){
        this.slug = slug;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setAdmin(String admin){
        this.admin = admin;
    }

    public void setPosts(Integer posts){ this.posts = posts; }

    public void setThreads(Integer threads){ this.threads = threads; }

    public ObjectNode getForumJson(){
        final ObjectNode node = map.createObjectNode();
        if (posts != null)node.put("posts", this.posts.intValue());
        node.put("slug", this.slug);
        if(threads != null) node.put("threads", this.threads.intValue());
        node.put("title", this.title);
        node.put("user", this.admin);
        return node;
    }
}
