package taskmanager.app.management;

import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.service.manager.FileBackedTasksManager;

import java.nio.file.Path;


public class Managers {
    public static TaskManager getDefault() {
        return new FileBackedTasksManager(Path.of("tasks.csv"));
    }

    public static InMemoryHistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
