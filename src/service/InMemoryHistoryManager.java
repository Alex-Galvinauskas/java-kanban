package service;

import core.Task;
import contracts.HistoryManager;

import java.util.*;

/**
 * HistoryManager хранит историю просмотров в оперативной памяти.
 * Использует двусвязный список для порядка и хэш-таблицу для быстрого доступа
 */

public class InMemoryHistoryManager implements HistoryManager {

    /**
     * Node класс, представляющий узел двусвязного списка.
     */
    private static class Node {
        final Task task;
        Node next;
        Node prev;

        /**
         * Создает новый узел
         *
         * @param task задача для хранения
         * @param prev предыдущий узел
         * @param next следующий узел
         */
        Node(Task task, Node prev, Node next) {
            this.task = task;
            this.prev = prev;
            this.next = next;
        }
    }

    /**
     * Хеш-таблица для быстрого доступа к узлам
     */
    private final Map<Integer, Node> historyMap = new HashMap<>();
    /**
     * Первый узел
     */
    private Node head;
    /**
     * Последний узел
     */
    private Node tail;

    /**
     * @return список задач от самого старого к самому новому
     */
    @Override
    public List<Task> getHistory() {
        List<Task> history = new ArrayList<>();
        Node current = head;
        while (current != null) {
            history.add(current.task);
            current = current.next;
        }
        return history;
    }

    /**
     * Добавляет задачу в историю.
     * Если задача есть в истории, то перемещает ее в конец.
     *
     * @param task задача для добавления (не может быть null)
     */
    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }
        int id = task.getId();
        remove(id);

        Node newNode = new Node(task, tail, null);
        linkLast(newNode);
        historyMap.put(id, newNode);
    }

    /**
     * Удаляет задачу из истории
     *
     * @param id идентификатор для удаления
     */
    @Override
    public void remove(int id) {
        Node node = historyMap.get(id);
        if (node != null) {
            removeNode(node);
            historyMap.remove(id);
        }
    }

    /**
     * Удаляет узел из истории
     *
     * @param node узел для удаления (не может быть null)
     */
    private void removeNode(Node node) {

        if (node == null) {
            return;
        }

        updatePrevNodeLinks(node);
        updateNextNodeLinks(node);
    }

    /**
     * Обновляет ссылки на предыдущий и следующий узлы
     *
     * @param node узел для обновления (не может быть null)
     */
    private void updatePrevNodeLinks(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
    }

    /**
     * Обновляет ссылки на предыдущий и следующий узлы
     *
     * @param node узел для обновления (не может быть null)
     */
    private void updateNextNodeLinks(Node node) {
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }

    /**
     * Добавляет узел в конец списка
     *
     * @param newNode узел для добавления (не может быть null)
     */
    private void linkLast(Node newNode) {

        if (newNode == null) {
            return;
        }

        if (head == null) {
            head = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
        }
        tail = newNode;
    }

    /**
     * Очищает историю
     */
    @Override
    public void clear() {
        historyMap.clear();
        head = null;
        tail = null;
    }
}

