package GUI;

/**
 * Class for projects.
 */
public class Project {
    private String name;
    private float time;
    private String currentPhase;

    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param time         the time passed
     * @param currentPhase the current phase
     */
    public Project(String name, float time, String currentPhase) {
        this.name = name;
        this.time = time;
        this.currentPhase = currentPhase;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets time.
     *
     * @return the time
     */
    public float getTime() {
        return time;
    }

    /**
     * Sets time.
     *
     * @param time the time
     */
    public void setTime(float time) {
        this.time = time;
    }

    /**
     * Gets current phase.
     *
     * @return the current phase
     */
    public String getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Sets current phase.
     *
     * @param currentPhase the current phase
     */
    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }
}
