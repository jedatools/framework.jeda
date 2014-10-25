package ru.kwanza.jeda.mock;

import ru.kwanza.jeda.api.*;
import ru.kwanza.jeda.api.internal.IStageInternal;
import ru.kwanza.jeda.api.IJedaManager;
import ru.kwanza.jeda.api.internal.ITransactionManagerInternal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Guzanov Alexander
 */
public class MockJedaManager implements IJedaManager {
    public static MockJedaManager instance;
    public static final String MOCK_CURRENT_STAGE = "MOCK_CURRENT_STAGE";

    public ConcurrentMap<String, IContextController> contextControllers =
            new ConcurrentHashMap<String, IContextController>();
    public ConcurrentMap<String, MockFlowBus> buses = new ConcurrentHashMap<String, MockFlowBus>();
    public ConcurrentMap<String, MockStage> stages = new ConcurrentHashMap<String, MockStage>();
    public ConcurrentMap<String, ITimer> timers = new ConcurrentHashMap<String, ITimer>();

    public static synchronized MockJedaManager getInstance() {
        if (instance == null) {
            instance = new MockJedaManager();
        }

        return instance;
    }


    public synchronized void clearAll() {
        contextControllers.clear();
        buses.clear();
        stages.clear();
        timers.clear();
    }


    public ITransactionManagerInternal getTransactionManager() {
        return null;
    }

    public MockStage getStage(String name) {
        MockStage result = stages.get(name);
        if (result == null) {
            result = new MockStage(name);
            if (stages.putIfAbsent(name, result) != result) {
                result = stages.get(name);
            }
        }
        return result;
    }

    public MockStage getStageInternal(String name) {
        return getStage(name);
    }

    public ITimer getTimer(String name) {
        return null;
    }

    public IPendingStore getPendingStore() {
        throw new UnsupportedOperationException("getPendingStore");
    }

    public MockContextController getContextController(String name) {
//          MockContext result = buses.load(name);
//        if (result == null) {
//            result = new MockContext(name);
//            if (contexts.putIfAbsent(name, result) != result) {
//                result = contexts.load(name);
//            }
//        }
        return null;
    }

    public MockFlowBus getFlowBus(String name) {
        MockFlowBus result = buses.get(name);
        if (result == null) {
            result = new MockFlowBus(name);
            if (buses.putIfAbsent(name, result) != result) {
                result = buses.get(name);
            }
        }
        return result;
    }

    public IStageInternal getCurrentStage() {
        return (IStageInternal) getStage(MOCK_CURRENT_STAGE);
    }

    public void setCurrentStage(IStageInternal stage) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public IStage registerStage(IStageInternal stage) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public IFlowBus registerFlowBus(String name, IFlowBus flowBus) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public IContextController registerContextController(String name, IContextController context) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public ITimer registerTimer(String name, ITimer timer) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public void registerObject(String name, Object object) {
    }

    public String resolveObjectName(Object object) {
        throw new UnsupportedOperationException("Operation not supported by mock!");
    }

    public Object resolveObject(String objectName) {
        return null;
    }


}