package cc.srv.data;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;
@Path("/test")
public class TestRessource {
@GET
@Path("/health")
public Response health() {
    return Response.ok("{\"status\": \"healthy\", \"version\": \"v2\"}").build();
}
}
