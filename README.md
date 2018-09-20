# swagger-mode 
springfox + swagger2自定义JsonObject/Map参数文档
## 说明
该改动不影响swagger原来的使用，Object/JsonObject 都可以兼容

## Controller
![image.png](https://upload-images.jianshu.io/upload_images/8509808-124ebefe715fb465.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
## Model
![image.png](https://upload-images.jianshu.io/upload_images/8509808-93341aafc580ab6e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## 最终结果
request：
![image.png](https://upload-images.jianshu.io/upload_images/8509808-ba093e202b8a97d5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
request model:
![image.png](https://upload-images.jianshu.io/upload_images/8509808-c592a94ab18c5e05.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

response：
![image.png](https://upload-images.jianshu.io/upload_images/8509808-53c7d92a3e481401.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
response model:
![image.png](https://upload-images.jianshu.io/upload_images/8509808-2bd1f20bc4092fb0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)



## 代码实现
首先根据官方文档，写一个OperationBuilderPlugin类型的插件，这个插件用来读取接口的参数
```
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ParametersReader implements OperationBuilderPlugin {
...
 @Override
 public void apply(OperationContext context) {
        context.operationBuilder().parameters(readParameters(context));
 }
 private List<Parameter> readParameters(OperationContext context) {
        List<Parameter> parameters = Lists.newArrayList();
        List<ResolvedMethodParameter> methodParameters = context.getParameters();

        //1. 先读取GlobalString类中我们定义的参数单元，用一个Map来保存
        Map<String, ApiSingleParam> paramMap = new HashMap<>();
        Field[] fields = GlobalString.class.getDeclaredFields();
        String type = new String();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ApiSingleParam.class)) {
                ApiSingleParam param = field.getAnnotation(ApiSingleParam.class);
                try {
                    String name = (String) field.get(type);
                    paramMap.put(name, param);
                } catch (Exception e) {
                }
            }
        }

        //遍历controller中的方法
        for (ResolvedMethodParameter methodParameter : methodParameters) {
            ParameterContext parameterContext = new ParameterContext(methodParameter,
                    new ParameterBuilder(),
                    context.getDocumentationContext(),
                    context.getGenericsNamingStrategy(),
                    context);
            Function<ResolvedType, ? extends ModelReference> factory = createModelRefFactory(parameterContext);

            //读取自定义的注释类
            Optional<ApiJsonObject> annotation = context.findAnnotation(ApiJsonObject.class);

            if (annotation.isPresent()) {
                //2. 自定义的注释类里包含参数列表，我们把它合成一个请求Model和应答Model，放在ModelCache缓存里面
                ModelCache.getInstance().setFactory(factory)
                        .setParamMap(paramMap)
                        .addModel(annotation.get());
            }
        }
        return parameters;
    }
}
```

然后重写 ApiListingScanner 类,将我们的Model加入到swagger的Model列表中
```
@Component
@Primary
public class ApiListingPluginsScanner extends ApiListingScanner {
...
    public Multimap<String, ApiListing> scan(ApiListingScanningContext context) {
         final Multimap<String, ApiListing> apiListingMap = LinkedListMultimap.create();
        int position = 0;
        Map<ResourceGroup, List<RequestMappingContext>> requestMappingsByResourceGroup
                = context.getRequestMappingsByResourceGroup();
        Collection<ApiDescription> additionalListings = pluginsManager.additionalListings(context);
        Set<ResourceGroup> allResourceGroups = FluentIterable.from(collectResourceGroups(additionalListings))
                .append(requestMappingsByResourceGroup.keySet())
                .toSet();

        List<SecurityReference> securityReferences = newArrayList();
        for (final ResourceGroup resourceGroup : sortedByName(allResourceGroups)) {

            DocumentationContext documentationContext = context.getDocumentationContext();
            Set<String> produces = new LinkedHashSet<String>(documentationContext.getProduces());
            Set<String> consumes = new LinkedHashSet<String>(documentationContext.getConsumes());
            String host = documentationContext.getHost();
            Set<String> protocols = new LinkedHashSet<String>(documentationContext.getProtocols());
            Set<ApiDescription> apiDescriptions = newHashSet();

            Map<String, Model> models = new LinkedHashMap<String, Model>();
            List<RequestMappingContext> requestMappings = nullToEmptyList(requestMappingsByResourceGroup.get(resourceGroup));

            for (RequestMappingContext each : sortedByMethods(requestMappings)) {//url
                Map<String, Model> knownModels = new HashMap<>();
                models.putAll(apiModelReader.read(each.withKnownModels(models)));
                apiDescriptions.addAll(apiDescriptionReader.read(each));
            }
            //加入自己的Model
            models.putAll(ModelCache.getInstance().getKnownModels());

            List<ApiDescription> additional = from(additionalListings)
                    .filter(and(
                                    belongsTo(resourceGroup.getGroupName()),
                                    onlySelectedApis(documentationContext)))
                    .toList();
            apiDescriptions.addAll(additional);

            List<ApiDescription> sortedApis = FluentIterable.from(apiDescriptions)
                    .toSortedList(documentationContext.getApiDescriptionOrdering());
            Optional<String> o = longestCommonPath(sortedApis);
            String resourcePath = new ResourcePathProvider(resourceGroup)
                    .resourcePath()
                    .or(o)
                    .orNull();

            PathProvider pathProvider = documentationContext.getPathProvider();
            String basePath = pathProvider.getApplicationBasePath();
            PathAdjuster adjuster = new PathMappingAdjuster(documentationContext);
            ApiListingBuilder apiListingBuilder = new ApiListingBuilder(context.apiDescriptionOrdering())
                    .apiVersion(documentationContext.getApiInfo().getVersion())
                    .basePath(adjuster.adjustedPath(basePath))
                    .resourcePath(resourcePath)
                    .produces(produces)
                    .consumes(consumes)
                    .host(host)
                    .protocols(protocols)
                    .securityReferences(securityReferences)
                    .apis(sortedApis)
                    .models(models)
                    .position(position++)
                    .availableTags(documentationContext.getTags());

            ApiListingContext apiListingContext = new ApiListingContext(
                    context.getDocumentationType(),
                    resourceGroup,
                    apiListingBuilder);
            apiListingMap.put(resourceGroup.getGroupName(), pluginsManager.apiListing(apiListingContext));
        }
        return apiListingMap;
    }
}
```

这样request的部分就完成了，下面是response的实现

先重写 SwaggerResponseMessageReader 类
```
@Primary
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 3)//一定要大一点
public class ResponseMessageReader extends SwaggerResponseMessageReader {
    
    private final TypeNameExtractor typeNameExtractor;
    private final TypeResolver typeResolver;

    public ResponseMessageReader(TypeNameExtractor typeNameExtractor, TypeResolver typeResolver) {
        super(typeNameExtractor, typeResolver);
        this.typeNameExtractor = typeNameExtractor;
        this.typeResolver = typeResolver;
    }

    @Override
    protected Set<ResponseMessage> read(OperationContext context) {
        ResolvedType defaultResponse = context.getReturnType();
        Optional<ApiOperation> operationAnnotation = context.findAnnotation(ApiOperation.class);
        Optional<ResolvedType> operationResponse =
                operationAnnotation.transform(resolvedTypeFromOperation(typeResolver, defaultResponse));
        Optional<ResponseHeader[]> defaultResponseHeaders = operationAnnotation.transform(responseHeaders());
        Map<String, Header> defaultHeaders = newHashMap();
        if (defaultResponseHeaders.isPresent()) {
            defaultHeaders.putAll(headers(defaultResponseHeaders.get()));
        }

        List<ApiResponses> allApiResponses = context.findAllAnnotations(ApiResponses.class);
        Set<ResponseMessage> responseMessages = newHashSet();

        Map<Integer, ApiResponse> seenResponsesByCode = newHashMap();
        for (ApiResponses apiResponses : allApiResponses) {
            ApiResponse[] apiResponseAnnotations = apiResponses.value();
            for (ApiResponse apiResponse : apiResponseAnnotations) {

                if (!seenResponsesByCode.containsKey(apiResponse.code())) {
                    seenResponsesByCode.put(apiResponse.code(), apiResponse);

                    java.util.Optional<ModelReference> responseModel = java.util.Optional.empty();
                    java.util.Optional<ResolvedType> type = resolvedType(null, apiResponse);
                    if (isSuccessful(apiResponse.code())) {
                        type = java.util.Optional.ofNullable(type.orElseGet(operationResponse::get));
                    }
                    if (type.isPresent()) {
                        //将返回的模型ID修改成自定义的，这里我取@apiResponse中的reference参数加"-result"组合
                        responseModel = java.util.Optional.of(new ModelRef(apiResponse.reference()+"-result"));
                    }
                    Map<String, Header> headers = newHashMap(defaultHeaders);
                    headers.putAll(headers(apiResponse.responseHeaders()));

                    responseMessages.add(new ResponseMessageBuilder()
                            .code(apiResponse.code())
                            .message(apiResponse.message())
                            .responseModel(responseModel.orElse(null))
                            .headersWithDescription(headers)
                            .build());
                }
            }
        }
        if (operationResponse.isPresent()) {
            ModelContext modelContext = returnValue(
                    context.getGroupName(),
                    operationResponse.get(),
                    context.getDocumentationType(),
                    context.getAlternateTypeProvider(),
                    context.getGenericsNamingStrategy(),
                    context.getIgnorableParameterTypes());
            ResolvedType resolvedType = context.alternateFor(operationResponse.get());

            ModelReference responseModel = modelRefFactory(modelContext, typeNameExtractor).apply(resolvedType);
            context.operationBuilder().responseModel(responseModel);
            ResponseMessage defaultMessage = new ResponseMessageBuilder()
                    .code(httpStatusCode(context))
                    .message(message(context))
                    .responseModel(responseModel)
                    .build();
            if (!responseMessages.contains(defaultMessage) && !"void".equals(responseModel.getType())) {
                responseMessages.add(defaultMessage);
            }
        }

        return responseMessages;
    }
    ...
}

```

ModelCache生成Model
```

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

```
ModelProperty的制作
```
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
```

这样就搞定了！！！

## Map
map的话比较简单，参考这篇
https://blog.csdn.net/hellopeng1/article/details/82227942

git和jar稍后发上来

