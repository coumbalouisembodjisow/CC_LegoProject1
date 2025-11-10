# Lego Platform Backend 

## Description

This project implements a backend service for a Lego auction application. The backend supports the creation and management of **Lego Sets**, **Auctions**, **Users**, and **Comments**. It uses Azure services to deploy, manage, and scale the backend efficiently.

## Architecture

- **Backend**: Java JAX-RS (RESTEasy) on Azure App Service
- **Database**: Azure Cosmos DB (NoSQL)
- **Cache**: Azure Redis Cache  
- **Storage**: Azure Blob Storage (media files)
- **Serverless**: Azure Functions (background tasks)

## Prerequisites

- **Java 21**
- **Maven**
- **Azure CLI**
- **Redis** (local development)


## Maven Configuration

This project uses **two separate Maven configurations** for different deployment targets:

### For Azure App Service (Main Backend)
```bash
# Rename for backend deployment
cp pomAzureAppService.xml pom.xml
mvn clean package azure-webapp:deploy
# Rename for functions deployment  
cp pomAzureFunctions.xml pom.xml
mvn clean package azure-functions:deploy 
```
Note: You must rename the appropriate POM file to pom.xml before each deployment.

### Azure Services Required
- Azure App Service - Hosts main REST API
- Azure Cosmos DB - Primary database
- Azure Redis Cache - Application caching
- Azure Blob Storage - Media file storage
- Azure Functions - Background tasks (auction closing, cleanup)


### API Endpoints
- **Users**
    - POST /rest/user - Create user
    - GET /rest/user/{id} - Get user
    - GET /rest/user/{id}/auctions - Get user's auctions
    - POST /rest/user/{id}/legosets/{legoSetId} - Add LegoSet to user

- **LegoSets**
    - POST /rest/legoset - Create LegoSet
    - GET /rest/legoset/{id} - Get LegoSet
    - GET /rest/legoset/any/recent - Get recent LegoSets
    - POST /rest/legoset/{id}/comment - Add comment
    - GET /rest/legoset/{id}/comments - Get comments

- **Auctions**
    - POST /rest/auction - Create auction
    - GET /rest/auction/any/recent - Get recent auctions
    - POST /rest/auction/{id}/bid - Place bid
    - GET /rest/auction/{id}/bids - Get bids

- **Media**
    - POST /rest/media - Upload media (images)
    - GET /rest/media/{id} - Download media

- **Configuration**
    - Set these environment variables in Azure App Service:
    - CACHE_ENABLED=true/false - Enable/disable Redis cache
    - COSMOS_ENDPOINT=your-cosmos-endpoint
    - REDIS_HOST=your-redis-host
    - BLOB_CONNECTION_STRING=your-blob-connection-string


### Load Testing
 - Load tests are located in the artillery/ directory:
```bash
    cd artillery
    artillery run workload1.yml
```

### Background Tasks (Azure Functions)
 - closeExpiredAuctions - Automatically closes expired auctions (Timer trigger)
 - garbageCollector - Cleans up old data (Timer trigger)
 - analyzeComment - Performs sentiment analysis on comments (HTTP trigger)

## Contributor
Name                             Email                                                                          Number
Coumba Louise Mbodji Sow     c.sow@campus.fct.unl.pt                                                              75921
                                        
