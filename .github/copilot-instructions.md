# Copilot instructions

## Integration tests

When adding or updating integration tests, follow the existing nested structure:

- For endpoint tests, create one top-level nested test class for each endpoint.
- Annotate endpoint test classes with `@DisplayName` containing the complete HTTP method and endpoint path.
- Organise endpoint scenarios into nested `Security`, `Validation`, and `HappyPath` classes.
- Preserve the same nested structure when copying or adapting tests for another endpoint.
- Keep shared fixtures and conversion helpers within the relevant test class where practical.
- Cover unauthorised access, missing or incorrect roles, validation failures, and successful behaviour as applicable.
- Prefer `@BeforeAll` and `@AfterAll` with `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` for shared fixtures that are not mutated between tests; use per-test setup only when isolation requires it.
- Use constructor injection for repositories and other Spring beans in integration test classes rather than `@Autowired` fields.
