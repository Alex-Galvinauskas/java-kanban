package service;

/**
 * The StatusCheckResult class is used to store the results of checking subtask statuses
 * in order to determine the overall status of an epic.
 * It tracks whether all subtasks are new, all are done, or if any are in progress.
 */

class StatusCheckResult {
    private boolean allNew = true;
    private boolean allDone = true;
    private boolean hasInProgress = false;

    public boolean isAllNew() { return allNew; }
    public void setAllNew(boolean allNew) { this.allNew = allNew; }
    public boolean isAllDone() { return allDone; }
    public void setAllDone(boolean allDone) { this.allDone = allDone; }
    public boolean isHasInProgress() { return hasInProgress; }
    public void setHasInProgress(boolean hasInProgress) { this.hasInProgress = hasInProgress; }
}