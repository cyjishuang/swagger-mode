package com.yueh.swagger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Function;
import com.yueh.swagger.model.ApiJsonObject;
import com.yueh.swagger.model.ApiJsonProperty;
import com.yueh.swagger.model.ApiJsonResult;
import com.yueh.swagger.model.ApiSingleParam;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import springfox.documentation.schema.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.TypeNameProviderPlugin;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.contexts.DocumentationContext;

import java.lang.reflect.Field;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.yueh.swagger.CommonData.*;
import static org.springframework.util.ObjectUtils.isEmpty;
import static springfox.documentation.schema.Collections.collectionElementType;
import static springfox.documentation.spi.schema.contexts.ModelContext.inputParam;

/**
 * Created by yueh on 2018/9/13.
 */
public class ModelCache {

    private Map<String, Model> knownModels = new HashMap<>();
    private DocumentationContext context;
    private Function<ResolvedType, ? extends ModelReference> factory;
    private TypeResolver typeResolver = new TypeResolver();
    private Map<String, ApiSingleParam> paramMap = new HashMap<>();


    private ModelCache() {
    }


    public static ModelCache getInstance() {
        return ModelCacheSub.instance;
    }

    public ModelCache setParamMap(Map<String, ApiSingleParam> paramMap) {
        this.paramMap = paramMap;
        return getInstance();
    }


    public ModelCache setFactory(Function<ResolvedType, ? extends ModelReference> factory) {
        this.factory = factory;
        return getInstance();
    }

    public void setContext(DocumentationContext context) {
        this.context = context;
    }

    public DocumentationContext getContext() {
        return context;
    }

    public Map<String, Model> getKnownModels() {
        return knownModels;
    }


    public ModelCache addModel(ApiJsonObject jsonObj) {
        String modelName =jsonObj.name();

        knownModels.put(modelName,
                new Model(modelName,
                        modelName,
                        new TypeResolver().resolve(String.class),
                        "xin.bee.model.entity.BusinessUser",
                        toPropertyMap(jsonObj.value()),
                        "POST参数",
                        "",
                        "",
                        newArrayList(), null, null
                ));
        String resultName = jsonObj.name() + "-" + "result";

        knownModels.put(resultName,
                new Model(resultName,
                        resultName,
                        new TypeResolver().resolve(String.class),
                        "xin.bee.model.entity.BusinessUser",
                        toResultMap(jsonObj.result(), resultName),
                        "返回模型",
                        "",
                        "",
                        newArrayList(), null, null
                ));
        return ModelCacheSub.instance;
    }

