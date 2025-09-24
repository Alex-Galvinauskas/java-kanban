package java.app.server;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Адаптер Gson для сериализации и десериализации объектов {@link LocalDateTime}.
 * Преобразует дату и время в строку формата ISO 8601 (yyyy-MM-dd'T'HH:mm:ss)
 * при сериализации в JSON и обратно при десериализации.
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Сериализует объект {@link LocalDateTime} в JSON.
     * Записывает дату и время в виде строки формата ISO 8601.
     *
     * @param out писатель JSON для записи значения
     * @param value дата и время для сериализации, может быть null
     * @throws IOException если произошла ошибка ввода-вывода при записи
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(formatter));
        }
    }

    /**
     * Десериализует объект {@link LocalDateTime} из JSON.
     * Читает значение как строку в формате ISO 8601 и создает объект LocalDateTime.
     *
     * @param in читатель JSON для чтения значения
     * @return объект LocalDateTime, созданный из строкового представления,
     *         или null если значение null или пустое
     * @throws IOException если произошла ошибка ввода-вывода при чтении
     * @throws java.time.format.DateTimeParseException если строка не соответствует формату ISO 8601
     */
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String value = in.nextString();
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value, formatter);
    }
}