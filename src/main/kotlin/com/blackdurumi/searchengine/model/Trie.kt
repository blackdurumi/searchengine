package com.blackdurumi.searchengine.model

import org.springframework.stereotype.Component

@Component
class Trie {

    private val root = TrieNode()

    fun insert(word: String, count: Int = 1) {
        var cur = root
        for (ch in word) {
            cur = cur.children.computeIfAbsent(ch) { TrieNode() }
        }
        cur.isEnd = true
        cur.count = count
    }

    fun updateCount(word: String) {
        var cur = root
        for (c in word) {
            cur = cur.children[c] ?: return   // 아직 Trie에 없음 → lazy load 때만 재구축
        }
        if (cur.isEnd) {
            cur.count += 1
        }
    }

    fun containsPrefix(prefix: String): Boolean {
        var cur = root
        for (ch in prefix) {
            val next = cur.children[ch] ?: return false
            cur = next
        }
        return true
    }

    fun search(prefix: String, limit: Int): List<String> {
        var cur = root
        for (c in prefix) {
            cur = cur.children[c] ?: return emptyList()
        }

        val result = mutableListOf<Pair<String, Int>>()

        fun dfs(node: TrieNode, sb: StringBuilder) {
            if (node.isEnd) {
                result.add(sb.toString() to node.count)
            }
            for ((ch, child) in node.children) {
                sb.append(ch)
                dfs(child, sb)
                sb.deleteCharAt(sb.lastIndex)
            }
        }

        dfs(cur, StringBuilder(prefix))

        return result
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}