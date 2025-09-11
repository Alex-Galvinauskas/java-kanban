package taskmanager.app.service.time;

import taskmanager.app.entity.Task;

import java.time.LocalDateTime;
import java.util.Set;

public interface TimeManager {
    void validateTaskTime(Task task);
    void addTaskToTimeSlots(Task task);
    void removeTaskFromTimeSlots(Task task);
    Set<LocalDateTime> getTimeSlotsForInterval(LocalDateTime start, LocalDateTime end);
    boolean isTimeSlotAvailable(LocalDateTime start, LocalDateTime end);
}