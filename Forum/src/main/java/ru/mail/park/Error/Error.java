package ru.mail.park.Error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Варя on 11.03.2017.
 */
public class Error {


    public static ObjectNode getErrorJson (String error){
        final ObjectMapper map = new ObjectMapper();
        final ObjectNode node = map.createObjectNode();
        node.put("error",error);
        return node;
    }

}
