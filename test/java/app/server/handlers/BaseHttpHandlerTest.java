package java.app.server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.app.management.TaskManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Тесты для BaseHttpHandler")
class BaseHttpHandlerTest {

    private BaseHttpHandler handler;
    private Gson gson;
    private HttpExchange exchange;
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        TaskManager taskManager = mock(TaskManager.class);
        gson = new Gson();
        handler = new BaseHttpHandler(taskManager, gson) {
            @Override
            public void handle(HttpExchange exchange) {
            }
        };
        exchange = mock(HttpExchange.class);

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
            when(exchange.getRequestBody()).thenReturn(inputStream);

            // When
            String actualBody = handler.readRequestBody(exchange);

            // Then
            assertEquals(expectedBody, actualBody);
        }

        @Test
        @DisplayName("Должен бросать исключение при ошибке чтения")
        void shouldThrowExceptionOnReadError() throws IOException {
            // Given
            InputStream inputStream = mock(InputStream.class);
            when(exchange.getRequestBody()).thenReturn(inputStream);
            when(inputStream.readAllBytes()).thenThrow(new IOException("Read error"));

            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> handler.readRequestBody(exchange));
            assertEquals("Read error", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Отправка ответов")
    class SendResponseTest {

        private ByteArrayOutputStream outputStream;

        @BeforeEach
        void setUp() {
            outputStream = new ByteArrayOutputStream();
            when(exchange.getResponseBody()).thenReturn(outputStream);
            Headers headers = mock(Headers.class);
            when(exchange.getResponseHeaders()).thenReturn(headers);
        }

        @Test
        @DisplayName("Должен отправлять текстовый ответ с корректными заголовками")
        void shouldSendTextResponse() throws IOException {
            // Given
            String response = "test response";
            int statusCode = 200;

            // When
            handler.sendText(exchange, response, statusCode);

            // Then
            verify(exchange).sendResponseHeaders(statusCode, response.getBytes().length);
            assertEquals(response, outputStream.toString());
        }

        @Test
        @DisplayName("Должен отправлять успешный ответ")
        void shouldSendSuccessResponse() throws IOException {
            // Given
            TestObject testObject = new TestObject();
            when(exchange.getResponseBody()).thenReturn(outputStream);
            Headers headers = mock(Headers.class);
            when(exchange.getResponseHeaders()).thenReturn(headers);

            // When
            handler.sendSuccess(exchange, testObject);

            // Then
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(exchange).sendResponseHeaders(statusCaptor.capture(), anyLong());
            assertEquals(200, statusCaptor.getValue());
            assertEquals(gson.toJson(testObject), outputStream.toString());
        }

        @Test
        @DisplayName("Должен отправлять ответ 201 Created")
        void shouldSendCreatedResponse() throws IOException {
            // Given
            TestObject testObject = new TestObject();
            when(exchange.getResponseBody()).thenReturn(outputStream);
            Headers headers = mock(Headers.class);
            when(exchange.getResponseHeaders()).thenReturn(headers);

            // When
            handler.sendCreated(exchange, testObject);

            // Then
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(exchange).sendResponseHeaders(statusCaptor.capture(), anyLong());
            assertEquals(201, statusCaptor.getValue());
        }

        @Test
        @DisplayName("Должен отправлять ответ 404 Not Found")
        void shouldSendNotFoundResponse() throws IOException {
            // Given
            String errorMessage = "Resource not found";
            when(exchange.getResponseBody()).thenReturn(outputStream);
            Headers headers = mock(Headers.class);
            when(exchange.getResponseHeaders()).thenReturn(headers);

            // When
            handler.sendNotFound(exchange, errorMessage);

            // Then
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(exchange).sendResponseHeaders(statusCaptor.capture(), anyLong());
            assertEquals(404, statusCaptor.getValue());
            assertTrue(outputStream.toString().contains(errorMessage));
        }

        @Test
        @DisplayName("Должен отправлять ответ 204 No Content")
        void shouldSendNoContentResponse() throws IOException {
            // When
            handler.sendNoContent(exchange);

            // Then
            verify(exchange).sendResponseHeaders(204, -1);
            verify(exchange, never()).getResponseBody();
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
            when(exchange.getRequestURI()).thenReturn(uri);

            // When
            String param = handler.getPathParameter(exchange, 1);

            // Then
            assertEquals("123", param);
        }

        @Test
        @DisplayName("Должен возвращать null для несуществующего индекса")
        void shouldReturnNullForNonExistentIndex() {
            // Given
            URI uri = URI.create("http://localhost:8080/tasks");
            when(exchange.getRequestURI()).thenReturn(uri);

            // When
            String param = handler.getPathParameter(exchange, 1);

            // Then
            assertNull(param);
        }
    }

    private static class TestObject {
        public TestObject() {
        }
    }
}