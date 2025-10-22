package cc.srv.data;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource for managing media files, such as images.
 */
@Path("/media")
public class MediaResource
{
	/**
	 * Post a new image.The id of the image is its hash.
	 */
	@POST
	@Path("/")
	@Consumes("image/*")
	@Produces(MediaType.APPLICATION_JSON)
	public String upload(@HeaderParam("Content-Type") String cntType, byte[] contents) {
		throw new ServiceUnavailableException();
	}

	/**
	 * Return the contents of an image. Throw an appropriate error message if
	 * id does not exist.
	 */
	@GET
	@Path("/{id}")
	@Produces("media/*")
	public Response download(@PathParam("id") String id) {
		throw new ServiceUnavailableException();
	}

	/**
	 * Lists the ids of images stored.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> list() {
		throw new ServiceUnavailableException();
	}
}
