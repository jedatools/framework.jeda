package ru.kwanza.jeda.clusterservice;

/**
 * @author Guzanov Alexander
 */
public interface INodeListener {

    public void onNodeLost(Long nodeId, long lastNodeTs);

    public void onNodeActivate(Long nodeId, long lastNodeTs);

    public void onCurrentNodeActivate();

    public void onCurrentNodeLost();
}
