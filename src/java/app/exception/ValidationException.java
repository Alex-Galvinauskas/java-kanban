package java.app.exception;


import java.app.entity.Epic;
import java.app.entity.SubTask;
import java.app.entity.Task;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Класс для проверки задач, эпиков и подзадач перед созданием и обновлением
 */
public class ValidationException {

    //основные методы валидации

    /**
     * Проверяет задачу перед созданием
     *
     * @param task          для проверки
     * @param existingTasks коллекция существующих задач
     */
    public void validateTaskForCreation(Task task, Collection<Task> existingTasks) {
        validateNotNull(task, "Задача ");
        validatePositiveId(task.getId());
        validateNotDuplicate(task, existingTasks);
    }

    /**
     * Проверяет положительность id
     *
     * @param id для проверки
     */
    public void validatePositiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Id не может быть отрицательным или равным нулю");
        }
    }

    /**
     * Проверяет на null
     *
     * @param task       для проверки
     * @param entityName название для сообщения об ошибке
     */
    public void validateNotNull(Object task, String entityName) {
        if (task == null) {
            throw new IllegalArgumentException(entityName + " не может быть null");
        }
    }

    /**
     * Проверяет на дубликаты по имени
     *
     * @param newTask       новая задача для проверки
     * @param existingTasks существующие задачи для проверки
     *
     * @return true, если задача не дублируется, иначе false
     */
    public boolean validateNotDuplicate(Task newTask, Collection<Task> existingTasks) {
        if (existingTasks == null || existingTasks.isEmpty()) {
            return true;
        }
        boolean isDuplicate = existingTasks.stream()
                .anyMatch(task -> !isSameTask(newTask, task)
                        && Objects.equals(task.getName(), newTask.getName()));
        if (isDuplicate) {
            throw new IllegalArgumentException("Такая задача " + newTask.getName() + " уже существует");
        }
        return false;
    }

    /**
     * Проверяет, являются ли задачи одинаковыми если:
     * Это один и тот же объект, или имеют одинаковые id и имена
     *
     * @param task1 для сравнения
     * @param task2 для сравнения
     *
     * @return true, если задачи одинаковые, иначе false
     */
    public boolean isSameTask(Task task1, Task task2) {
        if (task1 == task2) {
            return true;
        }

        if (task1 == null || task2 == null) {
            return false;
        }

        return Objects.equals(task1.getId(), task2.getId())
                && Objects.equals(task1.getName(), task2.getName());
    }

    //вспомогательные методы валидации

    /**
     * Проверяет эпик перед обновлением
     *
     * @param epic для проверки
     */
    public void validateForEpicCreation(Epic epic) {
        validateNotNull(epic, "Эпик ");
        validatePositiveId(epic.getId());
    }

    /**
     * Проверяет подзадачу перед созданием
     *
     * @param subTask для проверки
     * @param epics   карта эпиков для проверки существования эпика
     */
    public void validateForSubTaskCreation(SubTask subTask, Map<Integer, Epic> epics) {
        validateNotNull(subTask, "SubTask ");
        validatePositiveId(subTask.getId());
        validateEpicExist(epics, subTask.getEpicId());
    }

    /**
     * Проверяет существование эпика в карте
     *
     * @param epics  карта эпиков для проверки
     * @param epicId эпик для проверки
     */
    public void validateEpicExist(Map<Integer, Epic> epics, int epicId) {
        if (!epics.containsKey(epicId)) {
            throw new IllegalArgumentException("Эпик с id " + epicId + " не существует");
        }
    }

    /**
     * Проверяет задачу перед обновлением
     *
     * @param task  для проверки
     * @param tasks карта задач для проверки существования задачи
     */
    public void validateTaskForUpdate(Task task, Map<Integer, Task> tasks) {
        validateNotNull(task, "Задача ");
        validateIdExist(tasks, task.getId());
    }

    /**
     * Проверяет на существование в карте
     *
     * @param map карта для проверки
     * @param id  id для проверки
     */
    public void validateIdExist(Map<?, ?> map, int id) {
        if (!map.containsKey(id)) {
            throw new IllegalArgumentException("Задача с id " + id + " не существует");
        }
    }

    /**
     * Проверяет подзадачу перед обновлением
     *
     * @param subTask  для проверки
     * @param subTasks карта подзадач для проверки существования подзадачи
     * @param epics    карта эпиков для проверки существования родительского эпика
     */
    public void validateSubTaskForUpdate(SubTask subTask, Map<Integer, SubTask> subTasks, Map<Integer, Epic> epics) {
        validateNotNull(subTask, "SubTask ");
        validatePositiveId(subTask.getId());
        validateIdExist(subTasks, subTask.getId());
        validateEpicExist(epics, subTask.getEpicId());
    }
}