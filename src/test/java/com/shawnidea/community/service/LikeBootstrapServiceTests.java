package com.shawnidea.community.service;

import com.shawnidea.community.util.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LikeBootstrapServiceTests {

    @Test
    void shouldSeedMissingLikeKeysAndRebuildUserLikeCounts() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.keys("like:entity:*")).thenReturn(Set.of());
        when(redisTemplate.keys("like:user:*")).thenReturn(Set.of("like:user:149"));
        when(setOperations.members(any())).thenReturn(null);

        LikeBootstrapService service = spy(new LikeBootstrapService());
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minEntityKeys", 20);

        doReturn(List.of(11, 12, 13, 14, 149)).when(service).loadActiveUserIds();
        doReturn(List.of(
                new LikeBootstrapService.PostSeed(274, 149, 0, 1, 37),
                new LikeBootstrapService.PostSeed(275, 11, 1, 1, 12)
        )).when(service).loadPostSeeds();
        doReturn(List.of(
                new LikeBootstrapService.CommentSeed(104, 146, 1, 274, 0),
                new LikeBootstrapService.CommentSeed(165, 111, 2, 104, 149)
        )).when(service).loadCommentSeeds();

        service.bootstrapIfNeeded();

        verify(setOperations).add(RedisKeyUtil.getEntityLikeKey(1, 274), 11, 12, 13, 14);
        verify(setOperations).add(RedisKeyUtil.getEntityLikeKey(2, 104), 149, 11, 12, 13);
        verify(redisTemplate).delete(Set.of("like:user:149"));
        verify(valueOperations).set(eq(RedisKeyUtil.getUserLikeKey(149)), eq(4));
        verify(valueOperations).set(eq(RedisKeyUtil.getUserLikeKey(11)), eq(4));
        verify(valueOperations).set(eq(RedisKeyUtil.getUserLikeKey(146)), eq(4));
        verify(valueOperations).set(eq(RedisKeyUtil.getUserLikeKey(111)), eq(4));
    }

    @Test
    void shouldSkipBootstrapWhenEnoughLikeKeysAlreadyExist() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.keys("like:entity:*")).thenReturn(Set.of("a", "b", "c"));

        LikeBootstrapService service = spy(new LikeBootstrapService());
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "minEntityKeys", 3);

        service.bootstrapIfNeeded();

        verify(service, never()).loadActiveUserIds();
        verify(redisTemplate, never()).opsForSet();
    }
}
