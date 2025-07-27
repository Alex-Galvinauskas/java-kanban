package service;

import core.Task;
import managers.HistoryManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    public static final int MAX_SIZE = 10;
    private final List<Task> history = new LinkedList<>();
    private final Map<Integer, Task> historyMap = new HashMap<>();


    @Override
    public List<Task> getHistory() {
        return new LinkedList<>(history);
    }

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }
        removeIfExists(task.getId());
        addToHistory(task);
        removeOldest();
    }

    private void addToHistory(Task task) {
        if (task == null) return;
        removeIfExists(task.getId());
        historyMap.put(task.getId(), task);
        history.add(task);
        removeOldest();
    }

    public void removeIfExists(int id) {
        if (historyMap.containsKey(id)) {
            Task task = historyMap.remove(id);
            if (task != null) {
                history.remove(task);
            }
        }
    }

    private void removeOldest() {
        if (history.size() > MAX_SIZE) {
            Task oldestTask = history.removeFirst();
            historyMap.remove(oldestTask.getId());
        }
    }
}

