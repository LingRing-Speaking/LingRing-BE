package com.lingring.domain.matching.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidRoomIdGenerator implements RoomIdGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
