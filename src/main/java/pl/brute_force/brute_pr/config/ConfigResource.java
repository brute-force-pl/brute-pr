package pl.brute_force.brute_pr.config;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/config")
public class ConfigResource {
    private final ConfigDao configDao;

    public ConfigResource(ConfigDao configDao) {
        this.configDao = configDao;
    }

    @GET
    @Path("/{projectKey}/{repoSlug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRepoConfig(@PathParam("projectKey") String projectKey, @PathParam("repoSlug") String repoSlug) {
        return Response.ok(configDao.getConfigForRepo(projectKey, repoSlug)).build();
    }

    @GET
    @Path("/{projectKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectConfig(@PathParam("projectKey") String projectKey) {
        return Response.ok(configDao.getConfigForProject(projectKey)).build();
    }

    @PUT
    @Path("/{projectKey}/{repoSlug}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setRepoConfig(@PathParam("projectKey") String projectKey, @PathParam("repoSlug") String repoSlug, Config config) {
        configDao.setConfigForRepo(projectKey, repoSlug, config);
        return Response.ok().build();
    }

    @PUT
    @Path("/{projectKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setProjectConfig(@PathParam("projectKey") String projectKey, Config config) {
        configDao.setConfigForProject(projectKey, config);
        return Response.ok().build();
    }
}
