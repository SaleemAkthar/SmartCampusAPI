package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        final URI BASE_URI = URI.create("http://0.0.0.0:8080/api/v1/");

        SmartCampusApplication app = new SmartCampusApplication();
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, app, false);
        server.start();

        LOGGER.info("====================================");
        LOGGER.info("Smart Campus API is RUNNING!");
        LOGGER.info("Rooms:   http://localhost:8080/api/v1/rooms");
        LOGGER.info("Sensors: http://localhost:8080/api/v1/sensors");
        LOGGER.info("====================================");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        Thread.currentThread().join();
    }
}