package java.app.service.time;

import java.app.entity.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Сервис для управления временными интервалами задач.
 * Реализует механизм проверки пересечений задач по времени с использованием временных слотов.
 * Каждый временной слот представляет собой интервал длиной 15 минут.
 */
public class TimeManagerService implements TimeManager {
    private final Map<LocalDateTime, Integer> timeSlots = new HashMap<>();
    private static final Duration TIME_SLOT_SIZE = Duration.ofMinutes(15);

    /**
     * Проверяет, не пересекается ли задача по времени с уже существующими задачами.
     * Если пересечение обнаружено, выбрасывает исключение RuntimeException.
     *
     * @param task задача для проверки временного пересечения
     * @throws RuntimeException если задача пересекается по времени с существующей задачей
     */
    public void validateTaskTime(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return;
        }

        if (!isTimeSlotAvailable(task.getStartTime(), task.getEndTime())) {
            throw new RuntimeException("Задача '" + task.getName() +
                    "' пересекается по времени с существующей задачей");
        }
    }

    /**
     * Возвращает набор временных слотов, которые пересекаются с указанным временным интервалом.
     * Временные слоты имеют фиксированную длительность 15 минут.
     *
     * @param start начало временного интервала
     * @param end конец временного интервала
     * @return множество временных слотов, пересекающихся с указанным интервалом.
     *         Возвращает пустое множество, если start или end равны null, либо start после end.
     */
    public Set<LocalDateTime> getTimeSlotsForInterval(LocalDateTime start, LocalDateTime end) {
        Set<LocalDateTime> slots = new HashSet<>();

        if (start == null || end == null || start.isAfter(end)) {
            return slots;
        }

        LocalDateTime current = start.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime endTruncated = end.truncatedTo(ChronoUnit.MINUTES);

        long maxMinutes = Duration.between(start, end).toMinutes();
        long maxSlots = (maxMinutes / TIME_SLOT_SIZE.toMinutes()) + 2;

        for (int i = 0; i < maxSlots && !current.isAfter(endTruncated); i++) {
            slots.add(current);
            current = current.plus(TIME_SLOT_SIZE);
        }

        return slots;
    }

    /**
     * Добавляет задачу во все соответствующие временные слоты.
     * Увеличивает счетчик задач для каждого временного слота, который пересекается с задачей.
     *
     * @param task задача для добавления во временные слоты
     */
    public void addTaskToTimeSlots(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) return;

        LocalDateTime start = task.getStartTime();
        LocalDateTime end = task.getEndTime();
        Set<LocalDateTime> slots = getTimeSlotsForInterval(start, end);

        for (LocalDateTime slot : slots) {
            timeSlots.put(slot, timeSlots.getOrDefault(slot, 0) + 1);
        }
    }

    /**
     * Удаляет задачу из временных слотов.
     * Уменьшает счетчик задач для каждого временного слота, который пересекается с задачей.
     * Если счетчик становится равным нулю, временной слот удаляется из карты.
     *
     * @param task задача для удаления из временных слотов
     */
    public void removeTaskFromTimeSlots(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) return;

        LocalDateTime start = task.getStartTime();
        LocalDateTime end = task.getEndTime();
        Set<LocalDateTime> slots = getTimeSlotsForInterval(start, end);

        for (LocalDateTime slot : slots) {
            Integer count = timeSlots.get(slot);
            if (count != null) {
                if (count == 1) {
                    timeSlots.remove(slot);
                } else {
                    timeSlots.put(slot, count - 1);
                }
            }
        }
    }

    /**
     * Проверяет, свободны ли все временные слоты в указанном интервале.
     *
     * @param start начало проверяемого интервала
     * @param end конец проверяемого интервала
     * @return true если все временные слоты в интервале свободны,
     *         false если хотя бы один слот занят другой задачей.
     *         Возвращает true, если start или end равны null, либо start после end.
     */
    @Override
    public boolean isTimeSlotAvailable(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || start.isAfter(end)) {
            return true;
        }

        Set<LocalDateTime> slots = getTimeSlotsForInterval(start, end);
        return slots.stream().noneMatch(slot ->
                timeSlots.containsKey(slot) && !slot.equals(end));
    }
}