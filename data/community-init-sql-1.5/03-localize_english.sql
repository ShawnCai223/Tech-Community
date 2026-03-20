USE community;

UPDATE discuss_post
SET title = CASE MOD(id, 12)
        WHEN 0 THEN 'Spring Boot 3 migration notes from a production service'
        WHEN 1 THEN 'Redis cache invalidation strategies that actually worked'
        WHEN 2 THEN 'Elasticsearch indexing gotchas in a discussion platform'
        WHEN 3 THEN 'Kafka retry patterns without duplicate side effects'
        WHEN 4 THEN 'Docker image slimming tips for Java services'
        WHEN 5 THEN 'Choosing between JPA and MyBatis for read-heavy pages'
        WHEN 6 THEN 'Observability basics for one feature team'
        WHEN 7 THEN 'Safer file uploads in a Spring application'
        WHEN 8 THEN 'Practical rate limiting for login and comment endpoints'
        WHEN 9 THEN 'CI pipeline checks that caught our last regressions'
        WHEN 10 THEN 'JUnit integration test patterns for async event flows'
        ELSE 'Thread pool tuning lessons from a noisy notification job'
    END,
    content = CASE MOD(id, 12)
        WHEN 0 THEN 'We migrated a medium-sized service from Spring Boot 2 to Spring Boot 3 in three passes: dependency cleanup, Jakarta namespace changes, and contract verification. The biggest surprise was not compilation, it was filter ordering and small security behavior changes that broke admin actions in staging. If you have done the migration already, what failed after the app looked healthy?'
        WHEN 1 THEN 'Our first Redis integration looked fast in benchmarks and still produced stale data in production-like tests. The fix was to stop treating cache eviction as an afterthought and define ownership for each key, especially around post updates and delete events. I would like to compare simple eviction, versioned keys, and short TTL strategies for discussion data.'
        WHEN 2 THEN 'Search looked straightforward until we had to rebuild indexes without losing admin confidence. Mapping drift, analyzer selection, and partial sync behavior caused most of the friction. The useful lesson was to make index recreation explicit and observable instead of hiding it behind application startup.'
        WHEN 3 THEN 'We tested several retry approaches for asynchronous notifications. The naive version replayed side effects and inflated counters. The safer version kept retries narrow, stored stable event payloads, and treated duplicate delivery as expected behavior. What patterns have worked for you when the consumer writes to both Redis and MySQL?'
        WHEN 4 THEN 'A quick image build was not enough once CI started pushing every branch. We cut the image size by removing unused tools, tightening the build context, and separating local dev convenience from runtime needs. Smaller images improved pull time, but the real win was fewer hidden environment differences.'
        WHEN 5 THEN 'This codebase uses MyBatis in areas where explicit SQL helps a lot. We still evaluated whether some read-heavy pages would become simpler with JPA projections. My conclusion so far is that clarity matters more than ideology: predictable SQL and easy pagination are worth a lot on a community product.'
        WHEN 6 THEN 'A single feature can be easy to ship and still hard to operate. For our search and moderation flow, the useful baseline was simple: structured logs, one timing metric for each remote dependency, and enough context in errors to understand which post or event failed. Fancy dashboards did not help until those basics were present.'
        WHEN 7 THEN 'File upload code tends to become risky through small shortcuts. We reviewed filename handling, storage paths, and content-type assumptions after noticing how easy it was to mix user input with filesystem behavior. If you had to keep a simple local upload feature, which safeguards would you treat as mandatory?'
        WHEN 8 THEN 'Rate limiting is only useful when it matches product behavior. We do not need a globally perfect algorithm here; we need something that slows brute-force login attempts and comment spam without making normal browsing feel broken. Token bucket at the edge looks fine, but per-user and per-IP diagnostics still matter.'
        WHEN 9 THEN 'The last few regressions were not dramatic failures. They were small behavior mismatches: wrong redirect targets, a missing JSON error body, and a stale search result after delete. The pipeline now blocks on integration tests that reflect those flows. It is slower than a unit-only pipeline, but much more honest.'
        WHEN 10 THEN 'MockMvc plus real persistence gave us much better confidence than the old print-and-check tests. The remaining challenge is keeping fixtures realistic without making the suite fragile. I am interested in patterns for integration tests that cover async fallbacks and still stay readable six months later.'
        ELSE 'We found that a thread pool can look healthy in isolation and still create noisy user experience when queues hide backpressure. The notification path improved once we reduced fan-out, bounded the queue, and logged execution time with request context. What signals do you watch before changing executor settings?'
    END,
    create_time = TIMESTAMPADD(MINUTE, MOD(id, 8) * 37,
                  TIMESTAMPADD(DAY, id - 109, '2025-07-05 08:30:00')),
    score = 900 + comment_count * 4 + (CASE WHEN status = 1 THEN 60 ELSE 0 END)
          + (CASE WHEN type = 1 THEN 80 ELSE 0 END) + MOD(id, 11)
