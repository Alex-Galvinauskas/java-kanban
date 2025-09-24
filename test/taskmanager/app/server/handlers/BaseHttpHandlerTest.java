package taskmanager.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.*;
import taskmanager.app.management.Managers;
import taskmanager.app.management.TaskManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Тесты для BaseHttpHandler")
class BaseHttpHandlerTest {

    private BaseHttpHandler handler;
    private Gson gson;
    private HttpExchange exchange;
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        TaskManager taskManager = Managers.getDefault();
        gson = new Gson();
        handler = new BaseHttpHandler(taskManager, gson) {
            @Override
            public void handle(HttpExchange exchange) {
            }
        };
        exchange = new ReadRequestBodyTest.StubHttpExchange();

        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown() {
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("Чтение тела запроса")
    class ReadRequestBodyTest {

        @Test
        @DisplayName("Должен корректно читать тело запроса")
        void shouldReadRequestBody() throws IOException {
            // Given
            String expectedBody = "{\"name\":\"test\"}";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(expectedBody.getBytes());
            StubHttpExchange stubExchange = new StubHttpExchange();
            stubExchange.setRequestBody(inputStream);

            // When
            String actualBody = handler.readRequestBody(stubExchange);

            // Then
            assertThat(actualBody).isEqualTo(expectedBody);
        }

        @Test
        @DisplayName("Должен бросать исключение при ошибке чтения")
        void shouldThrowExceptionOnReadError() {
            // Given
            try (InputStream inputStream = new InputStream() {
                private boolean closed = false;

                @Override
                public int read() throws IOException {
                    if (closed) {
                        throw new IOException("Stream closed");
                    }
                    throw new IOException("Read error");
                }

                @Override
                public byte[] readAllBytes() throws IOException {
                    if (closed) {
                        throw new IOException("Stream closed");
                    }
                    throw new IOException("Read error");
                }

                @Override
                public void close() throws IOException {
                    closed = true;
                    super.close();
                }
            }) {
                StubHttpExchange stubExchange = new StubHttpExchange();
                stubExchange.setRequestBody(inputStream);

                // When & Then
                assertThatThrownBy(() -> handler.readRequestBody(stubExchange))
                        .isInstanceOf(IOException.class)
                        .hasMessage("Read error");
            } catch (IOException e) {
                fail("Unexpected IOException during stream closing: " + e.getMessage());
            }
        }

        @Nested
        @DisplayName("Отправка ответов")
        class SendResponseTest {

            private ByteArrayOutputStream outputStream;
            private StubHttpExchange stubExchange;

            @BeforeEach
            void setUp() {
                outputStream = new ByteArrayOutputStream();
                stubExchange = new StubHttpExchange();
                stubExchange.setResponseBody(outputStream);
            }

            @Test
            @DisplayName("Должен отправлять текстовый ответ с корректными заголовками")
            void shouldSendTextResponse() throws IOException {
                // Given
                String response = "test response";
                int statusCode = 200;

                // When
                handler.sendText(stubExchange, response, statusCode);

                // Then
                assertThat(stubExchange.getResponseCode()).isEqualTo(statusCode);
                assertThat(stubExchange.getResponseLength()).isEqualTo(response.getBytes().length);
                assertThat(outputStream.toString()).isEqualTo(response);
            }

            @Test
            @DisplayName("Должен отправлять успешный ответ")
            void shouldSendSuccessResponse() throws IOException {
                // Given
                TestObject testObject = new TestObject();

                // When
                handler.sendSuccess(stubExchange, testObject);

                // Then
                assertThat(stubExchange.getResponseCode()).isEqualTo(200);
                assertThat(outputStream.toString()).isEqualTo(gson.toJson(testObject));
            }

            @Test
            @DisplayName("Должен отправлять ответ 201 Created")
            void shouldSendCreatedResponse() throws IOException {
                // Given
                TestObject testObject = new TestObject();

                // When
                handler.sendCreated(stubExchange, testObject);

                // Then
                assertThat(stubExchange.getResponseCode()).isEqualTo(201);
                assertThat(outputStream.toString()).isEqualTo(gson.toJson(testObject));
            }

            @Test
            @DisplayName("Должен отправлять ответ 404 Not Found")
            void shouldSendNotFoundResponse() throws IOException {
                // Given
                String errorMessage = "Resource not found";

                // When
                handler.sendNotFound(stubExchange, errorMessage);

                // Then
                assertThat(stubExchange.getResponseCode()).isEqualTo(404);
                assertThat(outputStream.toString()).contains(errorMessage);
            }

            @Test
            @DisplayName("Должен отправлять ответ 204 No Content")
            void shouldSendNoContentResponse() throws IOException {
                // When
                handler.sendNoContent(stubExchange);

                // Then
                assertThat(stubExchange.getResponseCode()).isEqualTo(204);
                assertThat(stubExchange.getResponseLength()).isEqualTo(-1);
                assertThat(stubExchange.isResponseBodyAccessed()).isFalse();
            }
        }

        @Nested
        @DisplayName("Извлечение параметров пути")
        class PathParameterTest {

            @Test
            @DisplayName("Должен извлекать параметр по индексу")
            void shouldExtractPathParameter() {
                // Given
                URI uri = URI.create("http://localhost:8080/tasks/123");
                StubHttpExchange stubExchange = new StubHttpExchange();
                stubExchange.setRequestURI(uri);

                // When
                String param = handler.getPathParameter(stubExchange, 1);

                // Then
                assertThat(param).isEqualTo("123");
            }

            @Test
            @DisplayName("Должен возвращать null для несуществующего индекса")
            void shouldReturnNullForNonExistentIndex() {
                // Given
                URI uri = URI.create("http://localhost:8080/tasks");
                StubHttpExchange stubExchange = new StubHttpExchange();
                stubExchange.setRequestURI(uri);

                // When
                String param = handler.getPathParameter(stubExchange, 1);

                // Then
                assertThat(param).isNull();
            }
        }

        private static class StubHttpExchange extends HttpExchange {
            private InputStream requestBody;
            private ByteArrayOutputStream responseBody;
            private URI requestURI;
            private int responseCode;
            private long responseLength;
            private boolean responseBodyAccessed = false;
            private final Headers responseHeaders = new Headers();
            private final Headers requestHeaders = new Headers();

            public void setRequestBody(InputStream requestBody) {
                this.requestBody = requestBody;
            }

            public void setResponseBody(ByteArrayOutputStream responseBody) {
                this.responseBody = responseBody;
            }

            public void setRequestURI(URI requestURI) {
                this.requestURI = requestURI;
            }

            public int getResponseCode() {
                return responseCode;
            }

            public long getResponseLength() {
                return responseLength;
            }

            public boolean isResponseBodyAccessed() {
                return responseBodyAccessed;
            }

            @Override
            public Headers getRequestHeaders() {
                return requestHeaders;
            }

            @Override
            public Headers getResponseHeaders() {
                return responseHeaders;
            }

            @Override
            public URI getRequestURI() {
                return requestURI;
            }

            @Override
            public String getRequestMethod() {
                return "GET";
            }

            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            @Override
            public void close() {
            }

            @Override
            public InputStream getRequestBody() {
                return requestBody;
            }

            @Override
            public ByteArrayOutputStream getResponseBody() {
                responseBodyAccessed = true;
                return responseBody;
            }

            @Override
            public void sendResponseHeaders(int code, long length) {
                this.responseCode = code;
                this.responseLength = length;
            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public String getProtocol() {
                return "HTTP/1.1";
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public void setAttribute(String name, Object value) {
            }

            @Override
            public void setStreams(InputStream i, OutputStream o) {
            }

            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        }

        private static class TestObject {

            public TestObject() {
            }

        }
    }
}