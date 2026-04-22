package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        List<SensorReading> history = store.getReadings(sensorId);
        return Response.ok(history).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        String status = sensor.getStatus();

        // Both MAINTENANCE and OFFLINE sensors cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' has status '" + status
                + "' and cannot accept new readings. Set the sensor to ACTIVE first.");
        }

        SensorReading newReading = new SensorReading(reading.getValue());
        store.addReading(sensorId, newReading);

        // Side effect: update the parent sensor's currentValue for data consistency
        sensor.setCurrentValue(newReading.getValue());

        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "message", "Reading recorded successfully.",
                        "reading", newReading,
                        "sensorCurrentValue", sensor.getCurrentValue()
                )).build();
    }
}