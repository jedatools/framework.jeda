package ru.kwanza.jeda.persistentqueue.db;

import junit.framework.Assert;
import org.dbunit.Assertion;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.SortedDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import ru.kwanza.dbtool.core.DBTool;
import ru.kwanza.jeda.api.IPriorityEvent;
import ru.kwanza.jeda.clusterservice.Node;
import ru.kwanza.jeda.persistentqueue.DefaultPriorityPersistableEvent;
import ru.kwanza.jeda.persistentqueue.IQueuePersistenceController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Alexander Guzanov
 */
@ContextConfiguration("test-config_3.xml")
public class TestDBPersistentController_3 extends AbstractTransactionalJUnit4SpringContextTests {
    @Resource(name = "defaultQueueController")
    private IQueuePersistenceController<DefaultPriorityPersistableEvent> controller;
    private Node node1 = new Node(1);
    private Node node2 = new Node(2);

    @Resource(name = "dbtool.DBTool")
    protected DBTool dbTool;

    protected IDataSet getActualDataSet(String tablename) throws Exception {
        return new SortedDataSet(new DatabaseConnection(dbTool.getJDBCConnection())
                .createDataSet(new String[]{tablename}));
    }

    protected IDataSet getResourceSet(String fileName) throws DataSetException {
        return new SortedDataSet(new FlatXmlDataSetBuilder().build(this.getClass().getResourceAsStream(fileName)));
    }

    @Test
    public void testQueueName() {
        Assert.assertEquals("DBQueuePersistenceController.ru.kwanza.jeda.persistentqueue.db.queue.PriorityEventQueue",
                controller.getQueueName());
    }

    @Test
    public void testPersist() throws Exception {
        controller.persist(Arrays.asList(new DefaultPriorityPersistableEvent(1l, IPriorityEvent.Priority.CRITICAL)), node1);
        controller.persist(Arrays.asList(new DefaultPriorityPersistableEvent(2l, IPriorityEvent.Priority.HIGH)), node2);

        Assertion.assertEqualsIgnoreCols(getResourceSet("dataset_3_1.xml"),
                getActualDataSet("jeda_event_pqueue"), "jeda_event_pqueue", new String[]{"data"});
    }

    @Test
    public void testDelete() throws Exception {
        controller.delete(Arrays.asList(new DefaultPriorityPersistableEvent(3l,IPriorityEvent.Priority.CRITICAL)), node1);
        controller.delete(Arrays.asList(new DefaultPriorityPersistableEvent(13l,IPriorityEvent.Priority.CRITICAL)), node2);
        Assertion.assertEqualsIgnoreCols(getResourceSet("dataset_3_2.xml"),
                getActualDataSet("jeda_event_pqueue"), "jeda_event_pqueue", new String[]{"data"});
    }

    @Test
    public void testLoad() {
        Collection<DefaultPriorityPersistableEvent> load = controller.load(10, node1);
        Assert.assertEquals(8, load.size());


        load = controller.load(4, node1);
        Assert.assertEquals(4, load.size());
        load = controller.load(100, node2);
        Assert.assertEquals(8, load.size());
    }

}

