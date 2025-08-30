package taskmanager.app.cli;

public record MenuItem(String title, Runnable action) {
}