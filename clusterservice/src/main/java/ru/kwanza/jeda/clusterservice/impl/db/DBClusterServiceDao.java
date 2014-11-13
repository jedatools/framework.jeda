package ru.kwanza.jeda.clusterservice.impl.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.kwanza.dbtool.core.UpdateException;
import ru.kwanza.dbtool.orm.api.IEntityManager;
import ru.kwanza.dbtool.orm.api.IQuery;
import ru.kwanza.dbtool.orm.api.If;
import ru.kwanza.jeda.clusterservice.IClusteredComponent;
import ru.kwanza.jeda.clusterservice.Node;
import ru.kwanza.jeda.clusterservice.impl.db.orm.ComponentEntity;
import ru.kwanza.jeda.clusterservice.impl.db.orm.NodeEntity;
import ru.kwanza.jeda.clusterservice.impl.db.orm.WaitForReturnComponent;
import ru.kwanza.toolbox.fieldhelper.FieldHelper;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * @author Alexander Guzanov
 */
public class DBClusterServiceDao {
    private static Logger logger = LoggerFactory.getLogger(DBClusterService.class);

    @Resource(name = "dbtool.IEntityManager")
    private IEntityManager em;
    @Autowired
    private ComponentRepository repository;

    private IQuery<NodeEntity> queryActive;
    private IQuery<NodeEntity> queryPassive;
    private IQuery<NodeEntity> queryAll;
    private IQuery<ComponentEntity> queryForComponents;
    private IQuery<ComponentEntity> queryForAlienStale;

    @PostConstruct
    public void init() {
        queryActive = em.queryBuilder(NodeEntity.class).where(If.isGreater("lastActivity")).create();
        queryPassive = em.queryBuilder(NodeEntity.class).where(If.isLessOrEqual("lastActivity")).create();
        queryAll = em.queryBuilder(NodeEntity.class).create();
        queryForComponents = em.queryBuilder(ComponentEntity.class)
                .where(
                        If.and(
                                If.isEqual("nodeId"),
                                If.in("name"))
                ).create();

        queryForAlienStale = em.queryBuilder(ComponentEntity.class)
                .lazy()
                .where(
                        If.and(
                                If.notEqual("nodeId"),
                                If.in("name"),
                                If.isLessOrEqual("lastActivity"),
                                If.isEqual("repaired", If.valueOf(false))
                        )).create();
    }


    public NodeEntity findOrCreateNode(NodeEntity node) {
        if (em.readByKey(NodeEntity.class, node.getId()) == null) {
            try {
                em.create(node);
            } catch (UpdateException e) {
                if (em.readByKey(NodeEntity.class, node.getId()) == null) {
                    throw new IllegalStateException("Can't register node in database!");
                }
            }
        }

        return node;
    }

    public ComponentEntity findOrCreateComponent(NodeEntity node, IClusteredComponent component) {
        ComponentEntity result;

        final String id = ComponentEntity.createId(node.getId(), component.getName());
        if ((result = em.readByKey(ComponentEntity.class, id)) == null) {
            try {
                result = em.create(new ComponentEntity(node.getId(), component.getName()));
            } catch (UpdateException e) {
                result = em.readByKey(ComponentEntity.class, id);
                if (result == null) {
                    throw new RuntimeException("Can't create component!");
                }
            }
        }

        return result;
    }

    public Collection<ComponentEntity> loadComponentsByKey(Collection<String> keys) {
        Collection<ComponentEntity> items = em.readByKeys(ComponentEntity.class, keys);
        em.fetchLazy(ComponentEntity.class, items);

        return items;
    }


    public void updateComponents(Collection<ComponentEntity> items) throws UpdateException {
        em.update(ComponentEntity.class, items);
    }

    public List<? extends Node> selectActiveNodes() {
        return queryActive.prepare().setParameter(1, System.currentTimeMillis()).selectList();
    }

    public List<? extends Node> selectPassiveNodes() {
        return queryPassive.prepare().setParameter(1, System.currentTimeMillis()).selectList();
    }

    public List<? extends Node> selectNodes() {
        return queryAll.prepare().setParameter(1, System.currentTimeMillis()).selectList();
    }


    public List<ComponentEntity> selectAlienStaleComponents(Node node) {
        return queryForAlienStale.prepare()
                .setParameter(1, node.getId())
                .setParameter(2, repository.getActiveComponents().keySet())
                .setParameter(3, System.currentTimeMillis())
                .selectList();
    }

    public List<ComponentEntity> selectAllComponents(Node node) {
        return queryForComponents.prepare()
                .setParameter(1, node.getId())
                .setParameter(2, repository.getComponents().keySet())
                .selectList();
    }

    public Collection<ComponentEntity> selectWaitForReturn() {
        return em.readByKeys(ComponentEntity.class,
                FieldHelper.getFieldCollection(repository.getPassiveEntities(),
                        FieldHelper.construct(ComponentEntity.class, "id")));
    }


    public void markWaitForReturn() {
        try {
            em.update(WaitForReturnComponent.class, FieldHelper.getFieldCollection(repository.getPassiveEntities(),
                    FieldHelper.<ComponentEntity, WaitForReturnComponent>construct(ComponentEntity.class, "waitEntity")));
        } catch (UpdateException e) {
            //todo aguzanov log error;
        }
    }

}
