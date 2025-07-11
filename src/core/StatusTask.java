package core;

public enum StatusTask {
    NEW("Новое"),
    IN_PROGRESS("В процессе"),
    DONE("Сделано");

    String nameStatus;
    StatusTask(String nameStatus) {
        this.nameStatus = nameStatus;
    }

}
