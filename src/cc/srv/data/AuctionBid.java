package cc.srv.data;

import java.util.Date;

public class AuctionBid {
    private String id;
    private String auctionId; // id of the auction
    private String userId;  // id of the user placing the bid
    private double amount;    // bid amount
    
    
    // Constructors
    public AuctionBid() {
        
    }
    
    
    public AuctionBid(String id,String auctionId,String userId, double amount) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;

    
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

}