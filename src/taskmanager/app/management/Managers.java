package taskmanager.app.management;

import taskmanager.app.service.history.InMemoryHistoryManager;
import taskmanager.app.service.manager.FileBackedTasksManager;


public class Managers {
    public static TaskManager getDefault() {
        return new FileBackedTasksManager("tasks.csv");
    }

    public static InMemoryHistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
