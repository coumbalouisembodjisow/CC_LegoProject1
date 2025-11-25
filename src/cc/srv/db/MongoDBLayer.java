package cc.srv.db;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import cc.srv.data.User;
import cc.srv.data.Auction;
import cc.srv.data.LegoSet;
import cc.srv.data.Comment;
import cc.srv.data.AuctionBid;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import java.util.Iterator;
import java.util.Collections;


public class MongoDBLayer {
    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection<Document> users;
    private MongoCollection<Document> auctions;
    private MongoCollection<Document> legosets;
    private MongoCollection<Document> comments;
    
    private static MongoDBLayer instance;

    // Configuration MongoDB
    private static final String MONGODB_URI = System.getenv("MONGODB_URI");
    private static final String DB_NAME = "legodb";

    public static synchronized MongoDBLayer getInstance() {
        if (instance != null)
            return instance;

        MongoClient client = MongoClients.create(MONGODB_URI);
        instance = new MongoDBLayer(client);
        return instance;
    }

    public MongoDBLayer(MongoClient client) {
        this.client = client;
    }

    private synchronized void init() {
        if (db != null)
            return;
        db = client.getDatabase(DB_NAME);
        users = db.getCollection("Users");
        auctions = db.getCollection("Auctions");
        legosets = db.getCollection("LegoSets");
        comments = db.getCollection("Comments");
    }

    // --------------------- User methods ------------------- //
    
    public boolean delUserById(String id) {
        init();
        try {
            DeleteResult result = users.deleteOne(eq("_id", id));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error deleting user: " + e.getMessage());
            return false;
        }
    }
    
    public boolean delUser(User user) {
        return delUserById(user.getId());
    }
    
    public String putUser(User user) {
        init();
        try {
            Document doc = new Document();
            if (user.getId() != null && !user.getId().isEmpty()) {
                doc.put("_id", user.getId());
            }
            doc.put("name", user.getName());
            doc.put("password", user.getPassword());
            doc.put("photoMediaId", user.getPhotoMediaId());
            doc.put("ownedLegoSets", user.getOwnedLegoSets());
            
            users.insertOne(doc);
            return doc.getString("_id");
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error creating user: " + e.getMessage());
            return null;
        }
    }
    
    public User getUserById(String id) {
        init();
        try {
            Document doc = users.find(eq("_id", id)).first();
            return doc != null ? documentToUser(doc) : null;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error getting user by id: " + e.getMessage());
            return null;
        }
    }

