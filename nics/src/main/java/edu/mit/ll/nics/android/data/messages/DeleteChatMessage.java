package edu.mit.ll.nics.android.data.messages;

import java.util.List;
import edu.mit.ll.nics.android.database.entities.Chat;

public class DeleteChatMessage {
    private String message;
    private List<Chat> chats;
    private int count;

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

