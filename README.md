# Trie-based Autocomplete with Redis

This project is a **toy implementation of a search auto-complete system** built to deeply understand how autocomplete works internally.

Core idea:
- **Redis = source of truth (keywords + search counts)**
- **Trie = in-memory cache**, lazily populated only when needed
- Optimized for **fast autocomplete**, **correct ranking**, and **incremental learning**

---

## Overall Architecture (High Level)

```
Client
  ├─ GET  /auto-complete?q=
  │        ↓
  │     TrieApplication.autoComplete()
  │        ↓
  │     check Trie loads prefix (search:keywords)
  │        ↓
  │        ├─ load prefix node to Trie from Redis (search:keywords)
  │        ↓
  │     list autoComplete words by rank from Redis (search:keyword_count)
  │
  └─ POST /search?keyword=
           ↓
        TrieApplication.increaseSearchCount()
           ↓
        updateOrInsert count to Redis (search:keywords, search:keyword_count)
           ↓
        sync to In-memory Trie
```

---

## Redis Data Model

Redis uses **two ZSETs**, each with a single responsibility.

### 1. search:keywords (ZSET)
- Purpose: **prefix search**
- Score: unused (always 0)
- Members: keyword strings

Used with **lexicographical range queries**:
```
ZRANGEBYLEX search:keywords prefix prefix\uFFFF
```

---

### 2. search:keyword_count (ZSET)
- Purpose: **ranking**
- Score: search count
- Members: keyword strings

---

## Search API (`POST /search?keyword=`)

This API represents a **confirmed search action** (user pressed enter or selected a keyword).

### Responsibilities
- Increase search count
- Register new keywords if never seen before
- Keep Redis and Trie **strongly consistent**

### Flow

```
Client
  ↓
increaseSearchCount(keyword)
  ↓
Redis:
  - if new keyword → add to search:keywords
  - increment search:keyword_count
  ↓
Trie:
  - update count
  - if missing → insert word immediately
```

### Why this matters
- Ensures **newly searched words immediately appear in autocomplete**
- Avoids Redis ↔ Trie inconsistency
- No need to lazy-load full prefixes here (single-word insert only)

---

## Autocomplete API (`GET /auto-complete?q=`)

Returns top-N keywords that start with the given prefix.

### Key Design Decisions
- Trie is **NOT preloaded**
- Prefix subtree is loaded **only once**
- Redis is accessed **only for cold prefixes**

### Flow

```
Client
  ↓
autoComplete(prefix)
  ↓
Trie check:
  - exact word exists?
  - prefix fully loaded?
      ↓ NO
TrieLoader.loadPrefix(prefix)
  ↓
Redis:
  - ZRANGEBYLEX search:keywords
  - fetch counts from search:keyword_count
  ↓
Trie populated
  ↓
Trie.search(prefix)
```

### Important Detail
`containsPrefix()` alone is NOT sufficient.

Cases like this must be handled:
- "caffeine drink" is loaded
- "caffeine" exists in Redis
- but Trie node exists without `isEnd=true`

Solution:
- Load prefix if **exact word not present**
- or track `fullyLoaded` per prefix node

---

## To Run This Project

### 1. Install & Run Redis Server in local

```bash
brew install redis
redis-cli ping # PONG
redis-server
```

---

### 2. Test Data Script

```bash
#!/bin/bash

redis-cli <<EOF
DEL search:keywords
DEL search:keyword_count

ZADD search:keywords 0 "cafe"
ZADD search:keywords 0 "cafe near me"
ZADD search:keywords 0 "caffeine"
ZADD search:keywords 0 "caffeine drinks"
ZADD search:keywords 0 "coffee shop"
ZADD search:keywords 0 "coffee beans"

ZADD search:keyword_count 5 "cafe"
ZADD search:keyword_count 2 "cafe near me"
ZADD search:keyword_count 3 "caffeine"
ZADD search:keyword_count 1 "caffeine drinks"
ZADD search:keyword_count 4 "coffee shop"
ZADD search:keyword_count 2 "coffee beans"
EOF
```

---

### 3. Verify

```bash
redis-cli --raw ZRANGE search:keywords 0 -1
redis-cli --raw ZRANGE search:keyword_count 0 -1 WITHSCORES
```