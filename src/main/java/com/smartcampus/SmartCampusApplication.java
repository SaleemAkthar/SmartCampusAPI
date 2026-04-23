package com.smartcampus;

import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.ResourceNotFoundExceptionMapper;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);
        register(GlobalExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(ResourceNotFoundExceptionMapper.class);
        register(RoomNotEmptyExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(LoggingFilter.class);
        register(JacksonFeature.class);
    }
}