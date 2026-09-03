<p align="center">
  <img src="https://raw.githubusercontent.com/Tebrox/VertexCore/master/assets/banner.png" alt="VertexCore Banner">
</p>

# VertexCore

VertexCore is a **Paper-only core plugin** designed as a shared foundation for plugin developers.

It provides reusable infrastructure for configuration handling, database access and command execution,
reducing duplicated boilerplate across multiple plugins.

> VertexCore is **not** a gameplay plugin.  
> It is intended to be used as a dependency by other plugins.

---

## Features

### Configuration System
- Annotation-based configuration definitions
- Automatic file generation and loading
- Validation, default values and comments
- Config objects mapped directly to Java classes

### Database System
- Unified database abstraction
- Supported backends:
    - JSON (flatfile)
    - H2
    - MySQL / MariaDB
- Async and sync access
- Built-in migration support
- Config-driven database settings

### Command System
- Centralized command execution framework
- Support for root commands and subcommands
- Argument injection and resolvers
- Permission and visibility handling
- Tab completion support

---

## Requirements & Compatibility

VertexCore 1.1.x is built as **Java 21 bytecode** and compiled against the **Paper 1.21.4 API**.

Verified runtime compatibility:

| Paper | Java | Status |
| --- | --- | --- |
| 1.21.4 | 21 | Verified |
| 26.2 | 25 | Verified |

The same VertexCore JAR is used for both runtime checks.

- **Minimum Java for VertexCore:** Java 21
- **Minimum supported Paper baseline:** Paper 1.21.4
- Newer Paper versions may require a newer Java runtime independently of VertexCore. For example, the verified Paper 26.2 runtime uses Java 25.
- VertexCore 1.x remains compatibility-oriented; public API compatibility is preserved where possible.

The compatibility matrix is exercised by GitHub Actions using reproducible runtime smoke tests on both supported endpoints.

---

## Installation

1. Download the latest release
2. Place `VertexCore.jar` into your server’s `plugins` folder
3. Restart the server

Plugins using VertexCore must declare it as a dependency.

---

## Developer Usage

VertexCore is distributed via **jitpack.io**.

### Gradle

```gradle
repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
}

dependencies {
    compileOnly("com.github.Tebrox-Development:VertexCore:v1.1.0")
}
```

### Maven

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Tebrox-Development</groupId>
    <artifactId>VertexCore</artifactId>
    <version>v1.1.0</version>
    <scope>provided</scope>
</dependency>
```

---

## Development Line

The current `development` branch targets `1.1.0-SNAPSHOT`.

Its build baseline is:

- Java release target: `21`
- Paper compile API: `1.21.4-R0.1-SNAPSHOT`
- Runtime compatibility gates: Paper `1.21.4` / Java `21` and Paper `26.2` / Java `25`

---

## Documentation

- Full documentation is available in the **GitHub Wiki**
- Covers configuration, database, command system and migration

---

## License

MIT License