WHERE id BETWEEN 109 AND 280;

UPDATE discuss_post
SET title = '2025 Q4 platform roadmap and moderation priorities',
    content = 'This thread collects the engineering and moderation goals we want to finish before the end of Q4 2025. The highest priority items are a predictable Elasticsearch admin workflow, better cache visibility, cleaner test data, and safer moderation actions. Please keep replies concrete: if you suggest a change, include what we would measure to prove it helped.'
WHERE id = 275;

UPDATE discuss_post
SET title = 'Weekly engineering thread: what shipped and what still hurts',
    content = 'We shipped a cleaner admin sync path for search, tightened local environment setup, and finally removed the guesswork around IK plugin installation. The rough edges are still familiar: fixture quality, realistic forum seed data, and confidence when data is rebuilt from scratch. Share one improvement that made your local debugging loop faster this week.'
WHERE id = 234;

UPDATE discuss_post
SET title = 'Featured deep dive: keeping cache and search results consistent',
    content = 'The hardest bugs were not crashes. They were moments when MySQL, Redis, and Elasticsearch each told a slightly different story. This post summarizes the fixes that helped: explicit reindex actions, deterministic integration tests, and clearer ownership for update and delete events. I would especially like feedback on how people verify eventual consistency before release.'
WHERE id = 274;

UPDATE discuss_post
SET title = 'What we learned while stabilizing Elasticsearch admin tooling',
    content = 'The tooling finally became usable once we stopped hiding state. Health checks, explicit index recreation, and repeatable local containers made failures obvious instead of mysterious. The next step is polishing the operator experience so a developer can tell in one minute whether a failure belongs to code, data, or environment.'
WHERE id = 277;

UPDATE discuss_post
SET title = 'How we made local environments less fragile for backend work',
    content = 'A lot of lost time came from invisible local differences: old services still listening on the same ports, manual plugin installs inside containers, and database seeds that drifted from what tests expected. The new goal is simple: one command to boot dependencies, one command to verify the stack, and one reset path that returns to a clean baseline.'
WHERE id = 276;

UPDATE discuss_post
SET title = 'Spring Cache versus direct Redis access in one codebase',
    content = 'We kept both patterns in the project for a while, which made troubleshooting harder than it needed to be. Spring Cache is convenient for stable read paths, while direct Redis access still feels better for counters and ticket-like data. If you had to standardize on one default approach for a team project, where would you draw the line?'
WHERE id = 273;

UPDATE discuss_post
SET title = 'Open thread: what should we measure before enabling IK by default?',
    content = 'The IK analyzer improves recall for Chinese search, but enabling it by default should come with operational guardrails. I want a short list of metrics and checks we all trust: index recreation success, sync duration, search relevance samples, and rollback steps when analyzer changes go wrong. What would you require before calling the rollout safe?'
WHERE id = 280;

UPDATE comment
SET content = CASE MOD(id, 16)
        WHEN 0 THEN 'We saw the same issue after a dependency upgrade, and the fix ended up being better visibility rather than more retries.'
        WHEN 1 THEN 'I would start by making the failing path reproducible in one integration test before changing production code.'
        WHEN 2 THEN 'This feels like a cache ownership problem. Once each write path had a single eviction rule, the behavior became easier to reason about.'
        WHEN 3 THEN 'The practical lesson for us was to stop relying on startup side effects and expose an explicit admin action instead.'
        WHEN 4 THEN 'If you keep MyBatis here, I would document the query intent next to the mapper because future edits will otherwise drift.'
        WHEN 5 THEN 'We reduced debugging time by logging the entity id, actor id, and event type on every async write.'
        WHEN 6 THEN 'I like the idea, but I would still add one failure-mode test for delete followed by search.'
        WHEN 7 THEN 'The containerized setup helped more than expected because everyone stopped arguing about invisible local differences.'
        WHEN 8 THEN 'One thing that helped us was checking Elasticsearch health first and only then blaming the Java code.'
        WHEN 9 THEN 'For rate limiting, I would keep the algorithm simple and spend the extra effort on good diagnostics.'
        WHEN 10 THEN 'We hit a similar issue with stale search results. A deterministic resync endpoint made support much easier.'
        WHEN 11 THEN 'This is a good candidate for a short runbook entry. The fix is small, but the context is easy to forget.'
        WHEN 12 THEN 'I would measure queue depth and processing latency before touching the executor configuration again.'
        WHEN 13 THEN 'If the team keeps this pattern, please add one example of the happy path and one example of the rollback path.'
        WHEN 14 THEN 'The interesting part is not the framework choice, it is whether the next person can debug the data flow quickly.'
        ELSE 'We solved something close to this by making the admin behavior explicit and reusing the same path in tests.'
    END
