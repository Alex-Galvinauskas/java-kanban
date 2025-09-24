package java.app.management;

import java.app.service.history.InMemoryHistoryManager;
import java.app.service.manager.FileBackedTasksManager;
import java.nio.file.Path;


public class Managers {
    public static TaskManager getDefault() {
        return new FileBackedTasksManager(Path.of("tasks.csv"));
    }

    public static InMemoryHistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
