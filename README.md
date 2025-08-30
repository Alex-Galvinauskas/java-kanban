# Java Kanban Task Manager

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Version](https://img.shields.io/badge/version-1.1.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

**Автор:** [Alex Galvinauskas](https://github.com/Alex-Galvinauskas)  
**Лицензия:** [MIT]

## 🚀 Описание

Task Manager - это система управления задачами, реализованная на java, с поддержкой:

- Обычных задач (`Task`)
- Эпиков (`Epic`), которые могут содержать подзадачи
- Подзадач (`SubTask`), связанных с эпиками
- Хранение истории просмотров задач
- Автоматическое обновление статуса эпиков на основе статусов подзадач
- Валидацию данных перед созданием и обновлением

## 🔍 Возможности

- **Управление задачами**:
    - Создание, обновление и удаление задач всех типов
    - Хранение истории просмотров задач в виде двусвязного списка
    - Быстрое удаление задач из истории
    - Очистка всей истории просмотров
    - Проверка дубликатов при создании и обновлении задач
    - Обработка ошибок при создании и обновлении задач

- **Получение информации**:
    - Списки всех задач/эпиков/подзадач
    - Поиск по ID
    - Все подзадачи конкретного эпика

## 🛠 Установка

1 Клонируйте репозиторий:

```  
git clone https://github.com/Alex-Galvinauskas/java-kanban.git  
```

2 Перейдите в директорию проекта:

```
cd java-kanban  
```

3 Убедитесь, что у вас установлен JDK 17 и выше.  
4 Используйте Maven для сборки проекта:

```
mvn clean install  
```  

## Зависимости

- Java 17 и выше
- JUnit 5.8.1 и выше
- Lombok 1.18.24 и выше
- Maven 3.8.6 и выше

## 🏗 Структура проекта

```
├── taskmanager/
│      └──app/
│          ├── main.java           # Точка входа в приложение
│          ├── cli/
│          │      ├── MenuItem/        # record классы для меню
│          │      └── TaskManagerCLI.java   # Консольный интерфейс
│          ├── entity/
│          │     ├── Task.java           # Базовая задача
│          │     ├── Epic.java           # Эпик (содержит подзадачи)
│          │     ├── SubTask.java        # Подзадача
│          │     ├── StatusTask.java     # Перечисление статусов задачи
│          │     └── TaskType.java       # Перечисление типов задач
│          ├── exception/
│          │     ├── ManagerSaveExceptio.java  # Исключение при сохранении задач
│          │     └── ValidationException.java        # Валидация данных перед операциями создания/обновления
│          ├── management/
│          │     ├── HystoryManager.java        # Интерфейс для управления историей
│          │     ├── Managers.java              # Класс для создания менеджеров
│          │     └── TaskManager.java           # Интерфейс для управления задачами
│          ├── service/
│          │     ├──manager/
│          │     │     ├── FileBackedTaskManager.java   # Реализация TaskManager для управления задачами в файловой системе
│          │     │     └── InMemoryHistoryManager.java  # Реализация HistoryManager для хранения истории просмотров в памяти
│          │     ├──history/
│          │           └── InMemoryTaskManager.java     # Реализация TaskManager для управления задачами в памяти
           └── utils/
│                └── StatusCheckResult.java 
test/
├── taskmanager/
│     └──app/
│          ├── cli/         # Тесты для консольного интерфейса
│          ├── exception/       # Тесты для валидации данных
│          ├── management/        # Тесты для менеджеров
│          └── service/         # Тесты для HistoryManager и TaskManager
 ```

## 📦 Пакет `java.app.entity`

### Класс `Task` (базовая задача)

```java
/**
 * Базовый класс задачи.
 * Реализует паттерн защитного копирования
 */
public class Task {
    private int id;
    private String name;
    private String description;
    private StatusTask status;

    // Конструкторы
    public Task(int id, String name, String description, StatusTask status) {
    }

    public Task(Task other) {
    }  // Конструктор копирования

    // Геттеры
    public int getId() {
    }

    public String getName() {
    }

    // Сеттеры
    public void setId(int id) {
    }

    public void setStatus(StatusTask status) {
    }

    // Переопределенные методы
    @Override
    public boolean equals(Object obj) {
    }  // Сравнение по ID

    @Override
    public String toString() {
    }          // Строковое представление
}  
```  

### Особенности:

- *Использует StatusTask (NEW/IN_PROGRESS/DONE)*

- *Проверка равенства только по id*

- *Использует паттерн защитного копирования*

- *Поддержка проверки дубликатов при создании и обновлении задач*

- *Поддержка обработки исключений*

### Класс Epic (Наследник Task)

```java
public class Epic extends Task {
    private List<Integer> subtaskIds;  // ID связанных подзадач

    // Автоматически обновляет статус на основе подзадач
    public void updateStatus() {
    }
}  
```  

### Класс SubTask (наследник Task)

```java  
public class SubTask extends Task {
    private int epicId;  // ID родительского эпика
}  
```

## 📦 Пакет `Service`

### Класс FileBackedTaskManager

## 🛠 Использование

Реализация менеджера задач с хранением данных в файле:

Обеспечивает постоянное сохранение данных в файл между сессиями приложения.

```java
public class FileBackedTaskManager implements TaskManager {
    private final File file;
    private final HistoryManager historyManager;

    // Методы для создания, обновления, удаления задач...(реализованы через хуки)
}
```  

### Особенности:

- *Автоматически сохраняет изменения в CSV файле.*
- *Восстанавливает данные из файла при создании через loadFromFile().*
- *Использует структурированный CSV с заголовком и экранированием полей.*
- *Полностью наследует API InMemoryTaskManager, добавляя к нему персистентность.*

```java
// Создание менеджера и загрузка данных из файла
FileBackedTaskManager manager = FileBackedTaskManager.loadFromFile(Path.of("tasks.csv"));

// Все методы родительского класса доступны
manager.

createTask(new Task(...));
Task task = manager.getTaskById(1);

// Все изменения автоматически синхронизируются с файлом
```

Рекомендуется использовать эту реализацию, если необходимо сохранять состояние программы после завершения.

### Класс `InMemoryHystoryManager`

## 🛠 Использование

Реализует интерфейс HistoryManager с использованием двусвязного списка:

```java
public class InMemoryHistoryManager implements HistoryManager {
    private final Map<Integer, Node> historyMap = new HashMap<>();
    private Node head;
    private Node tail;

    // Методы для добавления, удаления, получения истории просмотров...
}
```  

### Особенности:

- *Использует внутренний класс Node для хранения узлов списка.*
- *Поддерживает добавление задачи в историю с проверкой на дубликат.*
- *Если задача уже существует в истории, она перемещается в конец.*
- *Поддерживает удаление задачи по ID.*
- *История хранится от самого старого к самому новому.*

#### Пример создания задачи:

```java  
Task task = new Task(manager.generatedId(), "Рефакторинг", "Обновить документацию", StatusTask.NEW);
manager.

createTask(task);
```  

#### Пример работы с эпиком:

```java  


Epic epic = new Epic("Разработка", "Новый функционал");
manager.

createEpic(epic);

SubTask subTask = new SubTask("Дизайн", "Создать макеты", epic.getId());
manager.

createSubTask(subTask);
```  

#### Работа с историей просмотров:

```java  
HistoryManager historyManager = new InMemoryHistoryManager();
historyManager.

add(task);
historyManager.

add(epic);

List<Task> history = historyManager.getHistory();
```

## 🧪 Тестирование

#### Проект содержит тесты для всех компонентов:

```
- ManagerTest  
```  

— проверяет работу методов по созданию менеджеров.

```
InMemoryHistoryManagerTest   
```

— тестирование истории просмотров задач.

```
InMemoryTaskManagerTest  
```  

— тестирование логики создания, удаления, обновления и получения задач.

```
TaskValidatorTest  
```  

— тестирование валидации данных

Тесты написаны с использованием JUnit 5.8.1 и обеспечивают покрытие основных функций.

## 📚 Как работает обновление статуса эпика

Класс эпик автоматически обновляет свой статус на основе статусов его подзадач.

1. Проверка на null
2. Проверка на пустой список подзадач
3. Проверка на совпадение статусов подзадач
4. Проверка положительного и отрицательного сценария

## ✅ Общие принципы работы

- Паттерн защитного копирования: при получении или добавлении задач возвращаются копии объектов.
- Работа с историей: реализовано через двусвязный список для более эффективного управления просмотренными задачами.
- Валидация данных: реализована через паттерн Builder для более удобной и безопасной работы с данными.
- Тестирование: реализовано через паттерн Factory для более удобной и безопасной работы с данными.
- Соблюдение принципов SOLID: каждая часть кода отвечает за свою функцию.

## 🧩 Возможные улучшения

1. Добавить поддержку времени выполнения задач (start, end)
2. Добавить возможность сохранения данных в файл или БД.
3. Добавить фильтрацию по статусу.

## 📌 Лицензия

Проект лицензирован под MIT License.

📞 Контакты  
Если у вас есть вопросы или предложения, вы можете связаться с автором через:  
Github: https://github.com/Alex-Galvinauskas
Telegram: https://t.me/Alex_Galvinauskas