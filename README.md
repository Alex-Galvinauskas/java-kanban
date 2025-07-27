
# Java Kanban Task Manager

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Version](https://img.shields.io/badge/version-1.0.3-blue)
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

##  🛠 Установка
1 Склонируйте репозиторий:  
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
src/  
├── main/  
│ ├── java/  
│ │ └── core/  
│ │ ├── Task.java         # Базовая задача  
│ │ ├── Epic.java         # Эпик (содержит подзадачи)  
│ │ ├── SubTask.java      # Подзадача  
│ │ └── StatusTask.java   # Перечисление статусов  
│ ├── exceptions/         # Обработка ошибок  
│ └── service/            # Логика TaskManager  
├── test/  
│ ├── managers/           # Тесты для менеджеров  
│ └── service/            # Тесты для сервисов  
 ```

## 📦 Пакет `core`

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

*Использует StatusTask (NEW/IN_PROGRESS/DONE)*

*Проверка равенства только по id*

*Поддержка обработки исключений*

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
Task task = new Task(manager.generatedId(), "Рефакторинг", "Обновить документацию", StatusTask.NEW);  
```  
  
#### Пример работы с эпиком:

```java  


Epic epic = new Epic("Разработка", "Новый функционал");
SubTask subTask = new SubTask("Дизайн", "Создать макеты", epic.getId());  
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

Тесты написаны с использованием JUnit 5.8.1 и обеспечивают покрытие основных функций.
