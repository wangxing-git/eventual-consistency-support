package org.xyattic.eventual.consistency.support.core.utils

import org.springframework.beans.factory.BeanFactory
import org.springframework.context.expression.*
import org.springframework.core.env.Environment
import org.springframework.expression.*
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.expression.spel.support.StandardTypeConverter
import org.springframework.expression.spel.support.StandardTypeLocator

/**
 * @author wangxing
 * @create 2018/4/2
 */
object SpelUtils {
    private var SPEL_EXPRESSION_PARSER: SpelExpressionParser
    private val PARSER_CONTEXT: ParserContext = TemplateParserContext()
    private val BEAN_RESOLVER: BeanResolver = BeanFactoryResolver(SpringBeanUtils.beanFactory)
    private val CUSTOM_PROPERTY_ACCESSOR = CustomPropertyAccessor()
    private val BEAN_EXPRESSION_CONTEXT_ACCESSOR = BeanExpressionContextAccessor()
    private val BEAN_FACTORY_ACCESSOR = BeanFactoryAccessor()
    private val MAP_ACCESSOR = MapAccessor()
    private val ENVIRONMENT_ACCESSOR = EnvironmentAccessor()
    private val TYPE_LOCATOR: TypeLocator = StandardTypeLocator(SpringBeanUtils.beanFactory.beanClassLoader)
    private var TYPE_CONVERTER: TypeConverter? = null
    private fun resolve(value: String): String {
        return PropertyPlaceholder.resolvePlaceholders(value)
    }

    fun <T> parse(expressionString: String, rootMap: Map<String?, Any?>?): T? {
        return parse(expressionString, rootMap, null)
    }

    fun <T> parse(expressionString: String): T? {
        return parse(expressionString, null, null)
    }

    fun <T> parse(expressionString: String, rootMap: Map<String?, Any?>?, valClass: Class<T>?): T? {
        var map = rootMap
        map = map ?: hashMapOf()
        return SPEL_EXPRESSION_PARSER
                .parseExpression(resolve(expressionString), PARSER_CONTEXT)
                .getValue(getEvaluationContext(map), valClass)
    }

    fun <T> parse(expressionString: String, valClass: Class<T>?): T? {
        return parse(expressionString, null, valClass)
    }

    private fun getEvaluationContext(rootMap: Map<String?, Any?>?): StandardEvaluationContext {
        val evaluationContext = StandardEvaluationContext()
        val rootObject = CustomRootObject()
        rootObject.map = rootMap ?: hashMapOf()
        rootObject.beanFactory = SpringBeanUtils.beanFactory
        rootObject.environment = SpringContextUtils.environment
        evaluationContext.setRootObject(rootObject)
        evaluationContext.beanResolver = BEAN_RESOLVER
        evaluationContext.addPropertyAccessor(CUSTOM_PROPERTY_ACCESSOR)
        evaluationContext.addPropertyAccessor(BEAN_EXPRESSION_CONTEXT_ACCESSOR)
        evaluationContext.addPropertyAccessor(BEAN_FACTORY_ACCESSOR)
        evaluationContext.addPropertyAccessor(MAP_ACCESSOR)
        evaluationContext.addPropertyAccessor(ENVIRONMENT_ACCESSOR)
        evaluationContext.typeLocator = TYPE_LOCATOR
        if (TYPE_CONVERTER != null) {
            evaluationContext.typeConverter = TYPE_CONVERTER!!
        }
        return evaluationContext
    }

    class CustomRootObject {

        var map: Map<String?, Any?> = hashMapOf()

        lateinit var beanFactory: BeanFactory

        lateinit var environment: Environment

    }

    class CustomPropertyAccessor : PropertyAccessor {
        override fun getSpecificTargetClasses(): Array<Class<*>> {
            return arrayOf(CustomRootObject::class.java)
        }

        override fun canRead(context: EvaluationContext, target: Any, name: String): Boolean {
            if (target is CustomRootObject) {
                val rootObject = target
                return rootObject.map.containsKey(name) || rootObject.beanFactory.containsBean(name) || rootObject.environment.containsProperty(name)
            }
            return false
        }

        override fun read(context: EvaluationContext, target: Any, name: String): TypedValue {
            val `val`: Any?
            val rootObject = target as CustomRootObject
            if (rootObject.map.containsKey(name)) {
                `val` = rootObject.map[name]
            } else if (rootObject.beanFactory.containsBean(name)) {
                `val` = rootObject.beanFactory.getBean(name)
            } else {
                `val` = rootObject.environment.getProperty(name)
            }
            return TypedValue(`val`)
        }

        override fun canWrite(context: EvaluationContext, target: Any, name: String): Boolean {
            return false
        }

        override fun write(context: EvaluationContext, target: Any, name: String, newValue: Any) {
            throw AccessException("不支持的操作")
        }
    }

    init {
        val configuration = SpelParserConfiguration()
        SPEL_EXPRESSION_PARSER = SpelExpressionParser(configuration)
        val conversionService = SpringBeanUtils.beanFactory.conversionService
        if (conversionService != null) {
            TYPE_CONVERTER = StandardTypeConverter(conversionService)
        }
    }
}