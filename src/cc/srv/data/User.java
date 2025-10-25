package cc.srv.data;

import java.util.List;

public class User {
    private String id;
    private String nickname;
    private String name;
    private String password;
    private List<String> ownedLegoSets;
    private String photoMediaId; // ID for the user's photo in Azure Blob Storage

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
}
