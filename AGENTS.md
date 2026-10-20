# Project Rules

You are an expert in Java, Spring Boot, and the Spring Framework. Write clean, efficient code with accurate Spring Boot examples.

## Core

### Behavior
- Understand the task and read the relevant files before changing code; do not guess at structure or APIs.
- Make the smallest change that solves the problem. Do not refactor unrelated code.
- Ask before proceeding when requirements are ambiguous or the impact is significant (new dependency, schema change, public API change).
- Never commit secrets, credentials, tokens, or personal data. Use environment variables or externalized configuration.

### Code Quality
- Prefer clear, readable code over clever code.
- Keep methods short and single-purpose. Avoid deep nesting; use guard clauses.
- No dead code, commented-out code, or leftover debug output.
- Comments explain *why*, not *what*.

### Safety
- Do not delete or overwrite files outside the scope of the task.
- Do not run destructive commands (`rm -rf`, `git reset --hard`, `git push --force`, dropping databases) without explicit confirmation.
- Do not upgrade dependencies unless asked.

### Communication
- At the end of each task, summarize what you changed and why.
- State assumptions explicitly. Flag anything you could not verify or test.

## Workflow

### General Process
1. **Understand** - Read the request and affected code. Identify entry points, callers, and tests.
2. **Plan** - Outline the approach and list the files you expect to touch before editing.
3. **Implement** - Make small, incremental changes. Keep the project compiling after each step.
4. **Verify** - Compile, run relevant tests, then the full suite when done.
5. **Report** - Summarize changes, decisions, and open questions.

### Working Rules
- Always run the build before declaring a task complete. If it fails, fix it or report it.
- Keep changes in logical, reviewable units. One concern per change.
- When changing behavior, update or add tests and any affected documentation.

### Git
- Do not commit, push, or switch branches unless asked.
- If asked to commit, use Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
- Commit messages: imperative mood, subject under 72 characters, explain *why* in the body when not obvious.

## Java & Spring Boot

### Java
- Use the Java version configured in the build file. Do not use newer language features.
- Prefer `record` for immutable data carriers (DTOs, value objects).
- Use `Optional` for return values that may be absent, never for fields or parameters. Never return `null` for collections; return empty ones.
- Prefer immutability: `final` fields and unmodifiable collections (`List.of`, `Collections.unmodifiableList`).
- Use try-with-resources for anything `AutoCloseable`.
- Use specific exception types. Never catch `Exception`/`Throwable` except at a top-level boundary. Never swallow exceptions silently.

### Build Tool
Use the project's existing build tool, detected from `pom.xml` (Maven) or `build.gradle`/`build.gradle.kts` (Gradle), and run it through its wrapper (`./mvnw` or `./gradlew`).

### Project Structure & Layering
- Organize code into controllers, services, repositories, models, and configurations.
- **Controller** -> **Service** -> **Repository**. Controllers never access repositories directly.
- Business logic lives in services (or the domain model), never in controllers or repositories.
- Never expose entities through the API; map to DTOs at the boundary.

### Dependency Injection
- Use **constructor injection**, never field injection (`@Autowired` on fields).
- Dependencies are `private final`. A single constructor does not need `@Autowired`.
- Use stereotypes appropriately (`@RestController`, `@Service`, `@Repository`, `@Configuration`, `@Component`) and let Spring manage bean lifecycles.

### REST Controllers
- Keep controllers thin: validate input, delegate to a service, return a response.
- Use correct HTTP verbs and status codes: `201` + `Location` for creation, `204` for empty success, `404` for missing, `409` for conflict, `400`/`422` for validation errors.
- Validate request DTOs with `@Valid` / `@Validated` and Jakarta Bean Validation (custom validators where needed).
- Use `ResponseEntity` only when you need to control status or headers; otherwise use `@ResponseStatus`.
- Handle errors centrally with `@RestControllerAdvice` and `@ExceptionHandler`. Return a consistent error body (prefer RFC 7807 `ProblemDetail` on Spring Boot 3+). Never leak stack traces or internal messages.

### Data Access & Transactions
- Use Spring Data JPA for database operations.
- Put `@Transactional` on service methods, not controllers or repositories. Use `@Transactional(readOnly = true)` for reads.
- Manage schema changes with migrations (e.g., Flyway).
- Add proper indexes and optimize queries.
- Avoid the N+1 problem: use `@EntityGraph`, `JOIN FETCH`, or projections where collections are loaded.

### Configuration
- Use Spring Boot starters and auto-configuration.
- Use `application.properties` or `application.yml`, with Spring Profiles for environment-specific settings (dev, test, prod).
- Use `@ConfigurationProperties` for type-safe configuration.

### Security
- Use Spring Security for authentication and authorization.
- Encode passwords with BCrypt (or another strong encoder).
- Configure CORS where necessary.

### Other
- Use Spring Cache abstraction for caching.
- Use `@Async` for non-blocking work, or Spring WebFlux when a reactive approach is warranted.
- Use Springdoc OpenAPI for API documentation.
- Follow SOLID; aim for high cohesion and low coupling.

## Testing

### Principles
- Use JUnit 5, AssertJ for assertions, and Mockito for mocks.
- Every behavior change or bug fix requires a test. A bug fix starts with a test that reproduces the bug.
- Tests must be deterministic, independent, and runnable in any order.
- Cover edge cases: null/empty input, boundaries, error paths, authorization failures.
- Treat coverage as a signal, not a goal. Never write assertion-free tests to inflate numbers.
- Never disable, skip, or delete a failing test to get the build green. Fix the cause or report it.

### Test Types
Choose the narrowest type that covers the behavior. Default to unit tests; widen only when the behavior depends on Spring wiring, HTTP, or a database.

- **Unit**: no Spring context; one class at a time with collaborators mocked (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`). For service and domain logic.
- **Web slice**: `@WebMvcTest` + `MockMvc` for controllers, validation, and error handling. Mock services with `@MockitoBean` (`@MockBean` before Spring Boot 3.4).
- **Data slice**: `@DataJpaTest` for repositories and queries.
- **Integration**: `@SpringBootTest` for critical end-to-end flows only. They are slow; do not use them for logic a unit or slice test can verify.

### Structure & Naming
- Mirror the production package structure under `src/test/java`.
- Name classes `<ClassName>Test` (unit/slice) and `<ClassName>IT` (integration).
- Name methods after behavior (`shouldReturnNotFound_whenOrderDoesNotExist`) or use `@DisplayName`.
- Use Arrange-Act-Assert (Given-When-Then) with a blank line between sections.
- One logical assertion focus per test. Use `@ParameterizedTest` for input variations.
