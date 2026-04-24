package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    @Consumes(MediaType.WILDCARD)
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();
        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filtered = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(new ArrayList<>(all)).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", 400);
            err.put("error", "Bad Request");
            err.put("message", "Sensor 'id' is required.");
            return Response.status(400).entity(err).build();
        }
        if (store.getSensor(sensor.getId()) != null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", 409);
            err.put("error", "Conflict");
            err.put("message", "Sensor '" + sensor.getId() + "' already exists.");
            return Response.status(409).entity(err).build();
        }
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                "The roomId '" + sensor.getRoomId() + "' does not exist. Create the room first.");
        }
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }
        store.addSensor(sensor);
        store.getRoom(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        return Response
                .created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    @GET
    @Path("{sensorId}")
    @Consumes(MediaType.WILDCARD)
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }
        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("{sensorId}")
    @Consumes(MediaType.WILDCARD)
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }
        if (sensor.getRoomId() != null && store.getRoom(sensor.getRoomId()) != null) {
            store.getRoom(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }
        store.deleteSensor(sensorId);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("status", 200);
        msg.put("message", "Sensor '" + sensorId + "' deleted successfully.");
        return Response.ok(msg).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        if (store.getSensor(sensorId) == null) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }
        return new SensorReadingResource(sensorId);
    }
}