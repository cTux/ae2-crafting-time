package com.ctux.ae2craftingtime.testdriver;

import com.google.gson.Gson;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import net.minecraft.client.Minecraft;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public final class InteractiveMcpServer implements AutoCloseable {
    private static final Map<String, Object> EMPTY_SCHEMA = Map.of(
            "type", "object", "properties", Map.of(), "additionalProperties", false);
    private final Minecraft minecraft;
    private final CraftPlanScenario scenario;
    private final DriverOptions options;
    private final DriverScheduler scheduler = new DriverScheduler(16, Duration.ofSeconds(5));
    private final Gson gson = new Gson();
    private final HttpServletStreamableServerTransportProvider transport;
    private final McpSyncServer server;
    private final Tomcat tomcat;
    private int screenshotIndex;

    public InteractiveMcpServer(Minecraft minecraft, CraftPlanScenario scenario, DriverOptions options) throws Exception {
        this.minecraft = minecraft;
        this.scenario = scenario;
        this.options = options;
        var token = System.getenv("AE2CT_TEST_DRIVER_TOKEN");
        var policy = new EndpointPolicy(token);
        transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .securityValidator(DefaultServerTransportSecurityValidator.builder()
                        .allowedHost("127.0.0.1:*").allowedOrigin("http://127.0.0.1:*").build())
                .build();
        server = McpServer.sync(transport)
                .serverInfo("ae2-crafting-time-test-driver", "1")
                .capabilities(ServerCapabilities.builder().tools(false).build())
                .tools(tools())
                .requestTimeout(Duration.ofSeconds(5))
                .build();
        tomcat = createTomcat(policy);
        tomcat.start();
        var port = tomcat.getConnector().getLocalPort();
        Files.createDirectories(options.output());
        Files.writeString(options.output().resolve("mcp-endpoint.json"),
                "{\"url\":\"http://127.0.0.1:" + port + "/mcp\"}\n", StandardCharsets.UTF_8);
    }

    private Tomcat createTomcat(EndpointPolicy policy) {
        var value = new Tomcat();
        value.setHostname("127.0.0.1");
        value.setPort(0);
        value.setBaseDir(options.output().resolve("tomcat").toString());
        Context context = value.addContext("", options.output().toString());
        var filterDefinition = new FilterDef();
        filterDefinition.setFilterName("testDriverSecurity");
        filterDefinition.setFilter(new RequestFilter(policy));
        context.addFilterDef(filterDefinition);
        var filterMapping = new FilterMap();
        filterMapping.setFilterName("testDriverSecurity");
        filterMapping.addURLPattern("/*");
        context.addFilterMapBefore(filterMapping);
        var wrapper = context.createWrapper();
        wrapper.setName("mcp");
        wrapper.setServlet(transport);
        wrapper.setLoadOnStartup(1);
        wrapper.setAsyncSupported(true);
        context.addChild(wrapper);
        context.addServletMappingDecoded("/*", "mcp");
        var connector = value.getConnector();
        connector.setProperty("address", "127.0.0.1");
        connector.setMaxPostSize(EndpointPolicy.MAX_REQUEST_BYTES);
        connector.setAsyncTimeout(5000);
        return value;
    }

    private List<McpServerFeatures.SyncToolSpecification> tools() {
        return List.of(
                tool("minecraft_get_state", () -> Map.of(
                        "state", scenario.state().name(), "elapsedMs", scenario.elapsedMillis(),
                        "failure", scenario.failure() == null ? "" : scenario.failure())),
                tool("minecraft_get_screen", () -> {
                    var snapshot = UiObservationStore.latest();
                    return snapshot == null ? Map.of("screen", "none") : Map.of(
                            "screen", snapshot.screen(), "menu", snapshot.menu(), "gui", snapshot.gui(),
                            "width", snapshot.screenWidth(), "height", snapshot.screenHeight(),
                            "scale", snapshot.guiScale());
                }),
                tool("minecraft_get_ui_snapshot", UiObservationStore::latest),
                tool("minecraft_take_screenshot", this::takeScreenshot),
                tool("minecraft_get_logs", this::logs),
                tool("minecraft_quit", () -> {
                    minecraft.stop();
                    return Map.of("quitting", true);
                }));
    }

    private McpServerFeatures.SyncToolSpecification tool(String name, Callable<?> action) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(Tool.builder(name, EMPTY_SCHEMA).description(name.replace('_', ' ')).build())
                .callHandler((exchange, request) -> {
                    try {
                        var response = scheduler.call(minecraft, action);
                        var json = EndpointPolicy.bounded(gson.toJson(response));
                        return CallToolResult.builder().content(List.of(TextContent.builder(json).build()))
                                .isError(false).build();
                    } catch (Exception error) {
                        return CallToolResult.builder().content(List.of(TextContent.builder(
                                error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage())).build()))
                                .isError(true).build();
                    }
                }).build();
    }

    private Object takeScreenshot() throws IOException {
        var name = "interactive-" + ++screenshotIndex + ".png";
        try (var image = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            image.writeToFile(options.output().resolve(name));
        }
        return Map.of("screenshot", name);
    }

    private Object logs() throws IOException {
        var path = minecraft.gameDirectory.toPath().resolve("logs").resolve("latest.log");
        if (!Files.exists(path)) {
            return Map.of("log", "");
        }
        var bytes = Files.readAllBytes(path);
        var start = Math.max(0, bytes.length - EndpointPolicy.MAX_REQUEST_BYTES);
        return Map.of("log", new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception {
        server.closeGracefully();
        tomcat.stop();
        tomcat.destroy();
    }

    private static final class RequestFilter implements Filter {
        private final EndpointPolicy policy;

        private RequestFilter(EndpointPolicy policy) {
            this.policy = policy;
        }

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
                throws IOException, ServletException {
            var request = (HttpServletRequest) servletRequest;
            var response = (HttpServletResponse) servletResponse;
            var startsController = "POST".equals(request.getMethod()) && request.getHeader("mcp-session-id") == null;
            if (!policy.authenticate(request.getRemoteAddr(), request.getHeader("Authorization"),
                    request.getContentLengthLong(), startsController)) {
                response.sendError(startsController ? 409 : 401);
                return;
            }
            chain.doFilter(new LimitedRequest(request), response);
        }

        @Override
        public void destroy() {
        }
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private ServletInputStream input;

        private LimitedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (input == null) {
                input = new LimitedInputStream(super.getInputStream());
            }
            return input;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private int read;

        private LimitedInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            var value = delegate.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            var count = delegate.read(bytes, offset, Math.min(length, EndpointPolicy.MAX_REQUEST_BYTES + 1));
            if (count > 0) {
                count(count);
            }
            return count;
        }

        private void count(int count) throws IOException {
            read += count;
            if (read > EndpointPolicy.MAX_REQUEST_BYTES) {
                throw new IOException("MCP request exceeds 64 KiB");
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
