package ru.kwanza.jeda.persistentqueue.berkeley;

/**
 * @author Guzanov Alexander
 */
public class TestBerkeleyPersistenceControllerWithArjuna extends TestBerkeleyPersistenceController {
    @Override
    protected String getContextName() {
        return "berkeley-persistentqueue-config-arjuna.xml";
    }
}
