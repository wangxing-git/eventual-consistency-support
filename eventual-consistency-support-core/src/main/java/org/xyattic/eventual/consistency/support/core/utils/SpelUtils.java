package org.xyattic.eventual.consistency.support.core.utils;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanExpressionContextAccessor;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.expression.*;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;

import java.util.Map;

/**
 * @author wangxing
 * @create 2018/4/2
 */
public class SpelUtils {

    private static final SpelExpressionParser SPEL_EXPRESSION_PARSER;
    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext();
    private static final BeanResolver BEAN_RESOLVER = new BeanFactoryResolver(SpringBeanUtils.getBeanFactory());
    private static final CustomPropertyAccessor CUSTOM_PROPERTY_ACCESSOR = new CustomPropertyAccessor();
    private static final BeanExpressionContextAccessor BEAN_EXPRESSION_CONTEXT_ACCESSOR = new BeanExpressionContextAccessor();
    private static final BeanFactoryAccessor BEAN_FACTORY_ACCESSOR = new BeanFactoryAccessor();
    private static final MapAccessor MAP_ACCESSOR = new MapAccessor();
    private static final EnvironmentAccessor ENVIRONMENT_ACCESSOR = new EnvironmentAccessor();
    private static final TypeLocator TYPE_LOCATOR = new StandardTypeLocator(SpringBeanUtils.getBeanFactory().getBeanClassLoader());
    private static TypeConverter TYPE_CONVERTER;

    static {
        final SpelParserConfiguration configuration = new SpelParserConfiguration();
        SPEL_EXPRESSION_PARSER = new SpelExpressionParser(configuration);
        final ConversionService conversionService = SpringBeanUtils.getBeanFactory().getConversionService();
        if (conversionService != null) {
            TYPE_CONVERTER = new StandardTypeConverter(conversionService);
        }
    }

    private static String resolve(final String value) {
        return PropertyPlaceholder.resolvePlaceholders(value);
    }

    public static <T> T parse(final String expressionString, final Map<String, Object> rootMap) {
        return parse(expressionString, rootMap, null);
    }

    public static <T> T parse(final String expressionString) {
        return parse(expressionString, null, null);
    }

    public static <T> T parse(final String expressionString, Map<String, Object> rootMap, final Class<T> valClass) {
        rootMap = rootMap == null ? Maps.newHashMap() : rootMap;
        return SPEL_EXPRESSION_PARSER
                .parseExpression(resolve(expressionString), PARSER_CONTEXT)
                .getValue(getEvaluationContext(rootMap), valClass);
    }

    public static <T> T parse(final String expressionString, final Class<T> valClass) {
        return parse(expressionString, null, valClass);
    }

    private static StandardEvaluationContext getEvaluationContext(final Map<String, Object> rootMap) {
        final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        final CustomRootObject rootObject = new CustomRootObject();
        rootObject.map = rootMap;
        rootObject.beanFactory = SpringBeanUtils.getBeanFactory();
        rootObject.environment = SpringContextUtils.getEnvironment();
        evaluationContext.setRootObject(rootObject);
        evaluationContext.setBeanResolver(BEAN_RESOLVER);
        evaluationContext.addPropertyAccessor(CUSTOM_PROPERTY_ACCESSOR);
        evaluationContext.addPropertyAccessor(BEAN_EXPRESSION_CONTEXT_ACCESSOR);
        evaluationContext.addPropertyAccessor(BEAN_FACTORY_ACCESSOR);
        evaluationContext.addPropertyAccessor(MAP_ACCESSOR);
        evaluationContext.addPropertyAccessor(ENVIRONMENT_ACCESSOR);
        evaluationContext.setTypeLocator(TYPE_LOCATOR);
        if (TYPE_CONVERTER != null) {
            evaluationContext.setTypeConverter(TYPE_CONVERTER);
        }
        return evaluationContext;
    }

    public static class CustomRootObject {

        private Map<String, Object> map = Maps.newHashMap();
        private BeanFactory beanFactory;
        private Environment environment;

        public Map<String, Object> getMap() {
            return this.map;
        }

        public void setMap(final Map<String, Object> map) {
            this.map = map;
        }

        public BeanFactory getBeanFactory() {
            return this.beanFactory;
        }

        public void setBeanFactory(final BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        public Environment getEnvironment() {
            return this.environment;
        }

        public void setEnvironment(final Environment environment) {
            this.environment = environment;
        }
    }

    public static class CustomPropertyAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{CustomRootObject.class};
        }

        @Override
        public boolean canRead(final EvaluationContext context, final Object target, final String name) throws AccessException {
            if (target instanceof CustomRootObject) {
                final CustomRootObject rootObject = (CustomRootObject) target;
                return rootObject.map.containsKey(name) || rootObject.beanFactory.containsBean(name) || rootObject.environment.containsProperty(name);
            }
            return false;
        }

        @Override
        public TypedValue read(final EvaluationContext context, final Object target, final String name) throws AccessException {
            final Object val;
            final CustomRootObject rootObject = (CustomRootObject) target;
            if (rootObject.map.containsKey(name)) {
                val = rootObject.map.get(name);
            } else if (rootObject.beanFactory.containsBean(name)) {
                val = rootObject.beanFactory.getBean(name);
            } else {
                val = rootObject.environment.getProperty(name);
            }
            return new TypedValue(val);
        }

        @Override
        public boolean canWrite(final EvaluationContext context, final Object target, final String name) throws AccessException {
            return false;
        }

        @Override
        public void write(final EvaluationContext context, final Object target, final String name, final Object newValue) throws AccessException {
            throw new AccessException("不支持的操作");
        }
    }

}