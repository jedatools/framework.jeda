package ru.kwanza.jeda.core.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kwanza.jeda.api.internal.IQueueObserver;

import java.util.ArrayList;

/**
 * @author Guzanov Alexander
 */
public class QueueObserverChain implements IQueueObserver {
    private static final Logger logger = LoggerFactory.getLogger(QueueObserverChain.class);

    private ArrayList<IQueueObserver> observers = new ArrayList<IQueueObserver>();


    public void notifyChange(int queueSize, int delta) {
        for (IQueueObserver qo : observers) {
            try {
                qo.notifyChange(queueSize, delta);
            } catch (Throwable ex) {
                logger.error("Error in ObserverChain!", ex);
            }
        }
    }

    public void addObserver(IQueueObserver observer) {
        observers.add(observer);
    }

    public boolean removeObserver(IQueueObserver observer) {
        return observers.remove(observer);
    }
}
