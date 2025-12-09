package com.blackdurumi.searchengine.api

import com.blackdurumi.searchengine.application.TrieApplication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ApiController(
    private val trieApplication: TrieApplication,
) {

    @PostMapping("/search")
    fun search(
        @RequestParam keyword: String
    ): String {
        trieApplication.increaseSearchCount(keyword)
        return "ok"
    }

    @GetMapping
    fun autoComplete(
        @RequestParam("q") query: String,
        @RequestParam(value = "limit", defaultValue = "5") limit: Int
    ): List<String> {
        if (query.isBlank()) return emptyList()
        return trieApplication.autoComplete(query, limit)
    }
}