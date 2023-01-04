package Burniee.Project;

import Burniee.Communication.ControllerHandler;
import Burniee.Server;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Queue;

/**
 * A thread that controls controller during project
 */
public class ActiveController extends Thread {
    private final ControllerHandler handler;
    private final Queue<AbstractMap.SimpleEntry<Integer, Long>> jobQueue;
    private final Project project;
    private static final byte AIR_FLOW = 100;
    private boolean endOfPhaseConfirmed, waitingForConfirmation;
    private static final long END_OF_PHASE_CONFIRMATION_TIMEOUT_IN_MILLIS = 5000;
    private int phaseNumber;

    public ActiveController(ControllerHandler handler, Queue<AbstractMap.SimpleEntry<Integer, Long>> queue, Project project) {
        this.handler = handler;
        this.jobQueue = queue;
        this.project = project;
        handler.getController().setProjectName(project.getID());
        phaseNumber = 0;
    }

    public void confirmEndOfPhase() {
        endOfPhaseConfirmed = true;
        if (waitingForConfirmation) {
            this.interrupt();
        }
    }

    public int getPhaseNumber() {return phaseNumber;}

    public void end() {
        handler.getController().setProjectName(null);
        project.end();
    }

    @Override
    public void run() {
        try {
            long startTime, jobTime;
            int temperature;
            for (AbstractMap.SimpleEntry<Integer, Long> i : jobQueue) {
                phaseNumber++;
                if (!handler.isActive()) {break;}
                endOfPhaseConfirmed = false;
                waitingForConfirmation = false;
                temperature = i.getKey();
                jobTime = i.getValue();
                startTime = System.nanoTime();
                handler.changeControllerParameters(temperature, AIR_FLOW, jobTime);
                try {
                    sleep(jobTime*1000);
                } catch (InterruptedException e) {
                    end();
                    return; // TODO test if this can only happen with big red button and make a note in logs that this happened
//                    e.printStackTrace();
//                    while (System.nanoTime() - startTime < jobTime) {
//                        try {
//                            sleep(jobTime - (System.nanoTime() - startTime));
//                        } catch (InterruptedException ex) {
//                            ex.printStackTrace();
//                        }
//                    }
                }
                endOfPhaseConfirmed = true; //TODO -> remove -> it is here for testing purposes
                if (!endOfPhaseConfirmed) {
                    waitingForConfirmation = true;
                    try {
                        sleep(END_OF_PHASE_CONFIRMATION_TIMEOUT_IN_MILLIS);
                        if (!endOfPhaseConfirmed) {
                            System.err.println("Phase end confirmation not received");
                            handler.bigRedButton();
                            end();
                            throw new ProjectException("Server did not receive end of phase confirmation");
                        }
                    } catch (InterruptedException ignored) {}
                }
            }
        } catch (Exception e) {
            Server.getInstance().sendExceptionToAllActiveGUIs(e);
        } finally {
            end();
        }
    }
}
