package cc.srv.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

import cc.srv.data.User;
import cc.srv.data.Auction;
import cc.srv.data.LegoSet;
import cc.srv.data.Comment;
import cc.srv.utils.EnvConfig;

public class CosmosDBLayer {
	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
    private CosmosContainer auctions;
	private CosmosContainer legosets;
	private CosmosContainer comments;
	
	private static CosmosDBLayer instance;

	// Configuration - to be moved to EnvConfig or similar
	private static final String CONNECTION_URL = "https://legoproject-75291.documents.azure.com:443/";
    private static final String DB_KEY = "KaibSLbABsKn9XzPb2cNg8ESoBH9PbYHKyMQlOiQmmwNtkAz7dmjjnabkU572RtJrSvCnaRDwuQkACDbzx5G4w==";
    private static final String DB_NAME = "legoDB";


	public static synchronized CosmosDBLayer getInstance() {
		if( instance != null)
			return instance;

		CosmosClient client = new CosmosClientBuilder()
		         .endpoint(CONNECTION_URL)
		         .key(DB_KEY)
		         //.directMode()
		         .gatewayMode()		
		         // replace by .directMode() for better performance
		         .consistencyLevel(ConsistencyLevel.SESSION)
		         .connectionSharingAcrossClientsEnabled(true)
		         .contentResponseOnWriteEnabled(true)
		         .buildClient();
		instance = new CosmosDBLayer( client);
		return instance;
		
	}
	
	
	
	
	public CosmosDBLayer(CosmosClient client) {
		this.client = client;
	}
	
	private synchronized void init() {
		if( db != null)
			return;
		db = client.getDatabase(DB_NAME);
		users = db.getContainer("Users");
        auctions = db.getContainer("Auctions");
		legosets = db.getContainer("LegoSets");
		comments = db.getContainer("Comments");
		
	}
	// --------------------- User methods ------------------- //
	public CosmosItemResponse<Object> delUserById(String id) {
		init();
		PartitionKey key = new PartitionKey( id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<Object> delUser(User user) {
		init();
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}
	
	public CosmosItemResponse<User> putUser(User user) {
		init();
		return users.createItem(user);
	}
	
	public CosmosPagedIterable<User> getUserById( String id) {
		init();
		return users.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(), User.class);
	}

	public CosmosPagedIterable<User> getUsers() {
		init();
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), User.class);
	}

	// AJOUT: Méthode pour tester la connexion
	public String testConnection() {
		try {
			init();
			// Test simple - lire la base de données
			db.read();
			return "Cosmos DB connection OK! Database: " + DB_NAME;
		} catch (Exception e) {
			return "Cosmos DB connection FAILED: " + e.getMessage();
		}
	}
	
	//  Méthode pour compter les utilisateurs
	public int countUsers() {
		try {
			init();
			int count = 0;
			CosmosPagedIterable<Object> items = users.queryItems(
				"SELECT VALUE COUNT(1) FROM c", 
				new CosmosQueryRequestOptions(), 
				Object.class
			);
			
			for (Object item : items) {
				count = ((Number) item).intValue();
			}
			return count;
		} catch (Exception e) {
			System.err.println("Error counting users: " + e.getMessage());
			return -1;
		}
	}
	
	//  Méthode updateUser manquante
	public CosmosItemResponse<User> updateUser(User user) {
		init();
		return users.upsertItem(user);
	}

    // --------------------- Auction methods ------------------- //
     public CosmosItemResponse<Auction> putAuction(Auction auction) {
        init();
        return auctions.createItem(auction);
    }
    
    public CosmosPagedIterable<Auction> getAuctionById(String id) {
        init();
        return auctions.queryItems("SELECT * FROM auctions WHERE auctions.id=\"" + id + "\"", 
                                  new CosmosQueryRequestOptions(), Auction.class);
    }
    
    public CosmosPagedIterable<Auction> getAuctions() {
        init();
        return auctions.queryItems("SELECT * FROM auctions", 
                                  new CosmosQueryRequestOptions(), Auction.class);
    }
    
    public CosmosPagedIterable<Auction> getActiveAuctions() {
        init();
        return auctions.queryItems("SELECT * FROM auctions WHERE auctions.status=\"ACTIVE\"", 
                                  new CosmosQueryRequestOptions(), Auction.class);
    }
    
    public CosmosItemResponse<Auction> updateAuction(Auction auction) {
        init();
        return auctions.upsertItem(auction);
    }
    
    public CosmosItemResponse<Object> delAuctionById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return auctions.deleteItem(id, key, new CosmosItemRequestOptions());
    }

	// --------------------- LegoSet methods ------------------- //
    
    public CosmosItemResponse<LegoSet> putLegoSet(LegoSet legoSet) {
        init();
        return legosets.createItem(legoSet);
    }
    
    public CosmosPagedIterable<LegoSet> getLegoSetById(String id) {
        init();
        return legosets.queryItems("SELECT * FROM legosets WHERE legosets.id=\"" + id + "\"", 
                                  new CosmosQueryRequestOptions(), LegoSet.class);
    }
    
    public CosmosPagedIterable<LegoSet> getLegoSets() {
        init();
        return legosets.queryItems("SELECT * FROM legosets", 
                                  new CosmosQueryRequestOptions(), LegoSet.class);
    }
    
    public CosmosItemResponse<LegoSet> updateLegoSet(LegoSet legoSet) {
        init();
        return legosets.upsertItem(legoSet);
    }
    
    public CosmosItemResponse<Object> delLegoSetById(String id) {
        init();
        PartitionKey key = new PartitionKey(id);
        return legosets.deleteItem(id, key, new CosmosItemRequestOptions());
    }
    
    public int countLegoSets() {
        init();
        int count = 0;
        CosmosPagedIterable<Object> items = legosets.queryItems(
            "SELECT VALUE COUNT(1) FROM c", 
            new CosmosQueryRequestOptions(), 
            Object.class
        );
        for (Object item : items) {
            count = ((Number) item).intValue();
        }
        return count;
    }

	// --------------------- Comments Methods ------------------- //
	public CosmosItemResponse<Comment> putComment(Comment comment) {
		init();
		return comments.createItem(comment);
	}
	
	public CosmosPagedIterable<Comment> getCommentsByLegoSetId(String legoSetId) {
		init();
		return comments.queryItems("SELECT * FROM comments WHERE comments.legoSetId=\"" + legoSetId + "\"", 
								  new CosmosQueryRequestOptions(), Comment.class);
	}
	
	public CosmosItemResponse<Object> delCommentById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return comments.deleteItem(id, key, new CosmosItemRequestOptions());
	}
	public void close() {
		client.close();
	}

}