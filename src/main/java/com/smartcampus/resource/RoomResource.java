package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", 400);
            err.put("error", "Bad Request");
            err.put("message", "Room 'id' is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }
        if (store.getRoom(room.getId()) != null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", 409);
            err.put("error", "Conflict");
            err.put("message", "Room '" + room.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(err).build();
        }
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        store.addRoom(room);
        return Response
                .created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted — it still has "
                + room.getSensorIds().size() + " sensor(s) assigned to it."
            );
        }
        store.deleteRoom(roomId);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("status", 200);
        msg.put("message", "Room '" + roomId + "' deleted successfully.");
        return Response.ok(msg).build();
    }
}