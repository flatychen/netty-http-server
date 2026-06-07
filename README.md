# Netty HTTP/HTTP2 Server

A comprehensive Netty-based HTTP/HTTP2 server implementation demonstrating enterprise-level patterns for handling HTTP requests with pluggable architecture.

## Features

- **HTTP/1.1 and HTTP/2 Support** - Configurable protocol support with proper protocol negotiation
- **Pluggable Architecture** - Easy to add custom filters and handlers
- **Asynchronous Processing** - Built on CompletableFuture for non-blocking I/O
- **Connection Management** - Configurable timeouts and concurrent stream limits
- **Resource Optimization** - Pooled buffers and automatic resource cleanup
- **Threading Model** - Configurable boss and worker thread pools
- **Extensive Configuration** - Builder pattern for server configuration

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+

### Running the Examples

#### HTTP/1.1 Server
```bash
# Build the project
mvn clean compile

# Run HTTP/1.1 server
mvn exec:java -Dexec.mainClass="cn.flaty.http2.server.Http1ServerBenchMark"
```

#### HTTP/2 Server
```bash
# Run HTTP/2 server
mvn exec:java -Dexec.mainClass="cn.flaty.http2.server.Http2ServerBenchMark"
```

Both servers will start on port 6667. You can test them with curl:

```bash
# Test HTTP/1.1 server
curl -v http://localhost:6667/

# Test HTTP/2 server (requires HTTP/2 support in curl)
curl --http2 -v http://localhost:6667/
```

## Architecture

### Core Components

1. **NettyHttpServer** - Main server class with fluent builder API
2. **HttpHandler** - Abstract base class for request processing logic
3. **HttpFilter** - Base class for request/response interception
4. **NettyHttpRequest/NettyHttpResponse** - Request and response abstractions

### Key Features

#### Pluggable Handlers
```java
// Create a custom handler
public class MyHandler extends HttpHandler {
    @Override
    protected CompletableFuture<NettyHttpResponse> handler(NettyHttpRequest request) {
        return CompletableFuture.completedFuture(
            NettyHttpResponse.builder()
                .body("Hello, World!".getBytes())
                .build()
        );
    }
}

// Use it in the server
NettyHttpServer.createBuilder()
    .port(8080)
    .httpHandlerFactory(() -> new MyHandler())
    .build()
    .startServer();
```

#### Adding Filters
```java
// Create a custom filter
public class MyFilter extends HttpFilter {
    @Override
    protected boolean beforeHandler(NettyHttpRequest request) {
        // Add pre-processing logic
        return false; // Continue to next handler/filter
    }
    
    @Override
    protected void afterHandler(ChannelHandlerContext ctx, NettyHttpResponse response) {
        // Add post-processing logic
    }
}

// Add to server configuration
NettyHttpServer.createBuilder()
    .port(8080)
    .addHttpFilterFactory(() -> new MyFilter())
    .httpHandlerFactory(() -> new MyHandler())
    .build()
    .startServer();
```

#### HTTP/2 Support
```java
NettyHttpServer.createBuilder()
    .port(8443)
    .http2Support(true)  // Enable HTTP/2
    .maxConcurrentStreamSize(1000)  // Configure concurrent streams
    .httpHandlerFactory(() -> new MyHandler())
    .build()
    .startServer();
```

## Configuration Options

| Parameter | Default | Description |
|-----------|---------|-------------|
| `port()` | 8080 | Server port |
| `bossThreadSize()` | 1 | Number of boss threads |
| `workThreadSize()` | Runtime.availableProcessors() | Number of worker threads |
| `maxLength()` | 10240 | Maximum content length in bytes |
| `connectionIdleTimeout()` | 0 | Connection idle timeout in ms (0 = no timeout) |
| `http2Support()` | false | Enable HTTP/2 support |
| `maxConcurrentStreamSize()` | 5000 | Max concurrent HTTP/2 streams |

## Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd netty-demo

# Build the project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn package
```

## Project Structure

```
src/
├── main/java/cn/flaty/netty/http2/
│   ├── NettyHttpServer.java           # Main server class
│   ├── HttpHandler.java               # Abstract request handler
│   ├── HttpFilter.java               # Abstract request filter
│   ├── NettyHttpRequest.java        # HTTP request abstraction
│   ├── NettyHttpResponse.java        # HTTP response builder
│   ├── http1/                        # HTTP/1.1 specific implementations
│   └── http2/                        # HTTP/2 specific implementations
└── test/java/cn/flaty/http2/
    └── server/                       # Server benchmark examples
```

## Dependencies

- Netty 4.1.78.Final
- Vertx 4.3.2
- Guava 30.1.1
- Lombok
- SLF4J + Logback
- JUnit 4
- Apache Commons Collections and Lang

## Examples

The project includes several examples:

1. **Http1ServerBenchMark** - Basic HTTP/1.1 server with a simple handler
2. **Http2ServerBenchMark** - HTTP/2 server with the same handler
3. **MyHttpClient** - Simple client for testing

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=FutureTest

# Run with coverage
mvn clean test jacoco:report
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is for educational purposes.