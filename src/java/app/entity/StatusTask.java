package java.app.entity;

public enum StatusTask {
    NEW("Новое"),
    IN_PROGRESS("В процессе"),
    DONE("Сделано");

    final String nameStatus;

    StatusTask(String nameStatus) {
        this.nameStatus = nameStatus;
    }
}
