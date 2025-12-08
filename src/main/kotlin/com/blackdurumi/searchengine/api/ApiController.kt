package com.blackdurumi.searchengine.api

import com.blackdurumi.searchengine.service.TrieService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class ApiController(
    private val trieService: TrieService,
) {

    @PostMapping("/search")
    fun search(
        @RequestParam keyword: String
    ): String {
        trieService.increaseSearchCount(keyword)
        return "ok"
    }

    @GetMapping
    fun autoComplete(
        @RequestParam("q") query: String,
        @RequestParam(value = "limit", defaultValue = "5") limit: Int
    ): List<String> {
        if (query.isBlank()) return emptyList()
        return trieService.autoComplete(query, limit)
    }
}