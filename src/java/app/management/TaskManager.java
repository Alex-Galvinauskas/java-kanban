package java.app.management;

import java.app.entity.Epic;
import java.app.entity.SubTask;
import java.app.entity.Task;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskManager {

    int createTask(Task task) throws IOException;

    int createEpic(Epic epic) throws IOException;

    int createSubTask(SubTask subTask) throws IOException;

    List<Task> getAllTasks();

    Optional<Task> getTaskById(int id);

    List<Epic> getAllEpics();

    Optional<Epic> getEpicById(int id);

    List<SubTask> getSubTasksByEpicId(int epicId);

    List<SubTask> getEpicSubtasks(int epicId);

    List<SubTask> getAllSubTasks();

    Optional<SubTask> getSubTaskById(int id);

    Collection<Task> getHistory();

    List<Task> getPrioritizedTasks();

    void updateTask(Task task) throws IOException;

    void updateSubTask(SubTask subTask) throws IOException;

    void deleteAllTasks() throws IOException;

    void deleteTaskById(int id) throws IOException;

    void deleteAllEpics() throws IOException;

    void deleteEpicById(int id) throws IOException;

    void deleteAllSubTasks() throws IOException;

    void deleteSubTaskById(int id) throws IOException;

    int generateId();

    boolean isTasksOverlap(Task task1, Task task2);
}
