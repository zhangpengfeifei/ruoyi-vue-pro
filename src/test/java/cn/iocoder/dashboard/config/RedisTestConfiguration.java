package cn.iocoder.dashboard.config;

import com.github.fppt.jedismock.RedisServer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

@Configuration(proxyBeanMethods = false)
@Lazy(false) // 禁止延迟加载
@EnableConfigurationProperties(RedisProperties.class)
public class RedisTestConfiguration {

    /**
     * 创建模拟的 Redis Server 服务器
     */
    @Bean(destroyMethod = "stop")
    public RedisServer redisServer(RedisProperties properties) throws IOException {
        RedisServer redisServer = new RedisServer(properties.getPort());
        // TODO 芋艿：一次执行多个单元测试时，貌似创建多个 spring 容器，导致不进行 stop。这样，就导致端口被占用，无法启动。。。
        try {
            redisServer.start();
        } catch (Exception ignore) {}
        return redisServer;
    }

}