WHERE id BETWEEN 1 AND 231;

UPDATE comment c
JOIN discuss_post p ON c.entity_type = 1 AND c.entity_id = p.id
SET c.create_time = TIMESTAMPADD(MINUTE, 24 + MOD(c.id, 7) * 16, p.create_time)
WHERE c.entity_type = 1;

UPDATE comment c
JOIN comment parent_comment ON c.entity_type = 2 AND c.entity_id = parent_comment.id
SET c.create_time = TIMESTAMPADD(MINUTE, 8 + MOD(c.id, 5) * 11, parent_comment.create_time)
WHERE c.entity_type = 2;

UPDATE message
SET content = CASE MOD(id, 10)
        WHEN 0 THEN 'Can you review the Elasticsearch mapping change before the next deploy?'
        WHEN 1 THEN 'I reproduced the cache issue locally and wrote down the exact steps.'
        WHEN 2 THEN 'The CI build passed, but the search sync still needs a quick manual check.'
        WHEN 3 THEN 'I pushed a small fix for the login redirect behavior.'
        WHEN 4 THEN 'Do we want one more integration test around delete plus reindex?'
        WHEN 5 THEN 'The Docker-based local stack is much more predictable now.'
        WHEN 6 THEN 'I left notes on the query optimization idea in the engineering thread.'
        WHEN 7 THEN 'The notification job looks better after reducing the queue size.'
        WHEN 8 THEN 'Please sanity-check the Redis key naming before we merge.'
        ELSE 'I can help verify the rollout once the admin endpoints are stable.'
    END
WHERE content NOT LIKE '{%';

UPDATE message
SET create_time = TIMESTAMPADD(MINUTE, MOD(id, 6) * 17,
                  TIMESTAMPADD(HOUR, id - 1, '2025-08-01 09:00:00'))
WHERE id >= 1;

UPDATE user
SET create_time = TIMESTAMPADD(MINUTE, MOD(id, 5) * 23,
                  TIMESTAMPADD(DAY, MOD(id, 75), '2025-07-01 10:00:00'))
WHERE id >= 1;

UPDATE user
SET username = CASE id
        WHEN 11 THEN 'ops_anchor'
        WHEN 12 THEN 'latencyloop'
        WHEN 13 THEN 'queryforge'
        WHEN 21 THEN 'tracepilot'
        WHEN 22 THEN 'buildharbor'
        WHEN 23 THEN 'cachecanvas'
        WHEN 24 THEN 'indexcraft'
        WHEN 25 THEN 'eventdock'
        WHEN 101 THEN 'river_api'
        WHEN 102 THEN 'typedbranch'
        WHEN 103 THEN 'deploylane'
        WHEN 111 THEN 'signalpath'
        WHEN 112 THEN 'mergewindow'
        WHEN 113 THEN 'heapflower'
        WHEN 114 THEN 'packetmason'
        WHEN 115 THEN 'retryfield'
        WHEN 116 THEN 'logcurrent'
        WHEN 117 THEN 'stateweaver'
        WHEN 118 THEN 'payloadfox'
        WHEN 119 THEN 'opsquill'
        WHEN 120 THEN 'streamgarden'
        WHEN 121 THEN 'byteharbor'
        WHEN 122 THEN 'metricatlas'
        WHEN 123 THEN 'cacheember'
        WHEN 124 THEN 'schemajet'
        WHEN 125 THEN 'threadmint'
        WHEN 126 THEN 'dbnorth'
        WHEN 127 THEN 'socketfield'
        WHEN 128 THEN 'routeglow'
        WHEN 129 THEN 'codecanyon'
        WHEN 131 THEN 'stackdrift'
        WHEN 132 THEN 'buildsprout'
        WHEN 133 THEN 'signalcove'
        WHEN 134 THEN 'queuestone'
        WHEN 137 THEN 'shardlane'
        WHEN 138 THEN 'flowparcel'
        WHEN 144 THEN 'patchhollow'
        WHEN 145 THEN 'opsriddle'
        WHEN 146 THEN 'coffeestack'
        WHEN 149 THEN 'nightdeploy'
        ELSE username
    END,
    header_url = CASE id
        WHEN 11 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 12 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 13 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 21 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 22 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 23 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 24 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 25 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 101 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 102 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 103 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 111 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 112 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 113 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 114 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 115 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 116 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 117 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 118 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 119 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 120 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 121 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 122 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 123 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 124 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 125 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 126 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 127 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 128 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 129 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 131 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 132 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 133 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 134 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 137 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 138 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 144 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 145 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 146 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        WHEN 149 THEN 'https://shawnidea.com/community/img/avatar-default.svg'
        ELSE header_url
    END
WHERE id <> 1;
