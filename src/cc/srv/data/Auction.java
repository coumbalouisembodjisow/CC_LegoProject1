package cc.srv.data;

import java.util.Date;
import java.util.*;
public class Auction {
    private String id;
    private String legoSetId;        // Référence au set Lego
    private String sellerId;         // Référence  au user vendeur
    private double basePrice;        // Prix de base (starting price)
    private Date closeDate;          // Date de fin de l'enchère
    private String status;           // "ACTIVE", "ENDED", "CANCELLED"
    private List<AuctionBid> bids;
    // Constructeurs
    public Auction() {
        this.bids = new ArrayList<>();
    }
    
    
    public Auction(String id, String legoSetId, String sellerId, double basePrice, Date closeDate) {
        this.id = id;
        this.legoSetId = legoSetId;
        this.sellerId = sellerId;
        this.basePrice = basePrice;
        this.closeDate = closeDate;
        this.status = "ACTIVE";
    }
    
    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLegoSetId() { return legoSetId; }
    public void setLegoSetId(String legoSetId) { this.legoSetId = legoSetId; }
    
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    
    public Date getCloseDate() { return closeDate; }
    public void setCloseDate(Date closeDate) { this.closeDate = closeDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

     public List<AuctionBid> getBids() { return bids; }
    public void setBids(List<AuctionBid> bids) { this.bids = bids; }
    
    // add a bid to the auction
    public void addBid(AuctionBid bid) {
        this.bids.add(bid);
    }
    
    // get the current winning bid
    public AuctionBid getCurrentWinningBid() {
        if (bids.isEmpty()) {
            return null;
        }
        return bids.stream()
                .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                .orElse(null);
    }
}