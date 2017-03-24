package ru.mail.park.Thread;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Варя on 21.03.2017.
 */
public class Like {
    private  String author;
    private int voice;

    public String getAuthor(){ return this.author; }

    public int getVoice(){return this.voice; }

    @JsonCreator
    public Like(@JsonProperty("nickname") String author, @JsonProperty("voice") int voice){
        this.author = author;
        this.voice = voice;
    }
}
