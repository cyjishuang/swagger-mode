package com.yueh.swagger.handler;

import com.yueh.swagger.ModelCache;
import com.yueh.swagger.model.ApiJsonModel;
import com.yueh.swagger.model.ApiSingleParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.DefaultTypeNameProvider;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * Created by yueh on 2018/9/18.
 */
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 3)
public class ApiModelJsonProvider extends DefaultTypeNameProvider {
    @Override
    public String nameFor(Class<?> type) {
        ApiJsonModel annotation = findAnnotation(type, ApiJsonModel.class);
        String defaultTypeName = super.nameFor(type);
        if (annotation != null) {
            Map<String, ApiSingleParam> paramMap = new HashMap<>();
            Field[] fields = type.getDeclaredFields();
            String typeName = new String();
            for (Field field : fields) {
                if (field.isAnnotationPresent(ApiSingleParam.class)) {
                    ApiSingleParam param = field.getAnnotation(ApiSingleParam.class);
                    try {
                        String name = (String) field.get(typeName);
                        paramMap.put(name, param);
                    } catch (Exception e) {
                    }
                }
            }

            ModelCache.getInstance().setParamMap(paramMap);

            return fromNullable(emptyToNull(annotation.value())).or(defaultTypeName);
        }
        return defaultTypeName;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