    public List<User> getUsers() {
        init();
        List<User> userList = new ArrayList<>();
        try {
            for (Document doc : users.find()) {
                userList.add(documentToUser(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting users: " + e.getMessage());
        }
        return userList;
    }

    // Test de connexion
    public String testConnection() {
        try {
            init();
            db.runCommand(new Document("ping", 1));
            return "MongoDB connection OK! Database: " + DB_NAME;
        } catch (Exception e) {
            return "MongoDB connection FAILED: " + e.getMessage();
        }
    }
    
    // Compter les utilisateurs
    public long countUsers() {
        init();
        return users.countDocuments();
    }
    
    // Mettre à jour un utilisateur
    public boolean updateUser(User user) {
        init();
        try {
            Document doc = new Document();
            doc.put("name", user.getName());
            doc.put("password", user.getPassword());
            doc.put("photoMediaId", user.getPhotoMediaId());
            doc.put("ownedLegoSets", user.getOwnedLegoSets());
            
            UpdateResult result = users.replaceOne(eq("_id", user.getId()), doc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error updating user: " + e.getMessage());
            return false;
        }
    }

    // --------------------- Auction methods ------------------- //
    
    public String putAuction(Auction auction) {
        init();
        try {
            Document doc = new Document();
            if (auction.getId() != null && !auction.getId().isEmpty()) {
                doc.put("_id", auction.getId());
            }
            doc.put("legoSetId", auction.getLegoSetId());
            doc.put("sellerId", auction.getSellerId());
            doc.put("basePrice", auction.getBasePrice());
            doc.put("closeDate", auction.getCloseDate());
            doc.put("status", auction.getStatus());
            doc.put("bids", auction.getBids());
            doc.put("highestBid", auction.getCurrentWinningBid());

            
            auctions.insertOne(doc);
            return doc.getString("_id");
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error creating auction: " + e.getMessage());
            return null;
        }
    }
    
    public Auction getAuctionById(String id) {
        init();
        try {
            Document doc = auctions.find(eq("_id", id)).first();
            return doc != null ? documentToAuction(doc) : null;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error getting auction by id: " + e.getMessage());
            return null;
        }
    }
    
    public List<Auction> getAuctions() {
        init();
        List<Auction> auctionList = new ArrayList<>();
        try {
            for (Document doc : auctions.find()) {
                auctionList.add(documentToAuction(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting auctions: " + e.getMessage());
        }
        return auctionList;
    }
    
    public List<Auction> getActiveAuctions() {
        init();
        List<Auction> auctionList = new ArrayList<>();
        try {
            for (Document doc : auctions.find(eq("status", "ACTIVE"))) {
                auctionList.add(documentToAuction(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting active auctions: " + e.getMessage());
        }
        return auctionList;
    }
    
    public boolean updateAuction(Auction auction) {
        init();
        try {
            Document doc = new Document();
            doc.put("legoSetId", auction.getLegoSetId());
            doc.put("sellerId", auction.getSellerId());
            doc.put("basePrice", auction.getBasePrice());
            doc.put("closeDate", auction.getCloseDate());
            doc.put("status", auction.getStatus());
            doc.put("bids", auction.getBids());
            doc.put("highestBid", auction.getCurrentWinningBid());
            
            UpdateResult result = auctions.replaceOne(eq("_id", auction.getId()), doc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error updating auction: " + e.getMessage());
            return false;
        }
    }
    
    public boolean delAuctionById(String id) {
        init();
        try {
            DeleteResult result = auctions.deleteOne(eq("_id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error deleting auction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get auctions by seller (userId)
     */
    public List<Auction> getAuctionsByUser(String userId) {
        List<Auction> userAuctions = new ArrayList<>();
        try {
            for (Document doc : auctions.find(eq("sellerId", userId))) {
                userAuctions.add(documentToAuction(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error retrieving auctions for user " + userId + ": " + e.getMessage());
        }
        return userAuctions;
    }

// Get auctions by legoSetId
    public List<Auction> getAuctionsByLegoSetId(String legoSetId) {
    init();
    try {
        List<Auction> auctionList = new ArrayList<>();
        
        //  Filtre par legoSetId
        for (Document doc : auctions.find(eq("legoSetId", legoSetId))) {
            auctionList.add(documentToAuction(doc));
        }
        return auctionList;
    } catch (Exception e) {
        Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting auctions by legoSetId: " + e.getMessage());
        return Collections.emptyList();
    }
}

public Iterator<Auction> getRecentAuctions(int start, int limit) {
        init();
        try {
            List<Auction> auctionList = new ArrayList<>();
            for (Document doc : auctions.find()
                                    .sort(new Document("closeDate", -1))
                                    .skip(start)
                                    .limit(limit)) {
                auctionList.add(documentToAuction(doc));
            }
            return auctionList.iterator();
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting auctions: " + e.getMessage());
            return Collections.emptyIterator();
        }
}


    // --------------------- LegoSet methods ------------------- //
    
    public String putLegoSet(LegoSet legoSet) {
        init();
        try {
            Document doc = new Document();
            if (legoSet.getId() != null && !legoSet.getId().isEmpty()) {
                doc.put("_id", legoSet.getId());
            }
            doc.put("name", legoSet.getName());
            doc.put("description", legoSet.getDescription());
            doc.put("photoMediaIds", legoSet.getPhotoMediaIds());
            doc.put("createdAt", legoSet.getCreatedAt());
            legosets.insertOne(doc);
            return doc.getString("_id");
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error creating lego set: " + e.getMessage());
            return null;
        }
    }
    
    public LegoSet getLegoSetById(String id) {
        init();
        try {
            Document doc = legosets.find(eq("_id", id)).first();
            return doc != null ? documentToLegoSet(doc) : null;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error getting lego set by id: " + e.getMessage());
            return null;
        }
    }
    
    public List<LegoSet> getLegoSets() {
        init();
        List<LegoSet> legoSetList = new ArrayList<>();
        try {
            for (Document doc : legosets.find()) {
                legoSetList.add(documentToLegoSet(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting lego sets: " + e.getMessage());
        }
        return legoSetList;
    }
    
    public boolean updateLegoSet(LegoSet legoSet) {
        init();
        try {
            Document doc = new Document();
            doc.put("name", legoSet.getName());
            doc.put("description", legoSet.getDescription());
            doc.put("photoMediaIds", legoSet.getPhotoMediaIds());
            doc.put("createdAt", legoSet.getCreatedAt());
            UpdateResult result = legosets.replaceOne(eq("_id", legoSet.getId()), doc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error updating lego set: " + e.getMessage());
            return false;
        }
    }
    
    public boolean delLegoSetById(String id) {
        init();
        try {
            DeleteResult result = legosets.deleteOne(eq("_id", id));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error deleting lego set: " + e.getMessage());
            return false;
        }
    }
    
    public long countLegoSets() {
        init();
        return legosets.countDocuments();
    }

    public Iterator<LegoSet> getRecentLegoSets(int start, int limit) {
        init();
        try {
            List<LegoSet> legoSetList = new ArrayList<>();
            for (Document doc : legosets.find()
                                    .sort(new Document("createdAt", -1))
                                    .skip(start)
                                    .limit(limit)) {
                legoSetList.add(documentToLegoSet(doc));
            }
            return legoSetList.iterator();
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting recent lego sets: " + e.getMessage());
            return Collections.emptyIterator();
        }
}

    // --------------------- Comments Methods ------------------- //
    
    public String putComment(Comment comment) {
        init();
        try {
            Document doc = new Document();
            if (comment.getId() != null && !comment.getId().isEmpty()) {
                doc.put("_id", comment.getId());
            }
            doc.put("legoSetId", comment.getLegoSetId());
            doc.put("userId", comment.getUserId());
            doc.put("content", comment.getContent());
            comments.insertOne(doc);
            return doc.getString("_id");
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error creating comment: " + e.getMessage());
            return null;
        }
    }
    
    public boolean updateComment(Comment comment) {
        init();
        try {
            Document doc = new Document();
            doc.put("legoSetId", comment.getLegoSetId());
            doc.put("userId", comment.getUserId());
            doc.put("content", comment.getContent());
            UpdateResult result = comments.replaceOne(eq("_id", comment.getId()), doc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error updating comment: " + e.getMessage());
            return false;
        }
    }
    
    public List<Comment> getCommentsByLegoSetId(String legoSetId) {
        init();
        List<Comment> commentList = new ArrayList<>();
        try {
            for (Document doc : comments.find(eq("legoSetId", legoSetId))) {
                commentList.add(documentToComment(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting comments for lego set: " + e.getMessage());
        }
        return commentList;
    }
    
    public boolean delCommentById(String id) {
        init();
        try {
            DeleteResult result = comments.deleteOne(eq("_id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).warning("Error deleting comment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère tous les comments d'un user
     */
    public List<Comment> getCommentsByUser(String userId) {
        List<Comment> userComments = new ArrayList<>();
        try {
            for (Document doc : comments.find(eq("userId", userId))) {
                userComments.add(documentToComment(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error retrieving comments for user " + userId + ": " + e.getMessage());
        }
        return userComments;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }

    // Récupère les LegoSets les plus aimés
    public List<LegoSet> getMostLikedLegoSets(int limit) {
        List<LegoSet> legoSetList = new ArrayList<>();
        try {
            for (Document doc : legosets.find(and(exists("sentimentScore"), eq("isLiked", true)))
                                       .sort(new Document("sentimentScore", -1))
                                       .limit(limit)) {
                legoSetList.add(documentToLegoSet(doc));
            }
        } catch (Exception e) {
            Logger.getLogger(MongoDBLayer.class.getName()).severe("Error getting most liked lego sets: " + e.getMessage());
        }
        return legoSetList;
    }

    // --------------------- Conversion methods ------------------- //
    
    private User documentToUser(Document doc) {
        if (doc == null) return null;
        User user = new User();
        user.setId(doc.getString("_id"));
        user.setName(doc.getString("name"));
        user.setPassword(doc.getString("password"));
        user.setPhotoMediaId(doc.getString("photoMediaId"));
        user.setOwnedLegoSets(doc.getList("OwnedLegoSets",String.class) != null ? 
                              new java.util.HashSet<>(doc.getList("OwnedLegoSets", String.class)) : 
                              new java.util.HashSet<>());
        return user;
    }
    
    private Auction documentToAuction(Document doc) {
        if (doc == null) return null;
        Auction auction = new Auction();
        auction.setId(doc.getString("_id"));
        auction.setLegoSetId(doc.getString("legoSetId"));
        auction.setSellerId(doc.getString("sellerId"));
        auction.setBasePrice(doc.getDouble("basePrice"));
        auction.setCloseDate(doc.getDate("closeDate"));
        auction.setStatus(doc.getString("status"));
        auction.setBids(doc.getList("bids", AuctionBid.class));
        auction.updateWinningBid();
        return auction;
    }
    
    private LegoSet documentToLegoSet(Document doc) {
        if (doc == null) return null;
        LegoSet legoSet = new LegoSet();
        legoSet.setId(doc.getString("_id"));
        legoSet.setName(doc.getString("name"));
        legoSet.setDescription(doc.getString("description"));
        legoSet.addPhotoMediaId(doc.getString("photoMediaIds"));
        legoSet.setCreatedAt(doc.getDate("createdAt"));
        return legoSet;
    }
    
    private Comment documentToComment(Document doc) {
        if (doc == null) return null;
        Comment comment = new Comment(); 
        comment.setId(doc.getString("_id"));
        comment.setLegoSetId(doc.getString("legoSetId"));
        comment.setUserId(doc.getString("userId"));
        comment.setContent(doc.getString("content"));
        return comment;
    }
}
