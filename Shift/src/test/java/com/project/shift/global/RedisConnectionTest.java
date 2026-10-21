package com.project.shift.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Redis에 값을 넣고 다시 꺼낼 수 있다")
    void canWriteAndRead() {
        redisTemplate.opsForValue().set("connection-test", "ok");

        assertThat(redisTemplate.opsForValue().get("connection-test")).isEqualTo("ok");

        redisTemplate.delete("connection-test");
    }
}