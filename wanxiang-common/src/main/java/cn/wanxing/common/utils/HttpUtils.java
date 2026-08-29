package cn.wanxing.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 出站 HTTP 工具（Apache HttpClient 4）。
 *
 * <p>每次调用统一记录请求/结果日志（方法、URL、状态码、耗时），失败时记 ERROR 带堆栈。
 * 响应体是流，此处不读取（会破坏调用方消费），只记状态码。
 * 注意：当前工程暂无调用方，作为短信网关等外呼接入的预留基建。
 */
@Slf4j
public class HttpUtils {

    /** 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** 等待连接池/读超时（毫秒）。原实现无超时，外呼对端无响应会一直挂起业务线程 */
    private static final int SOCKET_TIMEOUT_MS = 10_000;

    /**
     * get
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static HttpResponse doGet(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpGet request = new HttpGet(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return execute(httpClient, request, url);
    }

    /**
     * post form
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param bodys
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys,
        Map<String, String> bodys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpPost request = new HttpPost(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (bodys != null) {
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

            for (String key : bodys.keySet()) {
                nameValuePairList.add(new BasicNameValuePair(key, bodys.get(key)));
            }

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(nameValuePairList, "utf-8");
            formEntity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            request.setEntity(formEntity);
        }

        return execute(httpClient, request, url);
    }

    /**
     * Post String
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys,
        String body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpPost request = new HttpPost(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return execute(httpClient, request, url);
    }

    /**
     * Post stream
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPost(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys,
        byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpPost request = new HttpPost(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return execute(httpClient, request, url);
    }

    /**
     * Put String
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys,
        String body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpPut request = new HttpPut(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (StringUtils.isNotBlank(body)) {
            request.setEntity(new StringEntity(body, "utf-8"));
        }

        return execute(httpClient, request, url);
    }

    /**
     * Put stream
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @param body
     * @return
     * @throws Exception
     */
    public static HttpResponse doPut(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys,
        byte[] body)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpPut request = new HttpPut(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        if (body != null) {
            request.setEntity(new ByteArrayEntity(body));
        }

        return execute(httpClient, request, url);
    }

    /**
     * Delete
     *
     * @param host
     * @param path
     * @param method
     * @param headers
     * @param querys
     * @return
     * @throws Exception
     */
    public static HttpResponse doDelete(String host, String path, String method,
        Map<String, String> headers,
        Map<String, String> querys)
        throws Exception {
        HttpClient httpClient = wrapClient(host);

        String url = buildUrl(host, path, querys);
        HttpDelete request = new HttpDelete(url);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.addHeader(e.getKey(), e.getValue());
        }

        return execute(httpClient, request, url);
    }

    /**
     * 统一执行出口：记录方法、URL、状态码、耗时，失败带堆栈
     */
    private static HttpResponse execute(HttpClient httpClient, HttpUriRequest request, String url)
            throws Exception {
        long start = System.currentTimeMillis();
        try {
            HttpResponse response = httpClient.execute(request);
            log.info("[HTTP-OUT] {} {} status={} cost={}ms",
                    request.getMethod(), url, response.getStatusLine().getStatusCode(),
                    System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("[HTTP-OUT] {} {} 调用失败 cost={}ms error={}",
                    request.getMethod(), url, System.currentTimeMillis() - start, e.getMessage(), e);
            throw e;
        }
    }

    private static String buildUrl(String host, String path, Map<String, String> querys) throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(host);
        if (!StringUtils.isBlank(path)) {
            sbUrl.append(path);
        }
        if (null != querys) {
            StringBuilder sbQuery = new StringBuilder();
            for (Map.Entry<String, String> query : querys.entrySet()) {
                if (0 < sbQuery.length()) {
                    sbQuery.append("&");
                }
                if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
                    sbQuery.append(query.getValue());
                }
                if (!StringUtils.isBlank(query.getKey())) {
                    sbQuery.append(query.getKey());
                    if (!StringUtils.isBlank(query.getValue())) {
                        sbQuery.append("=");
                        sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
                    }
                }
            }
            if (0 < sbQuery.length()) {
                sbUrl.append("?").append(sbQuery);
            }
        }

        return sbUrl.toString();
    }

    private static HttpClient wrapClient(String host) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}
