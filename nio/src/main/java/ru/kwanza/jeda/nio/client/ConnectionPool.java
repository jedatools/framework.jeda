package ru.kwanza.jeda.nio.client;

import ru.kwanza.jeda.api.internal.AbstractResourceController;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Guzanov Alexander
 */
class ConnectionPool extends AbstractResourceController {
    private AtomicInteger batchSize;
    private ConcurrentMap<Connection, ConnectionHolder> leasedConnections = new ConcurrentHashMap<Connection, ConnectionHolder>();
    private LinkedBlockingQueue<ConnectionHolder> availableConnections = new LinkedBlockingQueue<ConnectionHolder>();
    private InetSocketAddress address;
    private IConnectionPoolConfigurator configurator;
    private volatile int maxBatchSize;

    public ConnectionPool(InetSocketAddress address, IConnectionPoolConfigurator configurator) {
        this.address = address;
        this.configurator = configurator;
        this.maxBatchSize = configurator.getPoolSize(address);
        this.batchSize = new AtomicInteger(maxBatchSize);
    }

    public void setMaxPoolSize(int maxPoolSize) {
        int delta = maxPoolSize - maxBatchSize;
        this.batchSize.addAndGet(delta);
    }

    public void throughput(int count, int batchSize, long millis, boolean success) {
    }

    public void input(long count) {
    }

    public double getInputRate() {
        return 0;
    }

    public double getThroughputRate() {
        return 0;
    }

    public int getBatchSize() {
        int size = batchSize.get();
        return size < 0 ? 0 : size;
    }

    public int getThreadCount() {
        return 1;
    }


    public ConnectionContext getConnectionContext(Connection connection) {
        ConnectionHolder connectionHolder = leasedConnections.get(connection);
        return connectionHolder == null ? null : connectionHolder.getContext();
    }

    public void write(TCPNIOTransport transport, ITransportEvent event) {
        ConnectionConfig connectionConfig = event.getConnectionConfig();
        if (connectionConfig.isKeepAlive()) {
            ConnectionHolder holder = availableConnections.poll();
            if (holder != null && !holder.isTimedOut()) {
                registerConnectionHolder(holder, event, configurator.getKeepAliveTimeout(address));
                holder.getConnection().write(event.getContent());
                batchSize.decrementAndGet();
                return;
            } else if (holder != null) {
                batchSize.decrementAndGet();
                leasedConnections.remove(holder.getConnection());
                holder.getConnection().closeSilently();
            }
        }
        int i = batchSize.decrementAndGet();
        transport.connect(connectionConfig.getEndpoint(), new ConnectCompletionHandler(this, event));
    }


    void registerConnection(Connection connection, ITransportEvent event) {
        registerConnectionHolder(
                new ConnectionHolder(connection, event.getConnectionConfig(),
                        configurator.getMaxRequestCount(address)),
                event, configurator.getKeepAliveTimeout(address));
    }

    void registerConnectionHolder(ConnectionHolder holder, ITransportEvent event,
                                  long keepAliveTimeout) {
        holder.update(keepAliveTimeout);
        holder.getContext().setRequestEvent(event);
        Connection connection = holder.getConnection();
        leasedConnections.put(connection, holder);
    }

    void releaseConnection(Connection result, ConnectionContext context) {
        if (context != null && context.getRequestEvent().getConnectionConfig().isKeepAlive()) {
            returnConnection(result, context.getRequestEvent().getConnectionConfig());
        }
    }


    void returnConnection(Connection result, ConnectionConfig config) {
        returnConnection(result, config, false);
    }

    void returnConnection(Connection result, ConnectionConfig config, boolean close) {
        if (result != null) {
            if (config.isKeepAlive()) {
                ConnectionHolder holder = leasedConnections.remove(result);
                if (holder != null && result.isOpen() && !close) {
                    availableConnections.offer(holder);
                }
            }
        }
        int i = batchSize.incrementAndGet();
        getStage().getThreadManager().adjustThreadCount(getStage(), getThreadCount());
    }
}