    public Map<String, ModelProperty> toResultMap(ApiJsonResult jsonResult, String groupName) {

        List<String> values = Arrays.asList(jsonResult.value());
        List<String> outer = new ArrayList<>();

        if (!getResultTypeOther().equals(jsonResult.type())) {
            outer.add(getJsonErrorCode());
            outer.add(getJsonErrorMsg());
            if (!getResultTypeNormal().equals(jsonResult.type())) {
                //model
                String subModelName = groupName + "-" + jsonResult.name();
                knownModels.put(subModelName,
                        new Model(subModelName,
                                subModelName,
                                new TypeResolver().resolve(String.class),
                                "xin.bee.model.entity.BusinessUser",
                                transResultMap(values),
                                "返回模型",
                                "",
                                "",
                                newArrayList(), null, null
                        ));

                //prop
                Map<String, ModelProperty> propertyMap = new HashMap<>();

//                outer.add(jsonResult.name());
                ResolvedType type = new TypeResolver().resolve(List.class);
                ModelProperty mp = new ModelProperty(
                        jsonResult.name(),
                        type,
                        "",
                        0,
                        false,
                        false,
                        true,
                        false,
                        "",
                        null,
                        "",
                        null,
                        "",
                        null,
                        newArrayList()
                );// new AllowableRangeValues("1", "2000"),//.allowableValues(new AllowableListValues(["ABC", "ONE", "TWO"], "string"))
                mp.updateModelRef(getModelRef());
                ResolvedType collectionElementType = collectionElementType(type);
                try {
                    Field f = ModelProperty.class.getDeclaredField("modelRef");
                    f.setAccessible(true);
                    f.set(mp, new ModelRef("List",new ModelRef(subModelName)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("0000000000");
                System.out.println(mp.getModelRef().getType());
                propertyMap.put(jsonResult.name(), mp);

                if (getResultTypePage().equals(jsonResult.type())) {
                    outer.add(getJsonStartPageNum());
                    outer.add(getJsonPageSize());
                    outer.add(getJsonTotalCount());
                }


                propertyMap.putAll(transResultMap(outer));
                return propertyMap;
            }

            outer.addAll(values);
            return transResultMap(outer);
        }

        return transResultMap(values);
    }

    public Map<String, ModelProperty> transResultMap(List<String> values) {
        Map<String, ModelProperty> propertyMap = new HashMap<>();
        for (String resultName : values) {
            ApiSingleParam param = paramMap.get(resultName);
            if (isEmpty(param)) {
                continue;
            }
            Class<?> type = param.type();
            if (!isEmpty(param)) {
                type = param.type();
            } else if (isEmpty(type)) {
                type = String.class;
            }

            boolean allowMultiple = param.allowMultiple();
            if (!isEmpty(param)) {
                allowMultiple = param.allowMultiple();
            }
            ResolvedType resolvedType = null;
            if (allowMultiple) {
                resolvedType = new TypeResolver().resolve(List.class, type);
            } else {
                resolvedType = new TypeResolver().resolve(type);
            }
            ModelProperty mp = new ModelProperty(
                    resultName,
                    resolvedType,
                    param.type().getName(),
                    0,
                    false,
                    false,
                    true,
                    false,
                    param.value(),
                    null,
                    param.example(),
                    null,
                    "",
                    null,
                    newArrayList()
            );// new AllowableRangeValues("1", "2000"),//.allowableValues(new AllowableListValues(["ABC", "ONE", "TWO"], "string"))
            mp.updateModelRef(getModelRef());
            propertyMap.put(resultName, mp);
        }

        return propertyMap;
    }

    public Map<String, ModelProperty> toPropertyMap(ApiJsonProperty[] jsonProp) {
        Map<String, ModelProperty> propertyMap = new HashMap<String, ModelProperty>();

        for (ApiJsonProperty property : jsonProp) {
            String propertyName = property.name();
            ApiSingleParam param = paramMap.get(propertyName);

            String description = property.description();
            if (isNullOrEmpty(description) && !isEmpty(param)) {
                description = param.value();
            }
            String example = property.description();
            if (isNullOrEmpty(example) && !isEmpty(param)) {
                example = param.example();
            }
            Class<?> type = property.type();
            if (!isEmpty(param)) {
                type = param.type();
            } else if (isEmpty(type)) {
                type = String.class;
            }

            boolean allowMultiple = property.allowMultiple();
            if (!isEmpty(param)) {
                allowMultiple = param.allowMultiple();
            }
            ResolvedType resolvedType = null;
            if (allowMultiple) {
                resolvedType = new TypeResolver().resolve(List.class, type);
            } else {
                resolvedType = new TypeResolver().resolve(type);
            }

            ModelProperty mp = new ModelProperty(
                    propertyName,
                    resolvedType,
                    type.toString(),
                    0,
                    property.required(),
                    false,
                    property.readOnly(),
                    property.allowEmptyValue(),
                    description,
                    null,
                    example,
                    null,
                    property.defaultValue(),
                    null,
                    newArrayList()
            );// new AllowableRangeValues("1", "2000"),//.allowableValues(new AllowableListValues(["ABC", "ONE", "TWO"], "string"))
            mp.updateModelRef(getModelRef());
            propertyMap.put(property.name(), mp);
        }

        return propertyMap;
    }


    private static class ModelCacheSub {
        private static ModelCache instance = new ModelCache();
    }

    private Function<ResolvedType, ? extends ModelReference> getModelRef() {
        Function<ResolvedType, ? extends ModelReference> factory = getFactory();
//        ModelReference stringModel = factory.apply(typeResolver.resolve(List.class, String.class));
        return factory;

    }


    public Function<ResolvedType, ? extends ModelReference> getFactory() {
        if (factory == null) {

            List<DefaultTypeNameProvider> providers = newArrayList();
            providers.add(new DefaultTypeNameProvider());
            PluginRegistry<TypeNameProviderPlugin, DocumentationType> modelNameRegistry =
                    OrderAwarePluginRegistry.create(providers);
            TypeNameExtractor typeNameExtractor = new TypeNameExtractor(
                    typeResolver,
                    modelNameRegistry,
                    new JacksonEnumTypeDeterminer());
            ModelContext modelContext = inputParam(
                    context.getGroupName(),
                    String.class,
                    context.getDocumentationType(),
                    context.getAlternateTypeProvider(),
                    context.getGenericsNamingStrategy(),
                    context.getIgnorableParameterTypes());
            factory = ResolvedTypes.modelRefFactory(modelContext, typeNameExtractor);
        }
        return factory;
    }
}
