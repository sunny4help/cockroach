package com.zhangyingwei.cockroach.http.client;

import com.zhangyingwei.cockroach.executer.Task;
import com.zhangyingwei.cockroach.executer.TaskResponse;
import com.zhangyingwei.cockroach.http.HttpProxy;

import java.util.Map;

/**
 * Created by zhangyw on 2017/8/10.
 */
public interface HttpClient {
    HttpClient setProxy(HttpProxy proxy);

    TaskResponse doGet(Task task) throws Exception;

    HttpClient proxy();

    TaskResponse doPost(Task task) throws Exception;

    HttpClient setCookie(String cookie);

    HttpClient setHttpHeader(Map<String, String> httpHeader);
}