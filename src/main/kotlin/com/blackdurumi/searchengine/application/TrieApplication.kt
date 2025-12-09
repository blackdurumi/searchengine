package com.blackdurumi.searchengine.application

import com.blackdurumi.searchengine.model.Trie
import com.blackdurumi.searchengine.service.TrieLoader
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class TrieApplication(
    private val trie: Trie,
    private val trieLoader: TrieLoader,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    fun increaseSearchCount(keyword: String) {
        val zsetOps = redisTemplate.opsForZSet()

        // 1. 기존 count 조회 (없으면 0)
        val currentCount =
            zsetOps.score("search:keyword_count", keyword)?.toInt() ?: 0

        // 2. 처음 본 키워드면 keywords ZSET에 추가
        if (currentCount == 0) {
            zsetOps.add("search:keywords", keyword, 0.0)
        }

        // 3. Redis count 증가
        val newCount =
            zsetOps.incrementScore("search:keyword_count", keyword, 1.0)!!.toInt()

        // 4. Trie와 즉시 동기화 (없으면 insert)
        trie.updateOrInsert(keyword, newCount)
    }

    fun autoComplete(prefix: String, limit: Int): List<String> {
        // 1. Trie에 prefix가 존재하는지 확인
        if (!trie.hasExactWord(prefix)) {
            trieLoader.loadPrefix(prefix)
        }

        // 2. Trie에서 자동완성 검색
        return trie.search(prefix, limit)
    }
}