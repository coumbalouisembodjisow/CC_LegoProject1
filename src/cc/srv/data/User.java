package cc.srv.data;

import java.util.List;

public class User {
    private String id;
    private String name;
    private String password;
    private String photoMediaId; // ID for the user's photo in Azure Blob Storage
    private List<String> ownedLegoSets;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

   
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getOwnedLegoSets() {
        return ownedLegoSets;
    }

    public void setOwnedLegoSets(List<String> ownedLegoSets) {
        this.ownedLegoSets = ownedLegoSets;
    }

    public String getPhotoMediaId() {
        return photoMediaId;
    }

    public void setPhotoMediaId(String photoMediaId) {
        this.photoMediaId = photoMediaId;
    }

    public void addOwnedLegoSet(String legoSetId) {
        if (this.ownedLegoSets != null && !this.ownedLegoSets.contains(legoSetId)) {
            this.ownedLegoSets.add(legoSetId);
        }
    }
}
