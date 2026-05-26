# SimpleDatabase (sdb)

A lightweight, schema-free flat-file database for JVM applications. No external dependencies, no setup — just annotate a data class and go.

## Setup

```groovy
repositories {
    maven { url = uri("https://repository.atlantisservices.net/repository/api/") }
}

dependencies {
    implementation 'net.atlantisservices:sdb:1.0.0'
}
```

## Usage

**Define a data class:**
```kotlin
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val roles: Set = emptySet(),
    val metadata: Map = emptyMap()
)
```

**Create a repository:**
```kotlin
val users = SdbRepository(User::class)                          // reads/writes users.sdb
val users = SdbRepository(User::class, repository = "players") // reads/writes players.sdb
```

**CRUD:**
```kotlin
users.save(user)
users.findById(1L)
users.findBy("username", "selixe")
users.findAllBy("role", "ADMIN")
users.findAll()
users.deleteById(1L)
users.count()
```

## Supported Types

| Type | Stored as |
|---|---|
| `String`, `Int`, `Long`, `Double`, `Float`, `Boolean` | Plain value |
| `List<T>`, `Set<T>`, `Collection<T>` | Comma-separated |
| `Array<T>` | Comma-separated |
| `Map<K, V>` | `key:value` pairs, comma-separated |

## File Format

Records are stored in `.sdb` files as human-readable blocks:
[user:1]
id=1
username=selixe
roles=ADMIN,MOD
metadata=theme:dark,lang:en
[user:2]
id=2
username=john
roles=USER
metadata=

## License

Copyright (c) 2026 Atlantis Services. Licensed under the MIT License. See [LICENSE](LICENSE) for details.