package ru.kwanza.jeda.clusterservice.impl.db;

import junit.framework.Assert;
import org.dbunit.Assertion;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import ru.kwanza.jeda.clusterservice.impl.db.orm.ClusteredComponent;
import ru.kwanza.jeda.clusterservice.impl.db.orm.ClusterNode;
import ru.kwanza.toolbox.fieldhelper.FieldHelper;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Alexander Guzanov
 */
@ContextConfiguration(locations = "application-config_3.xml")
public class TestDBClusterService_3 extends AbstractDBClusterService {
    @Resource(name = "repair_module_1")
    private RepairableTestComponent m1;
    @Resource(name = "repair_module_2")
    private RepairableTestComponent m2;
    @Resource(name = "jeda.clusterservice.DBClusterService")
    private DBClusterService service;
    @Resource(name = "jeda.clusterservice.DBClusterService2")
    private DBClusterService service2;

    private static FieldHelper.Field<DBClusterService, ConcurrentMap<Integer, ConcurrentMap<DBClusterService.Supervisor.RepairWorker, ClusteredComponent>>>
            repairingNodes = FieldHelper.construct(DBClusterService.class, "supervisor.repairingNodes");


    @Test
    public void testCreateExistedNodeEntityAndModules() throws Exception {
        Assertion.assertEqualsIgnoreCols(getResourceSet("data_set_3_1.xml"),
                getActualDataSet("jeda_cluster_service"), "jeda_cluster_service", new String[]{"last_activity"});

        Assertion.assertEqualsIgnoreCols(getResourceSet("data_set_3_2.xml"),
                getActualDataSet("jeda_clustered_module"), "jeda_clustered_module", new String[]{"last_repaired"});
    }


    @Test
    public void repair() throws Exception {
        Thread.sleep(1000);
        Assert.assertEquals(true, m1.isStarted());
        Assert.assertEquals(false, m1.isRepairing());
        Assert.assertEquals(false, m1.isRepaired());
        Assert.assertEquals(false, m1.isStopped());
        Assert.assertEquals(true, m2.isStarted());
        Assert.assertEquals(false, m2.isRepairing());
        Assert.assertEquals(false, m2.isRepaired());
        Assert.assertEquals(false, m2.isStopped());

        Assert.assertEquals(2,service.getActiveNodes().size());
        Assert.assertEquals(2,service2.getActiveNodes().size());
        Assert.assertEquals(0,service.getPassiveNodes().size());
        Assert.assertEquals(0,service2.getPassiveNodes().size());

        service.destroy();
        Thread.sleep(1000);

        Assert.assertEquals(false, m1.isStarted());
        Assert.assertEquals(false, m1.isRepairing());
        Assert.assertEquals(false, m1.isRepaired());
        Assert.assertEquals(true, m1.isStopped());
        Assert.assertEquals(true, m2.isStarted());
        Assert.assertEquals(false, m2.isRepairing());
        Assert.assertEquals(false, m2.isRepaired());
        Assert.assertEquals(false, m2.isStopped());

        ClusterNode clusterNode = em.readByKey(ClusterNode.class, 1l);
        ClusteredComponent clusteredComponent = em.readByKey(ClusteredComponent.class, "1_repairable_module");
        clusterNode.setLastActivity(clusteredComponent.getLastActivity());

        em.update(clusterNode);
        m2.setRepaired(true);

        Thread.sleep(1000);

        Assert.assertEquals(false, m1.isStarted());
        Assert.assertEquals(false, m1.isRepairing());
        Assert.assertEquals(false, m1.isRepaired());
        Assert.assertEquals(true, m1.isStopped());
        Assert.assertEquals(true, m2.isStarted());
        Assert.assertEquals(true, m2.isRepairing());
        Assert.assertEquals(true, m2.isRepaired());
        Assert.assertEquals(false, m2.isStopped());

        Assert.assertEquals(1,repairingNodes.value(service2).size());
        Assert.assertFalse(repairingNodes.value(service2).get(1).isEmpty());

        Assert.assertEquals(1,service.getActiveNodes().size());
        Assert.assertEquals(1,service2.getActiveNodes().size());
        Assert.assertEquals(1,service.getPassiveNodes().size());
        Assert.assertEquals(1,service2.getPassiveNodes().size());
        service.destroy();
    }

}
