package com.handy.appserver;

import com.handy.appserver.config.TestAwsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestAwsConfig.class)
class HandyServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
