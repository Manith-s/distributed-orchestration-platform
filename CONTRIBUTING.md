# Contributing to Distributed Job Orchestration Platform

Thank you for considering contributing to this project! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Enhancements](#suggesting-enhancements)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive criticism
- Accept feedback graciously
- Put the project's best interests first

## Getting Started

### Prerequisites

Before contributing, ensure you have:

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- Git
- An IDE (IntelliJ IDEA or Eclipse recommended)

### Setting Up Development Environment

1. **Fork the repository** on GitHub

2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/distributed-orchestration-platform.git
   cd distributed-orchestration-platform
   ```

3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/distributed-orchestration-platform.git
   ```

4. **Set up environment**:
   ```bash
   cp .env.example .env
   # Edit .env with your local configuration
   ```

5. **Start infrastructure**:
   ```bash
   docker-compose up -d
   ```

6. **Build the project**:
   ```bash
   mvn clean install
   ```

## Development Workflow

### Branching Strategy

We follow the Git Flow branching model:

- `main` - Production-ready code
- `develop` - Integration branch for features
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `hotfix/*` - Critical production fixes
- `release/*` - Release preparation

### Creating a Feature Branch

```bash
# Update your local develop branch
git checkout develop
git pull upstream develop

# Create a feature branch
git checkout -b feature/your-feature-name

# Make your changes and commit
git add .
git commit -m "Add: description of changes"

# Push to your fork
git push origin feature/your-feature-name
```

## Coding Standards

### Java Code Style

We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with some modifications:

#### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Braces**: K&R style (opening brace on same line)

#### Naming Conventions
- **Classes**: PascalCase (e.g., `JobService`)
- **Methods**: camelCase (e.g., `processJob()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRIES`)
- **Variables**: camelCase (e.g., `jobId`)

#### Example:
```java
public class JobService {
    private static final int MAX_RETRIES = 3;

    public void processJob(String jobId) {
        // Implementation
    }
}
```

### Documentation

- **Classes**: Document purpose and usage
- **Public methods**: Include JavaDoc with @param and @return
- **Complex logic**: Add inline comments explaining why, not what

```java
/**
 * Processes a job by distributing it to available workers.
 *
 * @param job the job to process
 * @return the processing result
 * @throws JobProcessingException if processing fails
 */
public JobResult processJob(Job job) throws JobProcessingException {
    // Implementation
}
```

### Package Structure

```
com.platform.[service]/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA entities
├── exception/      # Custom exceptions
├── repository/     # Data access layer
├── service/        # Business logic
└── util/           # Utility classes
```

## Testing Guidelines

### Test Coverage

- **Minimum coverage**: 70% overall
- **Critical paths**: 90% coverage required
- **New features**: Must include tests

### Test Structure

```java
@SpringBootTest
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Test
    @DisplayName("Should process job successfully")
    void shouldProcessJobSuccessfully() {
        // Given
        Job job = createTestJob();

        // When
        JobResult result = jobService.processJob(job);

        // Then
        assertThat(result.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }
}
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl orchestrator-service

# Run with coverage
mvn test jacoco:report

# Run integration tests
mvn verify -P integration-tests
```

## Commit Messages

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

### Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `perf`: Performance improvements

### Examples

```bash
feat(orchestrator): add job priority queue

Implement priority-based job scheduling to process high-priority
jobs before lower priority ones.

Closes #123

---

fix(worker): prevent duplicate job processing

Add distributed lock check before processing jobs to prevent
race conditions in multi-worker deployments.

Fixes #456

---

docs(readme): update installation instructions

Add Docker Desktop requirement and clarify Java version.
```

## Pull Request Process

### Before Submitting

1. **Update your branch**:
   ```bash
   git checkout develop
   git pull upstream develop
   git checkout feature/your-feature
   git rebase develop
   ```

2. **Run tests**:
   ```bash
   mvn clean verify
   ```

3. **Check code style**:
   ```bash
   mvn checkstyle:check
   ```

4. **Update documentation** if needed

### PR Template

When creating a PR, include:

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests added/updated
```

### Review Process

1. At least **one approval** required
2. All **CI checks must pass**
3. **Conflicts resolved** with target branch
4. **Code review comments** addressed

### Merging

- **Squash and merge** for feature branches
- **Merge commit** for release branches
- Delete branch after merging

## Reporting Bugs

### Before Reporting

1. **Check existing issues** to avoid duplicates
2. **Try latest version** - bug might be fixed
3. **Verify** it's reproducible

### Bug Report Template

```markdown
**Describe the bug**
Clear description of what the bug is.

**To Reproduce**
Steps to reproduce:
1. Start service with '...'
2. Send request to '...'
3. Observe error '...'

**Expected behavior**
What you expected to happen.

**Actual behavior**
What actually happened.

**Environment**
- OS: [e.g., Ubuntu 22.04]
- Java version: [e.g., 17.0.5]
- Service version: [e.g., 1.0.0]

**Logs**
```
Paste relevant logs here
```

**Additional context**
Any other relevant information.
```

## Suggesting Enhancements

### Enhancement Template

```markdown
**Is your feature request related to a problem?**
Clear description of the problem.

**Describe the solution**
How you'd like to see it solved.

**Describe alternatives**
Any alternative solutions considered.

**Additional context**
Mockups, examples, or other relevant information.
```

## Project-Specific Guidelines

### Database Migrations

- Use Flyway for schema changes
- Version format: `V{version}__{description}.sql`
- Test migrations on empty database
- Include rollback scripts

### API Changes

- Maintain backward compatibility
- Version breaking changes (e.g., `/api/v2/...`)
- Update OpenAPI documentation
- Include migration guide

### Performance Considerations

- Profile code changes for performance impact
- Add JMH benchmarks for critical paths
- Document any performance implications

### Security

- Never commit secrets or credentials
- Use environment variables for configuration
- Follow OWASP security guidelines
- Report security issues privately to maintainers

## Getting Help

- **Documentation**: Check [README.md](README.md) and [DEPLOYMENT.md](DEPLOYMENT.md)
- **Discussions**: Use GitHub Discussions for questions
- **Issues**: Search existing issues or create a new one
- **Chat**: Join our community chat (if available)

## Recognition

Contributors are recognized in:
- `CONTRIBUTORS.md` file
- Release notes
- Project README

Thank you for contributing to making this project better!
