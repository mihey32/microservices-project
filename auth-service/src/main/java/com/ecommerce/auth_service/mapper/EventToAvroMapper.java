package com.ecommerce.auth_service.mapper;

import com.ecommerce.auth_service.dto.EmailNotificationEvent;
import com.ecommerce.auth_service.dto.UserCreatedEvent;


public final class EventToAvroMapper {

    private EventToAvroMapper() {
    }

    public static com.ecommerce.avro.UserCreatedEvent toAvro(UserCreatedEvent dto) {
        return com.ecommerce.avro.UserCreatedEvent.newBuilder()
                .setUserId(dto.userId())
                .setEmail(dto.email())
                .setCreatedAt(dto.createdAt().toEpochMilli())
                .build();
    }

    public static com.ecommerce.avro.EmailNotificationEvent toAvro(EmailNotificationEvent dto) {
        return com.ecommerce.avro.EmailNotificationEvent.newBuilder()
                .setUserId(dto.userId())
                .setEmail(dto.email())
                .setResetToken(dto.resetToken())
                .setCreatedAt(dto.createdAt().toEpochMilli())
                .build();
    }
}