package com.blackdurumi.searchengine.model

import java.util.concurrent.ConcurrentHashMap

class TrieNode(
    val children: ConcurrentHashMap<Char, TrieNode> = ConcurrentHashMap(),
    var isEnd: Boolean = false,
    var count: Int = 0,
)
