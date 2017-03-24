package ru.mail.park.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Варя on 24.03.2017.
 */
public class Servicee {
    private int forum;
    private int post;
    private int thread;
    private int user;
    private ObjectMapper map = new ObjectMapper();

    public Servicee(int forum, int post, int thread, int user){
        this.forum = forum;
        this.post = post;
        this.thread = thread;
        this.user = user;
    }

    public ObjectNode getServiceJson(){
        final ObjectNode node = map.createObjectNode();
        node.put("forum", this.forum);
        node.put("thread", this.thread);
        node.put("post", this.post);
        node.put("user", this.user);
        return node;
    }
}
