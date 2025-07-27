package app.console;

public class MenuItem {
    private final String title;
    private final Runnable action;

    public MenuItem(String title, Runnable action) {
        this.title = title;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public Runnable getAction() {
        return action;
    }
}