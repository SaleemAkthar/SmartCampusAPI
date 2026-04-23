# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and an embedded Grizzly HTTP server.

**Module:** 5COSC022W Client-Server Architectures  
**Base URL:** `http://localhost:8080/api/v1`

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api

# 2. Build the project
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api-1.0.0.jar
```

Server starts at: http://localhost:8080/api/v1

---

## Sample curl Commands

### 1. Discovery
```bash
curl -X GET http://localhost:8080/api/v1/info
```

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ROOM-001","name":"Seminar Room A","capacity":25}'
```

### 4. Delete a room (409 if sensors exist)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Create sensor with invalid roomId (422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-ROOM"}'
```

### 7. Post a reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.5}'
```

### 8. Post reading to MAINTENANCE sensor (403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":5.0}'
```

### 9. Get reading history
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## API Overview

| Resource | Path |
|---|---|
| Discovery | GET /api/v1/info |
| All Rooms | GET /api/v1/rooms |
| Create Room | POST /api/v1/rooms |
| Get Room | GET /api/v1/rooms/{roomId} |
| Delete Room | DELETE /api/v1/rooms/{roomId} |
| All Sensors | GET /api/v1/sensors |
| Filter Sensors | GET /api/v1/sensors?type=CO2 |
| Create Sensor | POST /api/v1/sensors |
| Readings | GET/POST /api/v1/sensors/{id}/readings |

---

## Report — Answers to Coursework Questions

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a new instance of every resource class for each incoming HTTP request, making resources request-scoped. This means every call to /api/v1/rooms creates a fresh RoomResource object. The advantage is that there are no shared instance variables between requests, eliminating many concurrency issues at the resource class level.

However, because our data lives in the DataStore singleton (shared across all requests and threads), we must protect it from race conditions. If two requests simultaneously POST a new room, both could read the map, see no conflict, and both insert — potentially overwriting each other. To prevent this, the DataStore uses ConcurrentHashMap, which is thread-safe for individual operations. For more complex read-then-write sequences, putIfAbsent() is used to ensure atomic operations and prevent data loss or corruption.

---

### Part 1.2 — HATEOAS

HATEOAS is the principle that API responses should include links to related resources and available actions, making the API self-describing and navigable. For example, our GET /api/v1 discovery response includes a _links object pointing to /api/v1/rooms and /api/v1/sensors.

Compared to static documentation, HATEOAS benefits client developers by allowing them to discover the API dynamically at runtime rather than hardcoding URLs. If resource paths change in a new API version, clients following the embedded links will adapt automatically without requiring code changes. This reduces tight coupling between client and server, making the system more resilient to change.

---

### Part 2.1 — ID-only vs Full Object in List Responses

Returning only IDs in a list (e.g., ["LIB-301", "LAB-101"]) minimises payload size and network bandwidth, which is beneficial with thousands of rooms. However, it forces the client to make N additional HTTP requests to fetch details for each ID — the N+1 problem — increasing latency and server load.

Returning full objects increases payload size but eliminates follow-up requests. In this implementation, full room objects are returned for maximum usability. For very large datasets, pagination with full objects per page is the accepted industry standard.

---

### Part 2.2 — DELETE Idempotency

The DELETE operation is idempotent in HTTP terms — sending the same DELETE request multiple times produces the same server state. After the first successful DELETE, the room is removed. A second DELETE on the same room ID returns 404 Not Found.

This implementation is idempotent in outcome — the room remains absent after any number of DELETE calls. The response code differs (200 first, 404 after), but the server state does not change after the first deletion. This is the correct RESTful behaviour for DELETE.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS to only accept requests with Content-Type: application/json. If a client sends text/plain or application/xml, JAX-RS rejects the request before it reaches the method body and automatically returns HTTP 415 Unsupported Media Type.

This is handled entirely by the JAX-RS runtime with no manual checking needed in application code. It acts as a first layer of input validation at the framework level.

---

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using @QueryParam (e.g., GET /sensors?type=CO2) is superior to path parameters (e.g., /sensors/type/CO2) for filtering because:

1. **Semantics:** The path identifies a resource. Query parameters modify how that collection is presented.
2. **Optionality:** Query parameters are naturally optional — without them, all sensors are returned.
3. **Composability:** Multiple filters combine naturally: ?type=CO2&status=ACTIVE.
4. **REST Convention:** RFC standards consistently use query strings for filtering collections.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern allows a resource method to delegate routing to a separate dedicated class. In SensorResource, the method annotated with @Path("{sensorId}/readings") returns a new SensorReadingResource instance, and JAX-RS dispatches the request to that class.

This improves maintainability because each class has a single responsibility. SensorResource manages sensors, SensorReadingResource manages readings. Classes are smaller, easier to test in isolation, and new sub-resources can be added without modifying the parent class.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Referenced Resource

When a client POSTs a sensor with a roomId that does not exist, the request is syntactically valid — the JSON is well-formed and the endpoint exists. The problem is a semantic validation failure inside the payload.

HTTP 404 would incorrectly imply the endpoint /api/v1/sensors was not found. HTTP 422 Unprocessable Entity is more accurate because it signals: the server understood the request and found the endpoint, but the content contains a logical error it cannot process. It helps client developers understand they need to fix their payload (create the room first) rather than their URL.

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security vulnerability:

1. **Internal Path Disclosure:** Stack traces reveal package names and class names, exposing the application's internal architecture.
2. **Library Version Disclosure:** Stack traces show exact library versions (e.g., jersey-server-2.41). Attackers can search for known CVEs for those versions.
3. **Logic Exposure:** Line numbers and method call sequences reveal how the application processes data, helping attackers find injection points.
4. **Technology Fingerprinting:** Knowing the exact technology stack allows targeted attacks.

The GlobalExceptionMapper prevents this by catching every Throwable, logging full details server-side only, and returning a clean generic message to the client.

---

### Part 5.5 — Why Filters are Better Than Per-Method Logging

Inserting Logger.info() manually into every resource method violates the DRY principle and the Single Responsibility Principle. Logging is a cross-cutting concern that applies uniformly to all endpoints.

Using ContainerRequestFilter and ContainerResponseFilter provides four key advantages:
1. If the log format changes, only one class needs updating.
2. Every request is automatically logged — no risk of forgetting a new endpoint.
3. Resource classes stay clean and focused on business logic only.
4. All log entries follow a consistent format, making monitoring easier.