package managers;

import core.Task;

import java.util.List;

public interface HistoryManagerInterface {

    List<Task> getHistory();

    void add(Task task);
}
