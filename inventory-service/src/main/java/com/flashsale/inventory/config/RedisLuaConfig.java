package com.flashsale.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisLuaConfig {

    /**
     * RedisScript bean for atomic stock reservation loaded from reserve_stock.lua.
     */
    @Bean(name = "reserveStockScript")
    public RedisScript<Long> reserveStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/reserve_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * RedisScript bean for atomic stock release loaded from release_stock.lua.
     */
    @Bean(name = "releaseStockScript")
    public RedisScript<Long> releaseStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/release_stock.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
