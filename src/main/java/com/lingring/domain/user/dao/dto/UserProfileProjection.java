package com.lingring.domain.user.dao.dto;

import java.math.BigDecimal;

public interface UserProfileProjection {

    Long getId();

    String getNickname();

    String getLevel();

    BigDecimal getMannerTemperature();
}
