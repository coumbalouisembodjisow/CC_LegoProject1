package cc.srv.data;

import java.util.Date;

public class AuctionBid {
    private String bidderId;  // id of the user placing the bid
    private double amount;    // bid amount
    private Date bidTime;     // time when the bid was placed
    
    // Constructors
    public AuctionBid() {
        
    }
    
    
    public AuctionBid(String bidderId, double amount) {
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = new Date();
    }
    
    // Getters et Setters
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public Date getBidTime() { return bidTime; }
    public void setBidTime(Date bidTime) { this.bidTime = bidTime; }
}