package taskmanager.app.server;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

/**
 * Адаптер Gson для сериализации и десериализации объектов {@link Duration}.
 * Преобразует длительность в минуты при сериализации в JSON и обратно при десериализации.
 */
public class DurationAdapter extends TypeAdapter<Duration> {

    /**
     * Сериализует объект {@link Duration} в JSON.
     * Записывает длительность в виде количества минут.
     *
     * @param out писатель JSON для записи значения
     * @param value длительность для сериализации, может быть null
     * @throws IOException если произошла ошибка ввода-вывода при записи
     */
    @Override
    public void write(JsonWriter out, Duration value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toMinutes());
        }
    }

    /**
     * Десериализует объект {@link Duration} из JSON.
     * Читает значение как количество минут и создает объект Duration.
     *
     * @param in читатель JSON для чтения значения
     * @return объект Duration, созданный из количества минут, или null если значение null
     * @throws IOException если произошла ошибка ввода-вывода при чтении
     * @throws NumberFormatException если значение не является допустимым числом
     * @throws IllegalStateException если следующий токен не является числом или null
     */
    @Override
    public Duration read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        long minutes = in.nextLong();
        return Duration.ofMinutes(minutes);
    }
}