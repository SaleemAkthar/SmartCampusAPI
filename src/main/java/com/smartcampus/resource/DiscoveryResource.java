package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("info")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1/info");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("api", "Smart Campus Sensor & Room Management API");
        metadata.put("version", "1.0.0");
        metadata.put("status", "RUNNING");
        metadata.put("contact", "admin@smartcampus.ac.uk");
        metadata.put("resources", resources);
        metadata.put("_links", links);

        return Response.ok(metadata).build();
    }
}