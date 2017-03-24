package ru.mail.park.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

/**
 * Created by Варя on 11.03.2017.
 */
public class User {
    private String nickname;
    private String email;
    private String fullname;
    private String about;
    private ObjectMapper map = new ObjectMapper();

    @JsonCreator
    public User(@JsonProperty("nickname") String nickname,@JsonProperty("email") String email,
                @JsonProperty("fullname") String fullname,@JsonProperty("about") String about){
        this.nickname = nickname;
        this.email = email;
        this.fullname = fullname;
        this.about = about;
    }

    public String getNickname(){
        return nickname;
    }

    public String getEmail(){
        return email;
    }

    public String getFullname(){
        return fullname;
    }

    public String getAbout(){
        return about;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEmail(String email){this.email = email;}

    public void setFullname(String fullname){this.fullname = fullname;}

    public void setAbout(String about){this.about = about;}


    public ObjectNode getUserJson(){
        final ObjectNode node = map.createObjectNode();
        node.put("about", this.about);
        node.put("email", this.email);
        node.put("fullname", this.fullname);
        node.put("nickname", this.nickname);
        return node;
    }
}
