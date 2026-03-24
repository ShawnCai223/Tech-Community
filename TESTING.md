# Testing

The repository uses a layered test strategy so day-to-day development stays fast while external dependency coverage remains opt-in.

## CI Coverage

GitHub Actions currently runs:

- Backend tests with the `test` Spring profile
- Frontend type check and production build

See `.github/workflows/ci-tests.yml` for the exact steps.

## Default Integration Coverage

These tests should remain meaningful in normal development:

- `MainFlowIntegrationTests`

Run:

```bash
mvn -q -Dtest=MainFlowIntegrationTests test
```

## External Dependency Integration

These tests depend on services outside the default local test profile and should be treated as opt-in:

- `ElasticsearchIntegrationTests`
- `KafkaTests`
- `MailTests`

Examples:

```bash
mvn -q -Dcommunity.es.integration=true -Dtest=ElasticsearchIntegrationTests test
mvn -q -Dcommunity.es.integration=true -Dcommunity.es.ik.integration=true -Dtest=ElasticsearchIntegrationTests test
```

## Manual Exploration Tests

These are legacy exploratory tests kept only for reference. They are disabled by default because they are not assertion-based, may mutate state, or mainly print output:

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

If one of these still matters, the next step should be to rewrite it as an assertion-based automated test.
