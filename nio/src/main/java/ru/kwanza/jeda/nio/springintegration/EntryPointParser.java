package ru.kwanza.jeda.nio.springintegration;

import ru.kwanza.jeda.core.springintegration.JedaBeanDefinition;
import ru.kwanza.jeda.core.springintegration.JedaBeanDefinitionParser;
import ru.kwanza.jeda.nio.server.http.EntryPoint;
import ru.kwanza.jeda.nio.server.http.IEntryPoint;
import ru.kwanza.jeda.nio.server.http.IEntryPointKeystore;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * @author: Guzanov Alexander
 */
class EntryPointParser extends JedaBeanDefinitionParser {
    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder definitionBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(EntryPoint.class);
        List<Element> childElements = DomUtils.getChildElements(element);
        XmlReaderContext readerContext = parserContext.getReaderContext();
        NamespaceHandlerResolver namespaceHandlerResolver = readerContext.getNamespaceHandlerResolver();

        String host = element.getAttribute("host");
        if (StringUtils.hasText(host)) {
            definitionBuilder.addConstructorArgValue(host);
        }

        String port = element.getAttribute("port");
        definitionBuilder.addConstructorArgValue(port);

        String keepAliveMaxRequestsCount = element.getAttribute("keepAliveMaxRequestsCount");
        if (StringUtils.hasText(keepAliveMaxRequestsCount)) {
            definitionBuilder.addPropertyValue("keepAliveMaxRequestsCount", keepAliveMaxRequestsCount);
        }

        String keepAliveIdleTimeout = element.getAttribute("keepAliveIdleTimeout");
        if (StringUtils.hasText(keepAliveIdleTimeout)) {
            definitionBuilder.addPropertyValue("keepAliveIdleTimeout", keepAliveIdleTimeout);
        }

        String threadCount = element.getAttribute("threadCount");
        if (StringUtils.hasText(threadCount)) {
            definitionBuilder.addPropertyValue("threadCount", threadCount);
        }

        String serverConnectionBacklog = element.getAttribute("serverConnectionBacklog");
        if (StringUtils.hasText(serverConnectionBacklog)) {
            definitionBuilder.addPropertyValue("serverConnectionBacklog", serverConnectionBacklog);
        }

        String serverSocketSoTimeout = element.getAttribute("serverSocketSoTimeout");
        if (StringUtils.hasText(serverSocketSoTimeout)) {
            definitionBuilder.addPropertyValue("serverSocketSoTimeout", serverSocketSoTimeout);
        }

        String connectionIdleTimeout = element.getAttribute("connectionIdleTimeout");
        if (StringUtils.hasText(connectionIdleTimeout)) {
            definitionBuilder.addPropertyValue("connectionIdleTimeout", connectionIdleTimeout);
        }

        for (Element e : childElements) {
            String namespaceURI = e.getNamespaceURI();
            NamespaceHandler handler = namespaceHandlerResolver.resolve(namespaceURI);
            if (handler == null) {
                readerContext.error("Unable to locate Spring NamespaceHandler" +
                        " for XML schema namespace [" + namespaceURI + "]", e);
            } else {
                BeanDefinition bean = handler.parse(e, parserContext);
                if (bean instanceof JedaBeanDefinition) {
                    JedaBeanDefinition jedaBeanDefinition = (JedaBeanDefinition) bean;
                    if (jedaBeanDefinition.getType() == IEntryPointKeystore.class) {
                        definitionBuilder.addPropertyReference("keystore", jedaBeanDefinition.getId());
                    }
                }
            }
        }


        return createJedaDefinition(definitionBuilder.getBeanDefinition(), IEntryPoint.class, element, parserContext);
    }
}
