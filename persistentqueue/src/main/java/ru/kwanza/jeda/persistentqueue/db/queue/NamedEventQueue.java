package ru.kwanza.jeda.persistentqueue.db.queue;

import ru.kwanza.dbtool.orm.annotations.Entity;
import ru.kwanza.jeda.persistentqueue.DefaultPersistableEvent;
import ru.kwanza.jeda.persistentqueue.db.base.BaseNamedEventQueue;
import ru.kwanza.toolbox.SerializationHelper;

/**
 * @author Alexander Guzanov
 */
@Entity(name = "jeda.persistentqueue.db.NamedEventQueue", table = "jeda_event_nqueue")
public class NamedEventQueue<E extends DefaultPersistableEvent> extends BaseNamedEventQueue<E> {


    public NamedEventQueue(Long id, Integer nodeId, byte[] eventData, String queueName) {
        super(id, nodeId, eventData, queueName);
    }

    public static class Helper extends BaseEventQueueHelper<NamedEventQueue, DefaultPersistableEvent> {
        private String queueName;

        public Helper(String queueName) {
            this.queueName = queueName;
        }

        public Class<NamedEventQueue> getORMClass() {
            return NamedEventQueue.class;
        }

        public NamedEventQueue buildRecord(DefaultPersistableEvent event, int nodeId) {
            try {
                return new NamedEventQueue(event.getPersistId(), nodeId, SerializationHelper.objectToBytes(event), queueName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String getQueueNameField() {
            return "queueName";
        }

        public String getQueueNameValue() {
            return queueName;
        }

    }
}
