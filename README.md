# JAssert - Automatic Null Check Injection

A Java 21 annotation processor that automatically injects `Assert.notNull()` checks into constructors at compile time.

## Overview

JAssert provides a `@NullCheck` annotation that, when applied to a class, automatically adds null validation to all constructor parameters. The validation happens at compile time by modifying the bytecode, so there's zero runtime overhead and no reflection.

### Example

**Before compilation:**
```java
@NullCheck
public class User {
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
```

**After compilation (bytecode equivalent):**
```java
public class User {
    private final String name;
    private final String email;

    public User(String name, String email) {
        Assert.notNull("User.email", email);
        Assert.notNull("User.name", name);
        this.name = name;
        this.email = email;
    }
}
```

If you call `new User(null, "test@example.com")`, you'll get:
```
IllegalArgumentException: User.name must not be null
```

## Features

- ✅ **Automatic null checks** in all constructors
- ✅ **Works with Lombok** - processes Lombok-generated constructors
- ✅ **Compile-time only** - no runtime dependencies
- ✅ **Zero performance overhead** - bytecode modification, not reflection
- ✅ **Clear error messages** - includes class and field name in exception

## Project Structure

```
jassert/
├── nullcheck-processor/     # The annotation processor implementation
│   ├── @NullCheck          # Annotation to mark classes
│   ├── NullCheckProcessor  # Bytecode manipulation logic
│   └── Assert              # Runtime assertion class
└── demo-app/               # Example consumer application
```

## Usage

### 1. Add the Dependency

Add the annotation processor as a provided dependency:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>nullcheck-processor</artifactId>
    <version>1.0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### 2. Configure Maven Compiler Plugin

The annotation processor uses javac internals (AST manipulation), which requires special JVM exports. Add this to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <!-- Required for annotation processors that use javac internals -->
                <fork>true</fork>
                <compilerArgs>
                    <!-- Export javac internals to annotation processors -->
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
                    <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
                </compilerArgs>
                <!-- Processor order: Lombok first, then NullCheck -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.34</version>
                    </path>
                    <path>
                        <groupId>com.example</groupId>
                        <artifactId>nullcheck-processor</artifactId>
                        <version>1.0.1-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Note:** If you're using a parent POM, you can define this configuration once in `<pluginManagement>` and all child modules will inherit it automatically (see the `demo-app` module for an example).

### 3. Annotate Your Classes

Simply add `@NullCheck` to any class:

```java
import com.example.annotations.NullCheck;

@NullCheck
public class Product {
    private final String id;
    private final String name;
    private final double price;

    public Product(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}
```

## Using with Lombok

JAssert works seamlessly with Lombok. Just ensure Lombok is listed **before** NullCheck in the `annotationProcessorPaths`:

```java
import com.example.annotations.NullCheck;
import lombok.RequiredArgsConstructor;

@NullCheck
@RequiredArgsConstructor
public class User {
    private final String name;
    private final String email;
}
```

The processor will automatically add null checks to Lombok's generated constructor.

## How It Works

1. **Annotation Processing**: During compilation, the `NullCheckProcessor` runs after Lombok
2. **AST Modification**: It finds all constructors in `@NullCheck` annotated classes
3. **Bytecode Injection**: For each constructor parameter, it prepends:
   ```java
   Assert.notNull("ClassName.paramName", paramValue);
   ```
4. **Compilation**: The modified AST is compiled into bytecode

The processor uses javac's internal APIs (`JCTree`, `TreeMaker`) to manipulate the Abstract Syntax Tree before final compilation.

## Example Output

When a null value is passed to a constructor:

```java
@NullCheck
public class Order {
    private final String orderId;
    private final Customer customer;

    public Order(String orderId, Customer customer) {
        this.orderId = orderId;
        this.customer = customer;
    }
}

// Usage:
new Order("12345", null);  // throws IllegalArgumentException
```

**Exception:**
```
java.lang.IllegalArgumentException: Order.customer must not be null
	at com.example.Assert.notNull(Assert.java:10)
	at com.example.demo.Order.<init>(Order.java:8)
	at ...
```

