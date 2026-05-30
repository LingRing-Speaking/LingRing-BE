package com.lingring.domain.user.dao.dto;

import java.math.BigDecimal;

public interface UserProfileProjection {

    Long getId();

    String getNickname();

    String getProfileImage();

    String getLevel();

    BigDecimal getMannerTemperature();
}
