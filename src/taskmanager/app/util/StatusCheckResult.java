package taskmanager.app.util;

public class StatusCheckResult {
    private boolean allNew = true;
    private boolean allDone = true;
    private boolean hasInProgress = false;

    // Геттеры и сеттеры
    public boolean isAllNew() {
        return allNew;
    }

    public void setAllNew(boolean allNew) {
        this.allNew = allNew;
    }

    public boolean isAllDone() {
        return allDone;
    }

    public void setAllDone(boolean allDone) {
        this.allDone = allDone;
    }

    public boolean isHasInProgress() {
        return hasInProgress;
    }

    public void setHasInProgress(boolean hasInProgress) {
        this.hasInProgress = hasInProgress;
    }
}