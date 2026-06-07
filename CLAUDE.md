# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This is a Netty-based HTTP/HTTP2 server implementation demonstrating enterprise-level patterns for handling HTTP requests. The project showcases both HTTP/1.1 and HTTP/2 protocol support with a flexible, pluggable architecture.

## Build and Development Commands

### Maven Commands
- **Build the project**: `mvn clean compile`
- **Run tests**: `mvn test`
- **Package the application**: `mvn package`
- **Skip tests during build**: `mvn package -DskipTests`
- **Run with specific Maven profile**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`

### Running the Server
The project includes two benchmark classes that demonstrate how to start the server:

#### HTTP/1.1 Server
```bash
mvn exec:java -Dexec.mainClass="cn.flaty.http2.server.Http1ServerBenchMark"
```
Or run directly:
```java
NettyHttpServer.createBuilder()
    .port(6667)
    .connectionIdleTimeout(3000)
    .httpHandlerFactory(() -> new AppHandler())
    .build()
    .startServer();
```

#### HTTP/2 Server
```bash
mvn exec:java -Dexec.mainClass="cn.flaty.http2.server.Http2ServerBenchMark"
```
Or run directly:
```java
NettyHttpServer.createBuilder()
    .port(6667)
    .connectionIdleTimeout(3000)
    .httpHandlerFactory(() -> new AppHandler())
    .http2Support(true)
    .build()
    .startServer();
```

## Architecture Overview

### Core Components

1. **NettyHttpServer** - Main server class with builder pattern configuration
   - Configurable port, thread pools, connection timeouts
   - Support for HTTP/1.1 and HTTP/2 protocols
   - Pluggable filters and handlers

2. **HttpHandler** - Abstract base class for request processing
   - Uses CompletableFuture for async response handling
   - Built-in timeout management
   - Customizable exception handling
   - Uses ForkJoinPool.commonPool() for request processing

3. **HttpFilter** - Base class for request/response interception
   - Implements ChannelDuplexHandler for full pipeline control
   - beforeHandler() and afterHandler() hooks for request/response lifecycle
   - Automatic resource cleanup via ReferenceCountUtil

4. **Request/Response Objects**
   - NettyHttpRequest - Abstract HTTP request representation
   - NettyHttpResponse - HTTP response builder with fluent API
   - Protocol-specific implementations: Http1ToRequestHandler, Http2ToRequestHandler

### Key Design Patterns

- **Builder Pattern**: Used extensively in NettyHttpServer for configuration
- **Template Method**: HttpHandler defines the structure, subclasses implement specific logic
- **Chain of Responsibility**: HttpFilter creates middleware pipeline
- **Strategy Pattern**: Different protocol handlers (HTTP/1.1 vs HTTP/2)
- **Factory Pattern**: HttpHandlerFactory and HttpFilterFactory for creating instances

### Protocol Support

The server supports both protocols through separate channel initializers:
- HTTP/1.1: `Http1Initializer`
- HTTP/2: `Http2PriorKnowledgeHandlerInitializer` with `Http2StreamChannelInitializer`

### Resource Management

- Uses PooledByteBufAllocator for buffer pooling
- Configurable connection limits and timeouts
- Automatic resource leak detection (ADVANCED level)
- Built-in idle connection handling

### Threading Model

- Boss threads: Handle incoming connections (configurable size, default: 1)
- Worker threads: Process I/O operations (configurable, default: available processors)
- Request processing: ForkJoinPool.commonPool() for async execution
- Timeout handling: CompletableFuture with timeout support

### Configuration Options

Key configurable parameters in NettyHttpServer.Builder:
- `port()` - Server port (default: 8080)
- `bossThreadSize()` - Number of boss threads (default: 1)
- `workThreadSize()` - Number of worker threads (default: available processors)
- `maxLength()` - Maximum content length (default: 10KB)
- `connectionIdleTimeout()` - Connection idle timeout in milliseconds
- `http2Support()` - Enable HTTP/2 support (default: false)
- `maxConcurrentStreamSize()` - Max concurrent streams for HTTP/2 (default: 5000)

### Testing

The project includes:
- Benchmark classes for performance testing
- Simple client implementation (MyHttpClient)
- CompletableFuture test utilities

## Key Dependencies

- Netty 4.1.78.Final - Core networking framework
- Vertx 4.3.2 - Additional utilities
- Guava 30.1.1 - Google libraries
- Lombok - Code generation
- SLF4J + Logback - Logging
- JUnit 4 - Testing framework
- Apache Commons Collections and Lang - Utility libraries

## Important Implementation Notes

1. **Response Handling**: All handlers must return CompletableFuture<NettyHttpResponse>
2. **Timeout Management**: Default timeout is 5 seconds, configurable per handler
3. **Memory Management**: Uses pooled buffers and automatic resource cleanup
4. **Protocol Negotiation**: HTTP/2 requires explicit configuration
5. **Thread Safety**: Handler instances should be stateless or thread-safe
6. **Error Handling**: Custom exception responses can be implemented in handler subclasses