package cc.srv.data;

import java.util.Set;
import java.util.HashSet;

public class User {
    private String id;
    private String name;
    private String password;
    private String photoMediaId; // ID for the user's photo in Azure Blob Storage
    private Set<String> ownedLegoSets;

    // Constructors
    public User() {
        this.ownedLegoSets = new HashSet<>();
    }
    public User(String id, String name, String password, String photoMediaId, Set<String> ownedLegoSets) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.photoMediaId = photoMediaId;
        this.ownedLegoSets = ownedLegoSets != null ? ownedLegoSets : new HashSet<>();
    }

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
    public void setOwnedLegoSets(Set<String> ownedLegoSets) {
        this.ownedLegoSets = ownedLegoSets;
    }

    public String getPhotoMediaId() {
        return photoMediaId;
    }

    public void setPhotoMediaId(String photoMediaId) {
        this.photoMediaId = photoMediaId;
    }

     public Set<String> getOwnedLegoSets() {
        if (this.ownedLegoSets == null) {
            this.ownedLegoSets = new HashSet<>(); 
        }
        return this.ownedLegoSets;
    }

     public void addOwnedLegoSet(String legoSetId) {
        getOwnedLegoSets().add(legoSetId); 
    }
    

    public boolean ownsLegoSet(String legoSetId) {
        return getOwnedLegoSets().contains(legoSetId); 
    }
}
