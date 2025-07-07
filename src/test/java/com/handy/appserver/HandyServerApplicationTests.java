package com.handy.appserver;

import com.handy.appserver.config.TestAwsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestAwsConfig.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
class HandyServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
