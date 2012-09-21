package ru.kwanza.jeda.core.springintegration;

import ru.kwanza.jeda.api.internal.ISystemManager;
import ru.kwanza.jeda.api.internal.IThreadManager;
import ru.kwanza.jeda.core.threadmanager.stage.StageThreadManager;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Guzanov Alexander
 */
class StageThreadManagerParser extends JedaBeanDefinitionParser {

    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder
                .genericBeanDefinition(StageThreadManager.class);

        definitionBuilder.addConstructorArgValue(element.getAttribute("threadNamePrefix"));
        definitionBuilder.addConstructorArgReference(ISystemManager.class.getName());


        String maxThreadCount = element.getAttribute("maxThreadCount");
        if (StringUtils.hasText(maxThreadCount)) {
            definitionBuilder.addPropertyValue("maxThreadCount", maxThreadCount);
        }

        String maxSingleEventAttempt = element.getAttribute("maxSingleEventAttempt");
        if (StringUtils.hasText(maxSingleEventAttempt)) {
            definitionBuilder.addPropertyValue("maxSingleEventAttempt", maxSingleEventAttempt);
        }

        return createFlexFlowDefinition(definitionBuilder.getBeanDefinition(),
                IThreadManager.class, element, parserContext);
    }
}

