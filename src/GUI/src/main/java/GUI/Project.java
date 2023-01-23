package GUI;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

import java.sql.Time;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Class for projects.
 */
public class Project {
    private String name;
    private SimpleLongProperty time;
    private SimpleStringProperty currentPhase;

    /**
     * Instantiates a new Project.
     *
     * @param name         the name
     * @param time         the time passed in milis // todo hlupost
     * @param currentPhase the current phase
     */
    public Project(String name, long time, String currentPhase) {
        this.name = name;
        this.time = new SimpleLongProperty(TimeUnit.MILLISECONDS.toMinutes(time));
        this.currentPhase = new SimpleStringProperty(currentPhase);
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
    public SimpleLongProperty timeProperty() {
        return time;
    }

    /**
     * Sets time.
     *
     * @param time the time
     */
    public void setTimeProperty(SimpleLongProperty time) {
        this.time = time;
    }

    /**
     * Gets current phase.
     *
     * @return the current phase
     */
    public SimpleStringProperty currentPhaseProperty() {
        return currentPhase;
    }

    /**
     * Sets current phase.
     *
     * @param currentPhase the current phase
     */
    public void setCurrentPhaseProperty(SimpleStringProperty currentPhase) {
        this.currentPhase = currentPhase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(name, project.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
