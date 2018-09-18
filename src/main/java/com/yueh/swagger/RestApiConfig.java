package com.yueh.swagger;

import com.google.common.collect.Sets;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by yueh on 2018/9/7.
 */
@EnableWebMvc

@EnableSwagger2

@Configuration

@ComponentScan(basePackages ="xin.bee.controller")

public class RestApiConfig extends WebMvcConfigurationSupport {

    @Bean
    public Docket createRestApi() {

        return new Docket(DocumentationType.SWAGGER_2)

                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false)

//                .host("localhost:8081/v2/api-docs")

                .protocols(Sets.newHashSet("https","http"))

                //.pathMapping("/")

                .select()

                //只生成被Api这个注解注解过的类接口            //.apis(RequestHandlerSelectors.withClassAnnotation(Api.class))

                //只生成被ApiOperation这个注解注解过的api接口


                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))

                //生成所有API接口             //.apis(RequestHandlerSelectors.basePackage("com.hw.one.core.controller"))

                .paths(PathSelectors.any())

                .build();

    }



    private ApiInfo apiInfo() {

        return new ApiInfoBuilder()

                .title("吉富量用户系统API")

                .description("吉富量用户系统在线API文档，主要提供B端用户及其权限的所有功能实现接口。")

// .license("稳定版")

//             .termsOfServiceUrl("http://localhost:8080/dist/index.html")

// .contact(new Contact("ONE基础平台","http://192.168.15.246:8025/#/login","scsoft@163.com"))

                .version("1.0.0")

                .build();

    }

}