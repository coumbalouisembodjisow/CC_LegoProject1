package cc.srv.data;

import java.util.Date;

public class Comment {
    private String id;
    private String legoSetId;
    private String userId;
    private String content;
   

     public Comment() {}
    public Comment(String id, String legoSetId, String userId, String content) {
        this.id = id;
        this.legoSetId = legoSetId;
        this.userId = userId;
        this.content = content;
    }

    

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLegoSetId() { return legoSetId; }
    public void setLegoSetId(String legoSetId) { this.legoSetId = legoSetId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { 
        this.content = content;
    
    }

}
