package cc.srv.data;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class LegoSet {
    private String id;
    private String name;
    private String description;
    private List<String> photoMediaIds; // IDs for photos stored in Azure Blob Storage
    private Date createdAt; // to have the most recent sets
    
    // Constructeurs
    public LegoSet() {
        this.createdAt = new Date();
        this.photoMediaIds = new ArrayList<>();
    }
    
    public LegoSet(String id, String name, String codeNumber, String description) {
        this();
        this.id = id;
        this.name = name;
        
        this.description = description;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
   
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<String> getPhotoMediaIds() { return photoMediaIds; }
    public void setPhotoUrls(List<String> photoMediaIds) { this.photoMediaIds = photoMediaIds; }
    

    //  add a photo Media Id to the list
    public void addPhotoMediaId(String photoMediaId) {
        if (this.photoMediaIds == null) {
            this.photoMediaIds = new ArrayList<>();
        }
        this.photoMediaIds.add(photoMediaId );
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}