## Building the Project

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Build without tests
mvn clean install -DskipTests
```

## Running the Demo

The `demo-app` module contains example classes demonstrating different scenarios:

```bash
# Compile and test the demo app
mvn -pl demo-app clean test

# The tests verify that null checks are properly injected
```

Check these example classes in `demo-app/src/main/java/com/example/demo/`:
- `SimpleClass.java` - Basic class with manual constructor
- `SimpleClassLombok.java` - Class using Lombok's `@RequiredArgsConstructor`
- `SimpleRecord.java` - Java record (processors don't modify records by design)

## Requirements

- **Java 21+** (uses modern Java features and javac APIs)
- **Maven 3.9+**
- **maven-compiler-plugin 3.13.0+**

## Limitations

1. **Java 21 Requirement**: The processor uses javac 21 internals
2. **Maven Only**: Currently only configured for Maven (Gradle support would need additional configuration)
3. **JVM Exports Required**: Consumers must configure `--add-exports` flags
4. **Records Not Supported**: Java records have immutable constructors that can't be modified
5. **Primitive Types**: The processor adds checks for all parameters, but primitives can't be null (this is harmless but unnecessary)

## Technical Details

### Processor Implementation

The annotation processor (`NullCheckProcessor.java`) uses:
- `javax.annotation.processing.AbstractProcessor` - Standard annotation processing API
- `com.sun.tools.javac.tree.JCTree` - javac's internal AST representation
- `com.sun.tools.javac.tree.TreeMaker` - Factory for creating new AST nodes
- `com.sun.source.util.Trees` - Bridge between annotation processing and javac internals

### Why `--add-exports` is Needed

Java's module system (JPMS) hides javac internals by default. The `--add-exports` flags explicitly export these packages:
- `jdk.compiler/com.sun.tools.javac.api` - Compiler API
- `jdk.compiler/com.sun.tools.javac.tree` - AST classes
- `jdk.compiler/com.sun.tools.javac.util` - Utility classes
- `jdk.compiler/com.sun.tools.javac.processing` - Processing environment

Without these exports, you'll get:
```
IllegalAccessError: class NullCheckProcessor cannot access class JavacProcessingEnvironment
```

## Testing

The project includes comprehensive tests:

### Unit Tests (`nullcheck-processor`)
Tests the processor logic in isolation using in-memory compilation:
```bash
mvn -pl nullcheck-processor test
```

### Integration Tests (`demo-app`)
Tests real-world usage scenarios:
```bash
mvn -pl demo-app test
```

All tests verify that:
- Null values throw `IllegalArgumentException`
- Non-null values are accepted
- Error messages include the correct field names

## Troubleshooting

### Processor Not Running

**Symptom**: No null checks are injected, tests pass when they shouldn't

**Solution**:
1. Ensure `fork=true` in compiler configuration
2. Verify `annotationProcessorPaths` includes the processor
3. Check that `--add-exports` flags use `-J` prefix: `-J--add-exports=...`

### IllegalAccessError

**Symptom**:
```
IllegalAccessError: class NullCheckProcessor cannot access class JavacProcessingEnvironment
```

**Solution**: Add all required `--add-exports` flags with `-J` prefix

### Lombok Not Working

**Symptom**: Null checks not added to Lombok-generated constructors

**Solution**: Ensure Lombok is listed **before** NullCheck in `annotationProcessorPaths`

### Package Structure Corruption

**Symptom**: Classes compiled to wrong directory (e.g., `target/classes/MyClass.class` instead of `target/classes/com/example/MyClass.class`)

**Solution**: Run Maven from the project root, not from a submodule directory

## Contributing

This is an educational project demonstrating annotation processing and bytecode manipulation. Feel free to:
- Report issues
- Submit pull requests
- Use as a reference for your own processors

## License

[Add your license here]

## Acknowledgments

- Uses [Lombok](https://projectlombok.org/) for demo examples
- Inspired by Lombok's approach to compile-time code generation
- Built with Java 21 and Maven

## Further Reading

- [Java Annotation Processing](https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/annotation/processing/package-summary.html)
- [Lombok Documentation](https://projectlombok.org/)
- [javac Tree API](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.compiler/com/sun/source/tree/package-summary.html)
