# Kotlin Metrics Project

## Project Overview
This is a Kotlin-based code metrics analyzer that calculates LCOM (Lack of Cohesion of Methods) for Kotlin classes. It uses the Kotlin compiler's PSI (Program Structure Interface) to parse and analyze Kotlin source files.

## Build System
- **Build Tool**: Gradle with Kotlin DSL
- **Main Task**: `./gradlew fatJar` - Creates an executable JAR with all dependencies
- **Output**: `build/libs/kotlin-metrics-all-1.0.0.jar`

## Key Dependencies
- `kotlin-compiler-embeddable:1.9.22` - For PSI parsing
- `kotlin-stdlib:1.9.22` - Standard library
- `kotlin-reflect:1.9.22` - Reflection support

## Project Structure
```
src/main/kotlin/Main.kt - Main analyzer code
build.gradle.kts - Build configuration
gradlew - Gradle wrapper
```

## Usage
```bash
# Build the JAR
./gradlew fatJar

# Run the analyzer
java -jar build/libs/kotlin-metrics-all-1.0.0.jar <path-to-kotlin-file>

# Or use the alias (add to ~/.bashrc):
alias kotlin-metrics="java -jar /home/ersantana/dev/projects/personal/kotlin-metrics/build/libs/kotlin-metrics-all-1.0.0.jar"
```

## Development Notes
- The project analyzes Kotlin classes and calculates LCOM metrics
- Uses Kotlin PSI to parse source files and extract method-property relationships
- Handles duplicate META-INF files in fat JAR with `DuplicatesStrategy.EXCLUDE`
- Requires Java 17+ (configured in build.gradle.kts)