package taskmanager.app.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для DurationAdapter")
class DurationAdapterTest {

    private final DurationAdapter adapter = new DurationAdapter();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, adapter)
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
    @DisplayName("Сериализация Duration")
    class SerializationTest {

        @Test
        @DisplayName("Должен корректно сериализовать Duration в минуты")
        void shouldSerializeDurationToMinutes() throws IOException {
            // Given
            Duration duration = Duration.ofHours(2).plusMinutes(30); // 150 минут
            StringWriter writer = new StringWriter();
            com.google.gson.stream.JsonWriter jsonWriter = new com.google.gson.stream.JsonWriter(writer);

            // When
            adapter.write(jsonWriter, duration);
            jsonWriter.flush();

            // Then
            assertEquals("150", writer.toString());
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
            Duration duration = Duration.ofMinutes(45);

            // When
            String json = gson.toJson(duration);

            // Then
            assertEquals("45", json);
        }
    }

    @Nested
    @DisplayName("Десериализация Duration")
    class DeserializationTest {

        @Test
        @DisplayName("Должен корректно десериализовать минуты в Duration")
        void shouldDeserializeMinutesToDuration() throws IOException {
            // Given
            String json = "120";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When
            Duration result = adapter.read(reader);

            // Then
            assertEquals(Duration.ofHours(2), result);
        }

        @Test
        @DisplayName("Должен десериализовать null значение")
        void shouldDeserializeNullValue() throws IOException {
            // Given
            String json = "null";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When
            Duration result = adapter.read(reader);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Должен десериализовать через Gson")
        void shouldDeserializeWithGson() {
            // Given
            String json = "90";

            // When
            Duration duration = gson.fromJson(json, Duration.class);

            // Then
            assertEquals(Duration.ofMinutes(90), duration);
        }

        @Test
        @DisplayName("Должен бросать исключение при нечисловом значении")
        void shouldThrowExceptionForNonNumber() {
            // Given
            String json = "\"invalid\"";
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new StringReader(json));

            // When & Then
            assertThrows(NumberFormatException.class, () -> adapter.read(reader));
        }
    }
}