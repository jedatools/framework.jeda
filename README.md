#Description

**JEDA** stands for **J**ava **E**vent **D**riven **A**rchitecture.
This framework was inspired by document  [SEDA](http://www.eecs.harvard.edu/~mdw/proj/seda/).
**JEDA** offers architectural approach for building high load online transaction processing systems.

What are the most common requirements of architecture of classical OLTP:

* to process valuable operations like financial transaction, billing operations, transport transaction
* to guarantee low latency for transaction processing
* to provide ability to scale system
* to support high concurrency

Why SEDA-base architectural approach fits to build OLTP:

* batch operation processing helps to improve performance greatly and achieve low latency processing and system scaling
* low coupling between parts of  the system helps to make it flexible and configurable

# Core architecture

The are two common template of processing operation inspired by incoming request:

* **thread-per-request** approach. In this template we allocate a thread for each incoming request and process it here. It's the most widely used method. It is very convenient model, you can easily implement it. Most of all existing development platform  focused on this method: **Servlet**, **CGI**, **JSP** and so forth. Usually we have a thread pool which is shared among request handlers. It's obvious that this approach besides this simplicity has its drawback. It is the problem with scalability and performance of such system. You can't just make large thread pool and expect that all will be ok. Large number of threads lead to well-known bottleneck: _**thread context switch problem**_. Another important drawback is that your thread usually compete for other system resources like file system (FS), database(DB) and so forth. Besides this situation deteriorated because of blocking manner of interactions with this resources. Generally, each this blocking request to FS or DB can lead to tread context switch; thread context switch on large count of thread can be slow; slow context switch leads to situation when waiting threads just hold resources of DB and FS. So we have vicious circle. The only way to scale system in this situation is to do vertical scaling: more application nodes, more database nodes (sharding) and so forth.
  
* **event-driven** architecture. It's hard to implement in general. This model suppose that all our operations are made in non-blocking manner, we have only a few thread(processes) that process events from the main queue. It's the more natural approach, because of it mirrors the gist of computer systems. Thread is what is made for us by operation system. _"A Computer is a state machine. Threads are for people who can't program state machines"_ @[Alan Cox](https://en.wikiquote.org/wiki/Alan_Cox). There are not many development platform that provide ability to build application with event-driven architecture. As for networking we have **java.nio**, mature frameworks **netty** and **grizzly**, but we still have interactions with FS/DB and generally speaking we can't make async requests to them. _(It should be mentioned that there are **nodeJS** and [async driver for MySQL](https://github.com/sidorares/node-mysql2))_ 

Staged Event Driven Architecture is something between this two approach which considers their prons and cons. 

![Screen Shot 2016-01-20 at 3.31.15 PM.png](https://bitbucket.org/repo/XRaK6n/images/1812642601-Screen%20Shot%202016-01-20%20at%203.31.15%20PM.png)

The fundamental unit of processing within SEDA is the stage. A stage is a self-contained
application component consisting of an event handler, an incoming event queue, and a thread pool.Each stage is managed by one or more controllers that affect resource
consumption, scheduling, thread allocation, and admission control, as described below. Threads
within a stage operate by pulling a batch of events off of the incoming event queue and invoking the
application-supplied event handler. The event handler processes each batch of events, and dispatches
zero or more events by enqueueing them on the event queues of other stages.
Each event processed by a stage is typically a data structure representing a single client
request to the Internet service, for example, an HTTP request for a Web page. However, events may
represent other information relevant to the operation of the service, such as a network connection
being established, the firing of a timer, or an error condition. Throughout this thesis we often use
the terms event and request interchangeably, though technically a request is generated by a client,
while other types of events may be generated by the service internally.
The SEDA programming model exposes batches of events, rather than individual events,
to the application. This allows the application to exploit opportunities for processing multiple events
together, which can lead to higher performance. For example, if the event handler is able to amortize
an expensive operation across multiple events, throughput can be increased. Likewise, data
and cache locality may benefit if the processing for multiple events shares common code and data
structures.

A SEDA application is constructed as a network of stages, connected by event queues.
Event handlers may enqueue events onto another stage by first obtaining a handle to that stage’s
incoming event queue (through a system-provided lookup routine), and then invoking an enqueue
operation on that queue:

![Screen Shot 2016-01-20 at 3.55.39 PM.png](https://bitbucket.org/repo/XRaK6n/images/923587353-Screen%20Shot%202016-01-20%20at%203.55.39%20PM.png)

# Implementation details

**JEDA** in implemented on a basis of Spring framework. All it's components can be accessed through spring context. Framework also includes lightweight http server on a basis of **grizzly**. 
**JEDA** is like a little  application server. All that you need is to initialize proper spring contexts and that's it, no tomcat, jetty or anything more cumbersome if you don't want it.
Besides, **JEDA** has tight integration with spring: all stages and other components can be described through special defined  spring context extensions.

## Modules

There are bunch of modules:

1. **api** - base module where only pure abstractions are defined. Framework user should user only this interfaces in his application. All other modules contains implementations, utils and spring extensions.
1. **core** - contains implementations of JEDA container, threadpools, resource controller
1. **persistentqueue** - some useful implementation of durable queues
1. **clusterservice** - utility that helps to monitor the health of JEDA modules in cluster
1. **context** - persistent context, that can store anything and are intended to help integration different stages.
1. **nio** - stands for "netwoking input-output". Contains http server implementation and network client, implemented according **JEDA** terms.

## Example of stage definition:

It's quite simple to create your own stage. All that you need is to implement your **IEventHandler** and define your what event **IEvent** will be processed by this handler.


```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:jeda-core="http://www.kwanza-systems.ru/schema/jeda-core"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
               http://www.springframework.org/schema/beans/spring-beans.xsd
               http://www.kwanza-systems.ru/schema/jeda-core http://www.kwanza-systems.ru/schema/jeda-core.xsd">

    <import resource="classpath:txn-jta-config.xml"/>
    <import resource="classpath:jeda-core-config.xml"/>
    <import resource="classpath:pendingstore-logonly-config.xml"/>

    <bean id="jtaTransactionManager"
          class="com.atomikos.icatch.jta.AtomikosTransactionManager"
          init-method="init" destroy-method="close">
        <property name="forceShutdown" value="true"/>
    </bean>

    <jeda-core:stage name="Stage1">
        <jeda-core:memory-queue/>
        <jeda-core:static-resource-controller/>
        <jeda-core:admission-controller
                class="ru.kwanza.jeda.core.springintegration.TestAdmissionController"/>
        <jeda-core:event-processor class="ru.kwanza.jeda.core.springintegration.TestEventProcessor1"/>
        <jeda-core:stage-thread-manager threadNamePrefix="TestStage1"/>
    </jeda-core:stage>

    <jeda-core:stage name="Stage2">
        <jeda-core:memory-queue maxSize="10000"/>
        <jeda-core:static-resource-controller batchSize="1000" adjustmentCount="1000" adjustmentInterval="2000"/>
        <jeda-core:event-processor class="ru.kwanza.jeda.core.springintegration.TestEventProcessor2"/>
        <jeda-core:stage-thread-manager threadNamePrefix="TestStage2"/>
    </jeda-core:stage>
</beans>

```

## Real examples

You can find example application implementation here: [application.billing](https://bitbucket.org/aguzanov/application.billing/src). (see gateway module)

Root context:


```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:nio="http://www.kwanza-systems.ru/schema/jeda-nio" xmlns:jeda-core="http://www.kwanza-systems.ru/schema/jeda-core"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.kwanza-systems.ru/schema/jeda-nio http://www.kwanza-systems.ru/schema/jeda-nio.xsd http://www.kwanza-systems.ru/schema/jeda-core http://www.kwanza-systems.ru/schema/jeda-core.xsd">

    <!-- HTTP Server -->

    <nio:http-server name="billing.gateway.JSONServer">
        <nio:entry-point host="${gateway.host}" port="${gateway.port}"/>
    </nio:http-server>

    <!-- Thread Managers -->
    <jeda-core:shared-thread-manager threadNamePrefix="BillingRegistryManagement"
                                     id="billing.gateway.threads.BillingRegistryManagement">
        <jeda-core:share-strategy-by-input-rate-and-waiting-time/>
    </jeda-core:shared-thread-manager>
    <jeda-core:shared-thread-manager threadNamePrefix="BillingSessionManagement"
                                     id="billing.gateway.threads.BillingSessionManagement">
        <jeda-core:share-strategy-by-input-rate-and-waiting-time/>
    </jeda-core:shared-thread-manager>
    <jeda-core:shared-thread-manager threadNamePrefix="BillingPayment"
                                     id="billing.gateway.threads.BillingPayment">
        <jeda-core:share-strategy-by-input-rate-and-waiting-time/>
    </jeda-core:shared-thread-manager>
    <jeda-core:shared-thread-manager threadNamePrefix="BillingQuery"
                                     id="billing.gateway.threads.BillingQuery">
        <jeda-core:share-strategy-by-input-rate-and-waiting-time/>
    </jeda-core:shared-thread-manager>

    <!-- ID Generator -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:pattern>/generateIds</nio:pattern>
        <nio:stage>billing.application.generate.RequestIdGenerator</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.JSONReportBuilder" class="ru.kwanza.billing.gateway.JSONReportBuilder"/>
    <bean id="billing.gateway.JSONListBuilder" class="ru.kwanza.billing.gateway.JSONListBuilder"/>
    <jeda-core:stage name="billing.application.generate.RequestIdGenerator">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor class="ru.kwanza.billing.gateway.http.HttpIdEventProcessor">
            <property name="requestIdGenerator" ref="billing.processor.RequestIdGenerator"/>
            <property name="listBuilder" ref="billing.gateway.JSONListBuilder"/>
        </jeda-core:event-processor>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingPayment"/>
    </jeda-core:stage>

    <!-- RegisterCurrencyOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/RegisterCurrencyOrder</nio:uri>
        <nio:stage>stage.billing.application.process.RegisterCurrencyOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.RegisterCurrencyOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.RegisterCurrencyOrder"/>
    </bean>
    <bean id="billing.gateway.processor.RegisterCurrencyOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.RegisterCurrencyOrder"/>
        <property name="orderProcessor" ref="billing.processor.RegisterCurrencyOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.RegisterCurrencyOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.RegisterCurrencyOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.RegisterCurrencyOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- RegisterIssuerOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/RegisterIssuerOrder</nio:uri>
        <nio:stage>stage.billing.application.process.RegisterIssuerOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.RegisterIssuerOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.RegisterIssuerOrder"/>
    </bean>
    <bean id="billing.gateway.processor.RegisterIssuerOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.RegisterIssuerOrder"/>
        <property name="orderProcessor" ref="billing.processor.RegisterIssuerOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.RegisterIssuerOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.RegisterIssuerOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.RegisterIssuerOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- WalletIssueOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/WalletIssueOrder</nio:uri>
        <nio:stage>stage.billing.application.process.WalletIssueOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.WalletIssueOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.WalletIssueOrder"/>
    </bean>
    <bean id="billing.gateway.processor.WalletIssueOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.WalletIssueOrder"/>
        <property name="orderProcessor" ref="billing.processor.WalletIssueOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.WalletIssueOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.WalletIssueOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.WalletIssueOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- AccountIssueOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/AccountIssueOrder</nio:uri>
        <nio:stage>stage.billing.application.process.AccountIssueOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.AccountIssueOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.AccountIssueOrder"/>
    </bean>
    <bean id="billing.gateway.processor.AccountIssueOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.AccountIssueOrder"/>
        <property name="orderProcessor" ref="billing.processor.AccountIssueOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.AccountIssueOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.AccountIssueOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.AccountIssueOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- WalletAccountIssueOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/WalletAccountIssueOrder</nio:uri>
        <nio:stage>stage.billing.application.process.WalletAccountIssueOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.WalletAccountIssueOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.WalletAccountIssueOrder"/>
    </bean>
    <bean id="billing.gateway.processor.WalletAccountIssueOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.WalletAccountIssueOrder"/>
        <property name="orderProcessor" ref="billing.processor.WalletAccountIssueOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.WalletAccountIssueOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.WalletAccountIssueOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.WalletAccountIssueOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- PaymentOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/PaymentOrder</nio:uri>
        <nio:stage>stage.billing.application.process.PaymentOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.PaymentOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.PaymentOrder"/>
    </bean>
    <bean id="billing.gateway.processor.PaymentOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.PaymentOrder"/>
        <property name="orderProcessor" ref="billing.processor.PaymentOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.PaymentOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.PaymentOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.PaymentOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingPayment"/>
    </jeda-core:stage>

    <!-- OpenSessionOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="30000">
        <nio:uri>/process/OpenSessionOrder</nio:uri>
        <nio:stage>stage.billing.application.process.OpenSessionOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.OpenSessionOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.OpenSessionOrder"/>
    </bean>
    <bean id="billing.gateway.processor.OpenSessionOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.OpenSessionOrder"/>
        <property name="orderProcessor" ref="billing.processor.OpenSessionOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.OpenSessionOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.OpenSessionOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.OpenSessionOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingSessionManagement"/>
    </jeda-core:stage>

    <!-- CloseSessionOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/CloseSessionOrder</nio:uri>
        <nio:stage>stage.billing.application.process.CloseSessionOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.CloseSessionOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.CloseSessionOrder"/>
    </bean>
    <bean id="billing.gateway.processor.CloseSessionOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.CloseSessionOrder"/>
        <property name="orderProcessor" ref="billing.processor.CloseSessionOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.CloseSessionOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.CloseSessionOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.CloseSessionOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingSessionManagement"/>
    </jeda-core:stage>

    <!-- PaymentToolPermissionsOrder -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/PaymentToolPermissionsOrder</nio:uri>
        <nio:stage>stage.billing.application.process.PaymentToolPermissionsOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.PaymentToolPermissionsOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.PaymentToolPermissionsOrder"/>
    </bean>
    <bean id="billing.gateway.processor.PaymentToolPermissionsOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.PaymentToolPermissionsOrder"/>
        <property name="orderProcessor" ref="billing.processor.PaymentToolPermissionsOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.PaymentToolPermissionsOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.PaymentToolPermissionsOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.PaymentToolPermissionsOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingSessionManagement"/>
    </jeda-core:stage>


    <!-- CurrenciesQuery -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/CurrenciesQuery</nio:uri>
        <nio:stage>stage.billing.application.process.CurrenciesQuery</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.CurrenciesQueryParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.query.CurrenciesQuery"/>
    </bean>
    <bean id="billing.gateway.processor.CurrenciesQueryProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.query.CurrenciesQuery"/>
        <property name="orderProcessor" ref="billing.processor.CurrenciesQueryProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.CurrenciesQueryParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.CurrenciesQuery">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.CurrenciesQueryProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- AccountReportQuery -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/AccountReportQuery</nio:uri>
        <nio:stage>stage.billing.application.process.AccountReportQuery</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.AccountReportQueryParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.query.AccountReportQuery"/>
    </bean>
    <bean id="billing.gateway.processor.AccountReportQueryProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.query.AccountReportQuery"/>
        <property name="orderProcessor" ref="billing.processor.AccountReportQueryProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.AccountReportQueryParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.AccountReportQuery">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.AccountReportQueryProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>

    <!-- AccountStatementQuery -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/AccountStatementQuery</nio:uri>
        <nio:stage>stage.billing.application.process.AccountStatementQuery</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.AccountStatementQueryParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.query.AccountStatementQuery"/>
    </bean>
    <bean id="billing.gateway.processor.AccountStatementQueryProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.query.AccountStatementQuery"/>
        <property name="orderProcessor" ref="billing.processor.AccountStatementQueryProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.AccountStatementQueryParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.AccountStatementQuery">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.AccountStatementQueryProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingQuery"/>
    </jeda-core:stage>

    <!-- ConversionInstructionQuery -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/ConversionInstructionQuery</nio:uri>
        <nio:stage>stage.billing.application.process.ConversionInstructionQuery</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.ConversionInstructionQueryParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.query.ConversionInstructionQuery"/>
    </bean>
    <bean id="billing.gateway.processor.ConversionInstructionQueryProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.query.ConversionInstructionQuery"/>
        <property name="orderProcessor" ref="billing.processor.ConversionInstructionQueryProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.ConversionInstructionQueryParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.ConversionInstructionQuery">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.ConversionInstructionQueryProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingQuery"/>
    </jeda-core:stage>

    <!-- Remove currency -->

    <nio:http-handler server="billing.gateway.JSONServer" timeout="15000">
        <nio:uri>/process/RemoveCurrencyOrder</nio:uri>
        <nio:stage>stage.billing.application.process.RemoveCurrencyOrder</nio:stage>
    </nio:http-handler>
    <bean id="billing.gateway.json.RemoveCurrencyOrderParser" class="ru.kwanza.billing.gateway.JSONRequestParser">
        <constructor-arg value="ru.kwanza.billing.api.order.RemoveCurrencyOrder"/>
    </bean>
    <bean id="billing.gateway.processor.RemoveCurrencyOrderProcessor" class="ru.kwanza.billing.gateway.http.HttpEventProcessor">
        <constructor-arg value="ru.kwanza.billing.api.order.RemoveCurrencyOrder"/>
        <property name="orderProcessor" ref="billing.processor.RemoveCurrencyOrderProcessor"/>
        <property name="reportBuilder" ref="billing.gateway.JSONReportBuilder"/>
        <property name="requestParser" ref="billing.gateway.json.RemoveCurrencyOrderParser"/>
    </bean>
    <jeda-core:stage name="stage.billing.application.process.RemoveCurrencyOrder">
        <jeda-core:tx-memory-queue cloneType="NONE" maxSize="10000"/>
        <jeda-core:fixed-batch-size-resource-controller batchSize="1000"/>
        <jeda-core:event-processor factory-bean="billing.gateway.processor.RemoveCurrencyOrderProcessor" factory-method="getInstance"/>
        <jeda-core:thread-manager ref="billing.gateway.threads.BillingRegistryManagement"/>
    </jeda-core:stage>
</beans>

```