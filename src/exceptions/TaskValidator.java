package exceptions;

import core.*;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class TaskValidator {

    //основные методы валидации
    public void validateTaskForCreation(Task task, Collection<Task> existingTasks) {
        validateNotNull(task, "Задача ");
        validatePositiveId(task.getId());
        validateNotDuplicate(task, existingTasks);
    }

    public void validateForEpicCreation(Epic epic) {
        validateNotNull(epic, "Эпик ");
        validatePositiveId(epic.getId());
    }

    public void validateForSubTaskCreation(SubTask subTask, Map<Integer, Epic> epics) {
        validateNotNull(subTask, "SubTask ");
        validatePositiveId(subTask.getId());
        validateEpicExist(epics, subTask.getEpicId());
    }

    public void validateTaskForUpdate(Task task, Map<Integer, Task> tasks) {
       validateNotNull(task, "Задача ");
       validateIdExist(tasks, task.getId());
       validateIdExist(tasks, task.getId());
    }

    public void validateSubTaskForUpdate(SubTask subTask, Map<Integer, SubTask> SubTasks, Map<Integer, Epic> epics) {
        validateNotNull(subTask, "SubTask ");
        validatePositiveId(subTask.getId());
        validateIdExist(SubTasks, subTask.getId());
        validateEpicExist(epics, subTask.getEpicId());
    }


    //вспомогательные методы валидации
    public void validatePositiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id не может быть отрицательным или равным нулю");
        }
    }

    public void validateNotNull(Object task, String entityName) {
        if (task == null) {
            throw new IllegalArgumentException(entityName + " не может быть null");
        }
    }

    public void validateIdExist(Map<?, ?> map, int id) {
        if (!map.containsKey(id)) {
            throw new IllegalArgumentException("Задача с id " + id + " не существует");
        }
    }

    public void validateEpicExist(Map<Integer, Epic> epics, int epicId) {
        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с id " + epicId + " не существует");
        }
    }

    public void validateNotDuplicate(Task newTask, Collection<Task> existingTasks) {
        boolean isDuplicate = existingTasks.stream()
                .anyMatch(task -> !isSameTask(newTask, task) && Objects.equals(task.getName(), newTask.getName()));
        if (isDuplicate) {
            throw new IllegalArgumentException("Такая задача " + newTask.getName() + " уже существует");
        }
    }

    private boolean isSameTask(Task task1, Task task2) {
        return task1.getId() == task2.getId() && task1.getName().equals(task2.getName());
    }
}

