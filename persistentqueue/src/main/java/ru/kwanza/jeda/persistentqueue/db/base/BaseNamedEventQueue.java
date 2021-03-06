package ru.kwanza.jeda.persistentqueue.db.base;

import ru.kwanza.dbtool.orm.annotations.Field;
import ru.kwanza.jeda.persistentqueue.DefaultPersistableEvent;

/**
 * @author Alexander Guzanov
 */
public class BaseNamedEventQueue<E extends DefaultPersistableEvent> extends BaseEventQueue<E> {
    @Field("queue_name")
    private String queueName;

    public BaseNamedEventQueue(Long id, Integer nodeId, byte[] eventData, String queueName) {
        super(id, nodeId, eventData);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }
}
