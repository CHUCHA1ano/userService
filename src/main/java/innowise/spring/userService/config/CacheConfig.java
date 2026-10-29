package innowise.spring.userService.config;

import innowise.spring.userService.model.dto.CardResponseDto;
import innowise.spring.userService.model.dto.UserResponseDto;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(JsonMapper jsonMapper) {
        JavaType cardListType = jsonMapper.getTypeFactory()
                .constructCollectionType(List.class, CardResponseDto.class);

        RedisCacheConfiguration usersCache = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(jsonMapper, UserResponseDto.class)));

        RedisCacheConfiguration cardsCache = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(jsonMapper, cardListType)));

        return builder -> builder
                .withCacheConfiguration("users", usersCache)
                .withCacheConfiguration("cards", cardsCache);
    }
}
