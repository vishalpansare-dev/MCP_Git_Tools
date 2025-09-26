# Spring Boot MCP Server Example

This project is a sample implementation of a Spring Boot server with Model Context Protocol (MCP) integration. It demonstrates how to build a modular server application using Spring Boot and MCP for context-aware operations.

## Features
- Spring Boot RESTful server
- MCP integration for model context operations
- Example weather module
- Git tools utilities

## Prerequisites
- Java 17 or higher
- Maven 3.6+

## Installation
1. Clone the repository:
   ```bash
   git clone <repo-url>
   ```
2. Navigate to the project directory:
   ```bash
   cd MCP_Git_Tools
   ```
3. Build the project:
   ```bash
   ./mvnw clean compile package
   ```

## Usage
To start the server:
```bash
./mvnw spring-boot:run
```
The server will start on the default port (usually 8080). You can configure settings in `src/main/resources/application.properties`.

## MCP Configuration

The project uses an `mcp.json` configuration file to define MCP server settings. This file is typically located at:

```
~/.config/github-copilot/intellij/mcp.json
```

### Example mcp.json
```json
{
  "servers": {
    "githu": {
      "command": "java",
      "args": [
        "-jar",
        "/target/mcp-git-tools-0.0.1-SNAPSHOT.jar",
        "-DACCESS_TOKEN=your_github_access_token"
      ]
    }
  }
}
```

- `command`: The command to start the MCP server (Java in this case).
- `args`: Arguments for the command, including the path to the JAR file and the GitHub access token.

Update the `ACCESS_TOKEN` value with your own GitHub personal access token for authentication.

## Project Structure
```
src/
  main/
    java/
      com/mcp/git/tools/
        GitTools.java
        GitTools2.java
        MCPListApplication.java
    resources/
      application.properties
  test/
    java/
      com/
```

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request.

## License
This project is licensed under the MIT License.
