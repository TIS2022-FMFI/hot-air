package GUI;

public class Project {
    private String name;
    private float time;
    private String currentPhase;

    public Project(String name, float time, String currentPhase) {
        this.name = name;
        this.time = time;
        this.currentPhase = currentPhase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }
}
