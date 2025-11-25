package cc.srv;

import java.util.HashSet;
import java.util.Set;
import cc.srv.data.UserResource;
import cc.srv.data.AuctionResource;
import cc.srv.data.LegoSetResource;
import cc.srv.data.MediaResource;   
import cc.srv.data.TestRessource;

import jakarta.ws.rs.core.Application;

public class MainApplication extends Application {

    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        resources.add(UserResource.class);
        resources.add(AuctionResource.class);
        resources.add(LegoSetResource.class);
        resources.add(TestRessource.class);
        singletons.add(new MediaResource());
        
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
