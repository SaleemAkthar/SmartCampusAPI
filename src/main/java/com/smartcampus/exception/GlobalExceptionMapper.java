package com.smartcampus.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        if (ex instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) ex;
            int status = wae.getResponse().getStatus();

            String errorMsg;
            switch (status) {
                case 404: errorMsg = "Not Found"; break;
                case 405: errorMsg = "Method Not Allowed"; break;
                case 415: errorMsg = "Unsupported Media Type"; break;
                case 400: errorMsg = "Bad Request"; break;
                default:  errorMsg = "Request Error"; break;
            }

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", status);
            error.put("error", errorMsg);
            error.put("message", wae.getMessage() != null ? wae.getMessage() : "An error occurred.");
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }

        LOGGER.log(Level.SEVERE, "Unexpected error: " + ex.getMessage(), ex);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred. Please contact the administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}