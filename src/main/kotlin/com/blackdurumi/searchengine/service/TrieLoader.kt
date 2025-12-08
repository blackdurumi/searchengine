package com.blackdurumi.searchengine.service

import com.blackdurumi.searchengine.model.Trie
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class TrieLoader(
    private val redisTemplate: RedisTemplate<String, String>,
    private val trie: Trie
) {
    fun loadPrefix(prefix: String) {
        val words = findByPrefix(prefix)

        words.forEach { word ->
            val count = redisTemplate.opsForZSet()
                .score("search:keyword_count", word)
                ?.toInt() ?: 0

            trie.insert(word, count)
        }

        println("Loaded ${words.size} keywords for prefix=$prefix")
    }

    fun findByPrefix(prefix: String): List<String> {
        val max = prefix + "\uFFFF"

        // [prefix, max] 범위
        val range: Range<String> = Range.closed(prefix, max)

        return redisTemplate
            .opsForZSet()
            .rangeByLex("search:keywords", range)
            ?.toList()
            ?: emptyList()
    }
}