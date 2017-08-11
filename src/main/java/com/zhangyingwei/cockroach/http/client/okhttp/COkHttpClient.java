package com.zhangyingwei.cockroach.http.client.okhttp;

import com.zhangyingwei.cockroach.executer.Task;
import com.zhangyingwei.cockroach.executer.TaskResponse;
import com.zhangyingwei.cockroach.http.HttpParams;
import com.zhangyingwei.cockroach.http.HttpProxy;
import com.zhangyingwei.cockroach.http.ProxyTuple;
import com.zhangyingwei.cockroach.http.client.HttpClient;
import net.sf.json.JSONObject;
import okhttp3.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by zhangyw on 2017/8/10.
 */
public class COkHttpClient implements HttpClient {
    private OkHttpClient okHttpClient;
    private HttpProxy proxy;
    private ProxyTuple proxyTuple;
    private Integer reTryTime = 5;
    private String cookie;
    private Map<String, String> httpHeader;

    public COkHttpClient() {
        this.okHttpClient = new OkHttpClient.Builder().cookieJar(new CookieManager()).build();
    }

    @Override
    public HttpClient setProxy(HttpProxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Override
    public TaskResponse doGet(Task task) throws Exception {
        String params = String.join("&", task.getParams().entrySet().stream()
                .map(entity -> entity.getKey() + "=" + entity.getValue())
                .collect(Collectors.toList()));
        Request request = new Request.Builder()
                .url(String.format("%s?%s",task.getUrl(),""))
                .headers(Headers.of(HttpParams.headers()))
                .headers(Headers.of(this.httpHeader))
                .get()
                .build();
        Response response = this.okHttpClient.newCall(request).execute();
        if(!response.isSuccessful()){
            System.out.println("INFO: 服务端错误 - "+response.body().string());
            reTryTime--;
            if (reTryTime > 0) {
                System.out.println("INFO: resty - " +task);
                if(this.proxy != null){
                    this.proxy.disable(this.proxyTuple);
                    this.proxy();
                }
                return this.doGet(task);
            }
        } else if(response.isRedirect()){
            System.out.println("INFO: 重定向");
        }
        return TaskResponse.of(response.body().string(),task);
    }

    @Override
    public HttpClient proxy() {
        if(this.proxy != null){
            this.proxyTuple = this.proxy.randomProxy();
            this.okHttpClient = this.okHttpClient.newBuilder()
                    .cookieJar(new CookieManager(this.cookie))
                    .proxy(
                            new Proxy(
                                    Proxy.Type.HTTP,
                                    new InetSocketAddress(this.proxyTuple.ip(),this.proxyTuple.port())
                            )
                    ).build();
            System.out.println("代理:"+this.proxyTuple);
        }
        return this;
    }

    @Override
    public TaskResponse doPost(Task task) throws Exception {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                JSONObject.fromObject(task.getParams()).toString()
        );
        Request request = new Request.Builder()
                .url(task.getUrl())
                .headers(Headers.of(HttpParams.headers()))
                .headers(Headers.of(this.httpHeader))
                .post(requestBody)
                .build();
        Response response = this.okHttpClient.newCall(request).execute();
        if(response.isSuccessful()){
            System.out.println("INFO: 服务端错误");
        } else if(response.isRedirect()){
            System.out.println("INFO: 重定向");
        }
        return TaskResponse.of(response.message(),task);
    }

    @Override
    public HttpClient setCookie(String cookie) {
        this.cookie = cookie;
        this.okHttpClient = new OkHttpClient.Builder().cookieJar(new CookieManager(this.cookie)).build();
        return this;
    }

    @Override
    public HttpClient setHttpHeader(Map<String, String> httpHeader) {
        this.httpHeader = httpHeader;
        return this;
    }
}