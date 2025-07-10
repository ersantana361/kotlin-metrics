# Kotlin Metrics Analyzer

A command-line tool for analyzing Kotlin code metrics, specifically calculating LCOM (Lack of Cohesion of Methods) for Kotlin classes.

## What is LCOM?

LCOM (Lack of Cohesion of Methods) is a software metric that measures how well the methods of a class are related to each other through the instance variables they use. A lower LCOM value indicates better cohesion.

- **LCOM = 0**: High cohesion (good) - methods share instance variables
- **Higher LCOM**: Lower cohesion - methods are less related

## Features

- Analyzes Kotlin source files using the Kotlin compiler's PSI
- Calculates LCOM metrics for each class
- Standalone executable JAR for easy distribution
- Can be used across different projects

## Installation

### Building from Source

1. Clone the repository
2. Build the fat JAR:
   ```bash
   ./gradlew fatJar
   ```

### Setting up Global Alias

Add this to your `~/.bashrc`:
```bash
alias kotlin-metrics="java -jar /path/to/kotlin-metrics/build/libs/kotlin-metrics-all-1.0.0.jar"
```

Then reload your shell:
```bash
source ~/.bashrc
```

## Usage

### Basic Usage
```bash
java -jar kotlin-metrics-all-1.0.0.jar MyClass.kt
```

### With Global Alias
```bash
kotlin-metrics src/main/kotlin/MyClass.kt
```

### Example Output
```
Class: UserService
LCOM: 2
Class: DataProcessor
LCOM: 0
```

## Requirements

- Java 17 or higher
- Kotlin source files (.kt)

## How It Works

1. **Parsing**: Uses Kotlin compiler's PSI to parse source files
2. **Analysis**: Extracts class properties and methods
3. **Calculation**: For each method pair, checks if they share instance variables
4. **LCOM Formula**: `LCOM = P - Q` where:
   - P = number of method pairs with no shared variables
   - Q = number of method pairs with shared variables
   - If result < 0, LCOM = 0

## Example

Given a Kotlin class:
```kotlin
class Example {
    private val name: String = ""
    private val age: Int = 0
    
    fun getName(): String = name
    fun getAge(): Int = age
    fun getInfo(): String = "$name is $age years old"
}
```

- `getName()` uses `name`
- `getAge()` uses `age`  
- `getInfo()` uses both `name` and `age`

Method pairs:
- `getName()` & `getAge()`: no shared variables (P+1)
- `getName()` & `getInfo()`: share `name` (Q+1)
- `getAge()` & `getInfo()`: share `age` (Q+1)

LCOM = 1 - 2 = -1 → 0 (clamped to 0)

## License

This project is open source and available under the MIT License.