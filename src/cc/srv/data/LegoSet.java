package cc.srv.data;

//import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class LegoSet {
    private String id;
    private String name;
    private String codeNumber;  
    private String description;
    private List<String> photoMediaIds; // IDs for photos stored in Azure Blob Storage

    
    // Constructeurs
    public LegoSet() {
        this.photoMediaIds = new ArrayList<>();
    }
    
    public LegoSet(String id, String name, String codeNumber, String description) {
        this();
        this.id = id;
        this.name = name;
        this.codeNumber = codeNumber;
        this.description = description;
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCodeNumber() { return codeNumber; }
    public void setCodeNumber(String codeNumber) { this.codeNumber = codeNumber; }
    
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
}