package com.blackdurumi.searchengine.service

import com.blackdurumi.searchengine.model.Trie
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class TrieService(
    private val trie: Trie,
    private val trieLoader: TrieLoader,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun increaseSearchCount(keyword: String) {
        // 1. Redis count 증가
        redisTemplate.opsForZSet()
            .incrementScore("search:keyword_count", keyword, 1.0)

        // 2. Trie 업데이트 (lazy loading 후 사용할 수 있음)
        trie.updateCount(keyword)
    }

    fun autoComplete(prefix: String, limit: Int): List<String> {
        // 1. Trie에 prefix가 존재하는지 확인
        if (!trie.containsPrefix(prefix)) {
            // Trie에 prefix subtree 없음 → Redis에서 로딩
            trieLoader.loadPrefix(prefix)
        }

        // 2. Trie에서 자동완성 검색
        return trie.search(prefix, limit)
    }
}