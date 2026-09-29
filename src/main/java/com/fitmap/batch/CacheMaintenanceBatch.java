package com.fitmap.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMaintenanceBatch {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 매일 새벽 3시: 고아 캐시 키 정리
     * TTL이 만료됐지만 메모리에 남아있는 키 패턴을 스캔해 삭제.
     * Redis TTL이 설정돼 있으면 자동 만료되지만,
     * 배치 데이터 교체(시설 데이터 갱신) 시 stale 캐시가 남을 수 있어 명시적으로 제거.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void evictStaleCaches() {
        log.info("[CacheMaintenance] stale 캐시 정리 시작");
        int count = 0;
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                count++;
            }
        }
        log.info("[CacheMaintenance] {} 개 캐시 초기화 완료", count);
    }

    /**
     * 6시간마다: 고아 Redis 키 스캔 (prefix 없는 잔여 키)
     * Spring Cache가 관리하지 않는 키가 누적되면 메모리 낭비.
     * 관리 대상 캐시 prefix 외 키 중 TTL이 -1(영구)인 것만 삭제.
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000)
    public void cleanOrphanKeys() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys == null || keys.isEmpty()) return;

        int removed = 0;
        for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key);
            // TTL = -1 : 만료 없는 영구 키 (Spring Cache 관리 대상 아닌 고아 키)
            // TTL = -2 : 이미 없는 키
            if (ttl != null && ttl == -1L && isOrphanKey(key)) {
                redisTemplate.delete(key);
                removed++;
            }
        }
        if (removed > 0) {
            log.info("[CacheMaintenance] 고아 키 {}개 삭제", removed);
        }
    }

    private boolean isOrphanKey(String key) {
        // Spring Cache가 생성하는 키는 "cacheName::..." 형태
        // Oah 라이브러리의 refresh token 키는 별도 prefix 사용 — 건드리지 않음
        return !key.contains("::") && !key.startsWith("refresh:") && !key.startsWith("signup:");
    }
}
