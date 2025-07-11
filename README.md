
# Java Kanban Task Manager

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Version](https://img.shields.io/badge/version-1.0.1-blue)
![License](https://img.shields.io/badge/license-MIT-green)

**Автор:** [Alex Galvinauskas](https://github.com/Alex-Galvinauskas)  
**Лицензия:** [MIT](LICENSE)

## 🚀 Описание

Task Manager - это система управления задачами с поддержкой:
- Обычных задач (`Task`)
- Эпиков (`Epic`), которые могут содержать подзадачи
- Подзадач (`SubTask`), связанных с эпиками

Статус эпика автоматически рассчитывается на основе статусов его подзадач.

## 🔍 Возможности

- **Управление задачами**:
    - Создание/обновление/удаление задач всех типов
    - Автоматическое обновление статуса эпика
    - Валидация данных при изменениях

- **Получение информации**:
    - Списки всех задач/эпиков/подзадач
    - Поиск по ID
    - Все подзадачи конкретного эпика

## 🏗 Структура проекта  

src/  
├── main/  
│ ├── java/  
│ │ └── core/  
│ │ ├── Task.java # Базовая задача  
│ │ ├── Epic.java # Эпик (содержит подзадачи)  
│ │ ├── SubTask.java # Подзадача  
│ │ └── StatusTask.java # Перечисление статусов  
│ ├── exceptions/ # Обработка ошибок  
│ └── service/ # Логика TaskManager  

## 📦 Пакет `core`

### Класс `Task` (базовая задача)

```java
/**
 * Базовый класс задачи
 * Реализует паттерн защитного копирования
 */
public class Task {
    private int id;
    private String name;
    private String description;
    private StatusTask status;
    
    // Конструкторы
    public Task(int id, String name, String description, StatusTask status) {  }
    public Task(Task other) {  }  // Конструктор копирования
    
    // Геттеры
    public int getId() {  }
    public String getName() {  }
    
    // Сеттеры
    public void setId(int id) {  }
    public void setStatus(StatusTask status) { }
    
    // Переопределенные методы
    @Override
    public boolean equals(Object obj) {  }  // Сравнение по ID
    @Override
    public String toString() {  }          // Строковое представление
}  
```  
### Особенности:

Использует StatusTask (NEW/IN_PROGRESS/DONE)

Проверка равенства только по id

Поддержка обработки исключений

### Класс Epic (Наследник Task)  
```java
public class Epic extends Task {
private List<Integer> subtaskIds;  // ID связанных подзадач

    // Автоматически обновляет статус на основе подзадач
    public void updateStatus() {  }
}  
```  

### Класс SubTask (наследник Task)  
  
```java  
public class SubTask extends Task {
    private int epicId;  // ID родительского эпика
}  
```  

## 🛠 Использование  

#### Пример создания задачи:  
```java  
Task task = new Task(1, "Рефакторинг", "Обновить документацию", StatusTask.NEW);  
```  
  
#### Пример работы с эпиком:  
```java  
Epic epic = new Epic(2, "Разработка", "Новый функционал");
SubTask subTask = new SubTask(3, "Дизайн", "Создать макеты", epic.getId());  
```