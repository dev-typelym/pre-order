package com.app.preorder.orderservice.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class FeignOkHttpConfig {

    @Bean
    public okhttp3.OkHttpClient okHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::info);
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .addInterceptor(logging)
                .build();
    }

    @Primary
    @Bean
    public Client feignClient(LoadBalancerClient lbClient,
                              okhttp3.OkHttpClient okHttp,
                              LoadBalancerClientFactory lbFactory) { // ★ 여기도 Factory
        log.info("[Feign] Forcing OkHttp + FeignBlockingLoadBalancerClient (Factory-ctor)");
        Client delegate = new OkHttpClient(okHttp);
        return new FeignBlockingLoadBalancerClient(delegate, lbClient, lbFactory);
    }
}