package com.lingring.global.config;

import com.lingring.global.util.SystemDateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@TestContainersTest
@Import({DataInitializer.class, JpaAuditingConfig.class, SystemDateTimeProvider.class})
public abstract class RepositoryTestHelper {

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer.deleteAll();
    }

}
