package ru.kwanza.jeda.timerservice.pushtimer.springintegration;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import ru.kwanza.jeda.timerservice.pushtimer.springintegration.refs.ConsumerConfigRef;
import ru.kwanza.jeda.timerservice.pushtimer.springintegration.refs.DAORef;
import ru.kwanza.jeda.timerservice.pushtimer.springintegration.refs.TimerClassRef;

/**
 * @author Michael Yeskov
 */
public class JedaTimersNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("timer", new TimerParser());

        registerBeanDefinitionParser("timer-class", new TimerClassParser());
        registerBeanDefinitionParser("timer-class-ref", new RefBeanParser(TimerClassRef.class));

        registerBeanDefinitionParser("consumer-config-ref", new RefBeanParser(ConsumerConfigRef.class));
        registerBeanDefinitionParser("consumer-config", new ConsumerConfigParser());


        registerBeanDefinitionParser("dao-custom", new RefBeanParser(DAORef.class));
        registerBeanDefinitionParser("dao-insert-delete", new DAOParser());
        registerBeanDefinitionParser("dao-insert-single-update", new DAOParser());
        registerBeanDefinitionParser("dao-insert-multi-update", new DAOParser());
        registerBeanDefinitionParser("dao-updating", new DAOParser());

        registerBeanDefinitionParser("mapping", new MappingParser());

    }
}
