package com.blackdurumi.searchengine.model

class TrieNode(
    val children: MutableMap<Char, TrieNode> = mutableMapOf(),
    var isEnd: Boolean = false,
    var count: Int = 0,
)
