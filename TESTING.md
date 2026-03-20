# Test Layout

The test suite is intentionally split into three layers:

## Default integration coverage

These are the tests that should remain meaningful in day-to-day development:

- `MainFlowIntegrationTests`

They are assertion-based and exercise real application behavior.

Run:

```bash
mvn -q -Dtest=MainFlowIntegrationTests test
```

## External dependency integration

These tests require services outside the default local test profile and are opt-in:

- `ElasticsearchIntegrationTests`
- `KafkaTests`
- `MailTests`

Examples:

```bash
mvn -q -Dcommunity.es.integration=true -Dtest=ElasticsearchIntegrationTests test
mvn -q -Dcommunity.es.integration=true -Dcommunity.es.ik.integration=true -Dtest=ElasticsearchIntegrationTests test
```

## Manual exploration tests

These are legacy exploratory tests kept for reference only. They are disabled by default because they are not assertion-based, may mutate shared state, or mainly print output:

- `CaffeineTests`
- `ShawnIdeaApplicationTests`
- `ElasticsearchTests`
- `LoggerTests`
- `MapperTests`
- `QuartzTests`
- `RedisTests`
- `SensitiveTests`
- `ThreadPoolTests`
- `TransactionTests`

If one of these is still useful, the next step is to convert it into an assertion-based test and remove the manual classification.
