package taskmanager.app.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Тесты для LocalDateTimeAdapter")
class LocalDateTimeAdapterTest {

    private final LocalDateTimeAdapter adapter = new LocalDateTimeAdapter();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, adapter)
            .create();
    private TestInfo testInfo;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        this.testInfo = testInfo;
        System.out.printf("🚀 Подготовка теста: %s%n", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown() {
        System.out.printf("✅ Тест завершен: %s%n%n", testInfo.getDisplayName());
    }

    @Nested
    @DisplayName("Сериализация LocalDateTime")
    class SerializationTest {

        @Test
        @DisplayName("Должен корректно сериализовать LocalDateTime в ISO формат")
        void shouldSerializeToIsoFormat() throws IOException {
            // Given
            LocalDateTime dateTime = LocalDateTime.of(2023, 12, 1, 14, 30, 45);
            StringWriter writer = new StringWriter();
            com.google.gson.stream.JsonWriter jsonWriter = new com.google.gson.stream.JsonWriter(writer);

            // When
            adapter.write(jsonWriter, dateTime);
            jsonWriter.flush();

            // Then
            assertEquals("\"2023-12-01T14:30:45\"", writer.toString());
        }

        @Test
        @DisplayName("Должен сериализовать null значение")
        void shouldSerializeNullValue() throws IOException {
            // Given
            StringWriter writer = new StringWriter();
            com.google.gson.stream.JsonWriter jsonWriter = new com.google.gson.stream.JsonWriter(writer);

            // When
            adapter.write(jsonWriter, null);
            jsonWriter.flush();

            // Then
            assertEquals("null", writer.toString());
        }

        @Test
        @DisplayName("Должен сериализовать через Gson")
        void shouldSerializeWithGson() {
            // Given
            LocalDateTime dateTime = LocalDateTime.of(2023, 10, 5, 9, 15, 0);

            // When
            String json = gson.toJson(dateTime);

            // Then
            assertEquals("\"2023-10-05T09:15:00\"", json);
        }
    }

    @Nested
    @DisplayName("Десериализация LocalDateTime")
    class DeserializationTest {

        @Test
        @DisplayName("Должен корректно десериализовать ISO строку в LocalDateTime")
        void shouldDeserializeIsoStringToLocalDateTime() throws IOException {
            // Given
            String json = "\"2023-12-01T14:30:45\"";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When
            LocalDateTime result = adapter.read(reader);

            // Then
            assertEquals(LocalDateTime.of(2023, 12, 1, 14, 30, 45), result);
        }

        @Test
        @DisplayName("Должен десериализовать null значение")
        void shouldDeserializeNullValue() throws IOException {
            // Given
            String json = "null";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When
            LocalDateTime result = adapter.read(reader);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Должен десериализовать через Gson")
        void shouldDeserializeWithGson() {
            // Given
            String json = "\"2023-08-15T18:45:30\"";

            // When
            LocalDateTime dateTime = gson.fromJson(json, LocalDateTime.class);

            // Then
            assertEquals(LocalDateTime.of(2023, 8, 15, 18, 45, 30), dateTime);
        }

        @Test
        @DisplayName("Должен возвращать null для пустой строки")
        void shouldReturnNullForEmptyString() throws IOException {
            // Given
            String json = "\"\"";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When
            LocalDateTime result = adapter.read(reader);

            // Then
            assertNull(result);
        }
    }
}