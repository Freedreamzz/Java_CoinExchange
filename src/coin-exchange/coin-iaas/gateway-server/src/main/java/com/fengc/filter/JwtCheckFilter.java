package com.fengc.filter;

import com.alibaba.fastjson.JSONObject;
import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Set;

/**
 * @author lfc
 */
@Component
public class JwtCheckFilter implements GlobalFilter, Ordered {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //@Value("${no.require.urls:/admin/login}")
    @Value("${no.require.urls:/admin/login,/user/gt/register,/user/login,/user/users/register,/user/sms/sendTo,/user/users/setPassword}")
    private Set<String> noRequireTokenUris;

    /**
     * 过滤器拦截到用户的请求，校验jwt在redis中是否存在
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * 1.判断该请求是否需要进行权限校验
         * 2.去除用户的token
         * 3.判断token是否有效
         */
        if (!isCheckRequstToken(exchange)) {
            //不需要token直接放行
            return chain.filter(exchange);
        }
        String token = getUserToken(exchange);
        if (StringUtils.isEmpty(token)) {
            return buildeNoAuthorizationResult(exchange);
        }
        Boolean hasKey = stringRedisTemplate.hasKey(token);
        if (null != hasKey && hasKey) {
            return chain.filter(exchange);
        }
        return buildeNoAuthorizationResult(exchange);
    }

    /**
     * 给用户响应一个没有token的错误
     *
     * @return
     */
    private Mono<Void> buildeNoAuthorizationResult(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error", "NoAuthorization");
        jsonObject.put("errorMsg", "Token is NUll or Error");
        DataBuffer wrap = response.bufferFactory().wrap(jsonObject.toJSONString().getBytes());
        return response.writeWith(Flux.just(wrap));
    }


    /**
     * 获取用户token信息
     * 一般放在用户的请求头中
     *
     * @param exchange
     * @return
     */
    private String getUserToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return token == null ? null : token.replace("bearer ", "");
    }

    /**
     * 判断请求是否需要授权访问
     *
     * @param exchange
     * @return
     */
    private boolean isCheckRequstToken(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        //当前的path路径和 允许放行的path集合进行比较
        if (noRequireTokenUris.contains(path)) {
            return false;
        }
        return true;
    }

    /**
     * 拦截器的顺序
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
