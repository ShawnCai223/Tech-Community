package com.shawnidea.community.service;

import com.shawnidea.community.repository.LikeRecordRepository;
import com.shawnidea.community.util.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LikeServiceTests {

    @Test
    void shouldPersistLikeAndSyncRedis() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RedisOperations<String, Object> redisOperations = mock(RedisOperations.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        LikeRecordRepository likeRecordRepository = mock(LikeRecordRepository.class);

        when(likeRecordRepository.exists(11, 1, 275)).thenReturn(false);
        when(likeRecordRepository.insertIgnore(11, 1, 275, 149)).thenReturn(1);
        when(redisOperations.opsForSet()).thenReturn(setOperations);
        when(redisOperations.opsForValue()).thenReturn(valueOperations);
        when(setOperations.isMember(RedisKeyUtil.getEntityLikeKey(1, 275), 11)).thenReturn(false);
        doAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(redisOperations);
        }).when(redisTemplate).execute(any(SessionCallback.class));

        LikeService likeService = new LikeService();
        ReflectionTestUtils.setField(likeService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(likeService, "likeRecordRepository", likeRecordRepository);

        likeService.like(11, 1, 275, 149);

        verify(setOperations).add(RedisKeyUtil.getEntityLikeKey(1, 275), 11);
        verify(valueOperations).increment(RedisKeyUtil.getUserLikeKey(149));
    }

    @Test
    void shouldReadLikeCountFromDatabaseWhenRedisMisses() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        LikeRecordRepository likeRecordRepository = mock(LikeRecordRepository.class);

        when(redisTemplate.hasKey(RedisKeyUtil.getEntityLikeKey(1, 275))).thenReturn(false);
        when(likeRecordRepository.countByEntity(1, 275)).thenReturn(2L);
        when(likeRecordRepository.findUserIdsByEntity(1, 275)).thenReturn(List.of(11, 12));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        LikeService likeService = new LikeService();
        ReflectionTestUtils.setField(likeService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(likeService, "likeRecordRepository", likeRecordRepository);

        long count = likeService.findEntityLikeCount(1, 275);

        assertEquals(2L, count);
        verify(setOperations).add(eq(RedisKeyUtil.getEntityLikeKey(1, 275)), eq(11), eq(12));
    }
}
