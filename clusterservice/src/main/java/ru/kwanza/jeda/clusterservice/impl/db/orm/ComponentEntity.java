package ru.kwanza.jeda.clusterservice.impl.db.orm;

import ru.kwanza.dbtool.orm.annotations.*;
import ru.kwanza.jeda.clusterservice.IClusteredComponent;
import ru.kwanza.toolbox.fieldhelper.FieldHelper;

import java.sql.Types;
import java.util.Collection;

/**
 * @author Alexander Guzanov
 */
@Entity(table = "jeda_clustered_component", name = "jeda.clusterservice.ComponentEntity")
public class ComponentEntity {
    @IdField(value = "id", type = Types.VARCHAR)
    private String id;
    @Field("name")
    private String name;
    @Field("node_id")
    private Integer nodeId;
    @Field("hold_node_id")
    private Integer holdNodeId;
    @Field("wait_for_return")
    private Boolean waitForReturn;
    @Field("repaired")
    private Boolean repaired;
    @Field("last_activity")
    private Long lastActivity;
    @VersionField("version")
    private Long version;

    @ManyToOne(property = "nodeId")
    private NodeEntity node;

    @ManyToOne(property = "holdNodeId")
    private NodeEntity holdNode;

    public ComponentEntity() {
    }

    public ComponentEntity(Integer nodeId, String name) {
        this.id = createId(nodeId, name);
        this.nodeId = nodeId;
        this.name = name;
        this.lastActivity = System.currentTimeMillis();
        this.repaired = false;
        this.waitForReturn = false;
    }

    public static String createId(Integer nodeId, String name) {
        return nodeId.toString() + "_" + name;
    }

    public String getId() {
        return id;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public Long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Integer getHoldNodeId() {
        return holdNodeId;
    }

    public void setHoldNodeId(Integer holdNodeId) {
        this.holdNodeId = holdNodeId;
    }

    public Boolean getWaitForReturn() {
        return waitForReturn;
    }

    public void setWaitForReturn(Boolean waitForReturn) {
        this.waitForReturn = waitForReturn;
    }

    public Boolean getRepaired() {
        return repaired;
    }

    public void setRepaired(Boolean repaired) {
        this.repaired = repaired;
    }

    public NodeEntity getNode() {
        return node;
    }

    public NodeEntity getHoldNode() {
        return holdNode;
    }

    public static Collection<String> getIds(final int nodeId, Collection<IClusteredComponent> modules) {
        return FieldHelper.getFieldCollection(modules, new FieldHelper.Field<IClusteredComponent, String>() {
            public String value(IClusteredComponent cm) {
                return createId(nodeId, cm.getName());
            }
        });
    }

    public WaitForReturnComponent getWaitEntity() {
        return new WaitForReturnComponent(id, true);
    }

    public void clearMarkers() {
        this.holdNodeId = null;
        this.waitForReturn = false;
        this.repaired = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentEntity component = (ComponentEntity) o;

        if (id != null ? !id.equals(component.id) : component.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
