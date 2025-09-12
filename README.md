# Java Kanban Task Manager

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Version](https://img.shields.io/badge/version-1.4.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

**Автор:** [Alex Galvinauskas](https://github.com/Alex-Galvinauskas)  
**Лицензия:** [MIT]

---

## 🚀 Описание

**Task Manager** — это система управления задачами, реализованная на Java, поддерживающая следующие функции:

- Работу с обычными задачами (`Task`)
- Эпиками (`Epic`), которые могут содержать подзадачи
- Подзадачами (`SubTask`), связанными с эпиками
- Хранение истории просмотров задач
- Автоматическое обновление статуса эпиков на основе статусов подзадач
- Валидацию данных перед созданием и обновлением
- Управление временем выполнения задач (начало, окончание, продолжительность)

---

## 🔍 Возможности

### Управление задачами:
- Создание, обновление и удаление задач всех типов
- Хранение истории просмотров задач в виде двусвязного списка
- Быстрое удаление задач из истории
- Очистка всей истории просмотров
- Проверка дубликатов при создании и обновлении задач
- Обработка ошибок при создании и обновлении задач

### Получение информации:
- Списки всех задач/эпиков/подзадач
- Поиск по ID
- Получение всех подзадач конкретного эпика
- Получение задач в порядке приоритета (по времени начала)

---

## 🛠 Установка

1. Клонируйте репозиторий:

```bash
    git clone https://github.com/Alex-Galvinauskas/java-kanban.git
```

2. Перейдите в директорию проекта:

```bash
    cd java-kanban
```

3. Убедитесь, что у вас установлен JDK 17 и выше.

4. Используйте Maven для сборки проекта:

```bash
    mvn clean install
```

---

## 📦 Зависимости

- **Java** 17+
- **JUnit** 5.8.1+
- **Lombok** 1.18.24+
- **Maven** 3.8.6+

---

## 🏗 Структура проекта

```
├── taskmanager/
│   └── app/
│       ├── Main.java                     # Точка входа в приложение
│       ├── cli/
│       │   ├── MenuItem/               # record классы для меню
│       │   └── TaskManagerCLI.java     # Консольный интерфейс
│       ├── entity/
│       │   ├── Task.java               # Базовая задача
│       │   ├── Epic.java               # Эпик (содержит подзадачи)
│       │   ├── SubTask.java            # Подзадача
│       │   ├── StatusTask.java         # Перечисление статусов задачи
│       │   └── TaskType.java           # Перечисление типов задач
│       ├── exception/
│       │   ├── ManagerSaveException.java  # Исключение при сохранении задач
│       │   └── ValidationException.java   # Валидация данных
│       ├── management/
│       │   ├── HistoryManager.java      # Интерфейс для управления историей
│       │   ├── Managers.java            # Класс для создания менеджеров
│       │   └── TaskManager.java         # Интерфейс для управления задачами
│       ├── service/
│       │   ├── manager/
│       │   │   ├── FileBackedTaskManager.java  # Реализация TaskManager с сохранением в файл
│       │   │   └── InMemoryHistoryManager.java # Реализация HistoryManager в памяти
│       │   ├── history/
│       │       └── InMemoryTaskManager.java    # Реализация TaskManager в памяти
│       ├── time/
│       │   ├── TimeManager.java              # Интерфейс для управления временем задач
│       │   └── TimeManagerService.java      # Реализация TimeManager
│       └── utils/
│           └── StatusCheckResult.java          # Вспомогательные функции проверки статуса
└── test/
    └── taskmanager/
        └── app/
            ├── cli/              # Тесты для консольного интерфейса
            ├── exception/        # Тесты для валидации данных
            ├── management/       # Тесты для менеджеров
            └── service/          # Тесты для HistoryManager и TaskManager
```

---

## 📦 Пакет `taskmanager.app.entity`

### Класс `Task` (базовая задача)

```java
/**
 * Базовый класс задачи.
 * Реализует паттерн защитного копирования.
 */
public class Task {
    private int id;
    private String name;
    private String description;
    private StatusTask status;
    private LocalDateTime startTime;
    private Duration duration;

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public StatusTask getStatus() { return status; }
    public void setStatus(StatusTask status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }

    // Конструкторы
    public Task(int id, String name, String description, StatusTask status, Duration duration, LocalDateTime startTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.duration = duration;
        this.startTime = startTime;
    }

    // Конструктор копирования
    public Task(Task other) {
        this.id = other.id;
        this.name = other.name;
        this.description = other.description;
        this.status = other.status;
        this.duration = other.duration;
        this.startTime = other.startTime;
    }

    // Переопределенные методы
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Task)) return false;
        Task task = (Task) obj;
        return id == task.id;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", duration=" + duration +
                '}';
    }
}
```

### Особенности:

- Использует `StatusTask` (NEW / IN_PROGRESS / DONE)
- Проверка равенства только по ID
- Использует паттерн защитного копирования
- Поддерживает проверку дубликатов при создании и обновлении
- Поддерживает обработку исключений
- Поддерживает управление временем выполнения задач (начало, окончание, продолжительность)

---

### Класс `Epic` (наследник `Task`)

```java
public class Epic extends Task {
    private List<Integer> subtaskIds;  // ID связанных подзадач
    private LocalDateTime endTime;

    // Геттеры и сеттеры
    public List<Integer> getSubTaskIds() { return new ArrayList<>(subtaskIds); }
    public void addSubTaskId(int id) { subtaskIds.add(id); }
    public void removeSubTaskId(int id) { subtaskIds.remove(Integer.valueOf(id)); }
    public void clearSubTaskIds() { subtaskIds.clear(); }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    // Конструктор
    public Epic(int id, String name, String description) {
        super(id, name, description, StatusTask.NEW, null, null);
        this.subtaskIds = new ArrayList<>();
    }

    // Методы
    public void updateStatus() {
        // Обновление статуса эпика на основе подзадач
    }
}
```

---

### Класс `SubTask` (наследник `Task`)

```java
public class SubTask extends Task {
    private int epicId;  // ID родительского эпика

    // Геттеры и сеттеры
    public int getEpicId() { return epicId; }
    public void setEpicId(int epicId) { this.epicId = epicId; }

    // Конструктор
    public SubTask(int id, String name, String description, StatusTask status, Duration duration, LocalDateTime startTime, int epicId) {
        super(id, name, description, status, duration, startTime);
        this.epicId = epicId;
    }

    // Конструктор копирования
    public SubTask(SubTask other) {
        super(other);
        this.epicId = other.epicId;
    }
}
```

---

## 📦 Пакет `taskmanager.app.service.manager`

### Класс `InMemoryTaskManager`

Реализация менеджера задач в оперативной памяти.

```java
public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, SubTask> subTasks = new HashMap<>();
    private final InMemoryHistoryManager historyManager = new InMemoryHistoryManager();
    private final AtomicInteger nextId = new AtomicInteger(1);

    // Методы для управления задачами, эпиками и подзадачами
    // ...
}
```

#### Особенности:
- Хранение задач, эпиков и подзадач в картах
- Поддержка истории просмотров через `InMemoryHistoryManager`
- Генерация уникальных ID
- Валидация данных перед созданием и обновлением
- Поддержка управления временем выполнения задач
- Обновление статуса эпиков на основе статусов подзадач

---

### Класс `FileBackedTaskManager`

Реализация менеджера задач с хранением данных в файле. Обеспечивает постоянное сохранение данных между сессиями.

```java
public class FileBackedTaskManager extends InMemoryTaskManager {
    private final Path filePath;
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private boolean isLoading = false;

    // Методы для работы с файлом
    // ...
}
```

#### Особенности:
- Автоматически сохраняет изменения в CSV-файл
- Восстанавливает данные из файла при создании через `loadFromFile()`
- Использует структурированный CSV с заголовком и экранированием полей
- Полностью наследует API `InMemoryTaskManager`, добавляя персистентность

#### Пример использования:

```java
// Создание менеджера и загрузка данных из файла
FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(Path.of("tasks.csv"));

// Все методы родительского класса доступны
manager.createTask(new Task(...));
Task task = manager.getTaskById(1);

// Все изменения автоматически синхронизируются с файлом
```

---

## 📦 Пакет `taskmanager.app.service.time`

### Класс `TimeManagerService`

```java
/**
 * Сервис для управления временными интервалами задач.
 * Реализует механизм проверки пересечений задач по времени с использованием временных слотов.
 * Каждый временной слот представляет собой интервал длиной 15 минут.
 */
public class TimeManagerService implements TimeManager {
    private final Map<LocalDateTime, Integer> timeSlots = new HashMap<>();
    private static final Duration TIME_SLOT_SIZE = Duration.ofMinutes(15);

    /**
     * Проверяет, не пересекается ли задача по времени с уже существующими задачами.
     * Если пересечение обнаружено, выбрасывает исключение RuntimeException.
     *
     * @param task задача для проверки временного пересечения
     * @throws RuntimeException если задача пересекается по времени с существующей задачей
     */
    public void validateTaskTime(Task task) {
        if (task.getStartTime() == null || task.getDuration() == null) {
            return;
        }

        if (!isTimeSlotAvailable(task.getStartTime(), task.getEndTime())) {
            throw new RuntimeException("Задача '" + task.getName() +
                    "' пересекается по времени с существующей задачей");
        }
    }
     ...
```

#### Особенности:
- Управляет временными слотами задач
- Проверяет пересечение времени задач
- Добавляет и удаляет временные слоты при изменении задач
- Обеспечивает корректную работу планирования задач

---

## 📦 Пакет `taskmanager.app.service.history`

### Класс `InMemoryHistoryManager`

Реализует интерфейс `HistoryManager` с использованием двусвязного списка.

```java
public class InMemoryHistoryManager implements HistoryManager {
    private final Map<Integer, Node> historyMap = new HashMap<>();
    private Node head;
    private Node tail;

    // Методы для добавления, удаления, получения истории просмотров...
}
```

#### Особенности:
- Использует внутренний класс `Node` для хранения узлов списка
- Поддерживает добавление задачи в историю с проверкой на дубликат
- Если задача уже существует, она перемещается в конец
- Поддерживает удаление задачи по ID
- История хранится от самого старого к самому новому

#### Пример работы с историей:

```java
HistoryManager historyManager = new InMemoryHistoryManager();
historyManager.add(task);
historyManager.add(epic);

List<Task> history = historyManager.getHistory();
```

---

## 🧪 Тестирование

Проект содержит тесты для всех компонентов:

- `InMemoryTaskManagerTest` — тестирование логики создания, удаления, обновления и получения задач
- `ManagersTest` — проверяет работу методов по созданию менеджеров
- `InMemoryHistoryManagerTest` — тестирование истории просмотров задач
- `TaskValidatorTest` — тестирование валидации данных
- `TimeManagerTest` — тестирование логики управления временем

Тесты написаны с использованием **JUnit 5.8.1**, обеспечивают покрытие основных функций и 
помогают поддерживать стабильность кода.

---

## 📚 Как работает обновление статуса эпика

Класс `Epic` автоматически обновляет свой статус на основе статусов его подзадач:

1. Проверка на null
2. Проверка на пустой список подзадач
3. Проверка на совпадение статусов подзадач
4. Проверка положительного и отрицательного сценария

---

## ✅ Общие принципы работы

- **Паттерн защитного копирования**: при получении или добавлении задач возвращаются копии объектов
- **Работа с историей**: реализована через двусвязный список для эффективного управления просмотренными задачами
- **Валидация данных**: реализована через паттерн Builder для удобной и безопасной работы
- **Тестирование**: через паттерн Factory
- **SOLID**: каждая часть кода отвечает за свою функцию

---

## 🧩 Возможные улучшения

1. Добавить поддержку фильтрации задач по статусу
2. Добавить возможность сохранения данных в БД
3. Добавить поддержку планирования задач на определённое время
4. Переделать временные слоты на использование интервального красно-черного дерева для оптимизации

---

## 📌 Лицензия

Проект лицензирован под [MIT License].

📞 **Контакты**  
Если у вас есть вопросы или предложения, вы можете связаться с автором через:
- **Github:** [Alex Galvinauskas](https://github.com/Alex-Galvinauskas)
- **Telegram:** [https://t.me/Alex_Galvinauskas](https://t.me/Alex_Galvinauskas)