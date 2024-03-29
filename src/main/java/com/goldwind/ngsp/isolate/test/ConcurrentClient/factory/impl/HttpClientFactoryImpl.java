package com.goldwind.ngsp.isolate.test.ConcurrentClient.factory.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.goldwind.ngsp.isolate.test.ConcurrentClient.factory.AbstractClientFactory;
import com.goldwind.ngsp.isolate.test.ConcurrentClient.util.DataUtil;
import com.goldwind.ngsp.isolate.test.ConcurrentClient.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.goldwind.ngsp.isolate.test.ConcurrentClient.consts.ConcurrentClientConst.HTTP_CLIENT_URL;

@Component
@Slf4j
public class HttpClientFactoryImpl extends AbstractClientFactory {

    @Autowired
    private KafkaUtil kafkaUtil;

    @Autowired
    private DataUtil dataUtil;

    private final List<OkHttpClient> httpClientList = new ArrayList<>();

    @Override
    protected void createClient(String proxyIP, int proxyPort) {
        Proxy socks5Proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyIP, proxyPort));
        OkHttpClient httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10 * 60 * 1000, TimeUnit.MILLISECONDS)
                .readTimeout(10 * 60 * 1000, TimeUnit.MILLISECONDS)
                .writeTimeout(10 * 60 * 1000, TimeUnit.MILLISECONDS)
                .proxy(socks5Proxy)
                .build();
        httpClientList.add(httpClient);
    }

    @Override
    public void sendMsg() throws Exception {
        initializeClientFactory();
        for (OkHttpClient httpClient : httpClientList) {
            executorService.submit(() -> {
                for (; ; ) {
                    byte[] bytes = dataUtil.getMsg();
                    if (log.isDebugEnabled()) {
                        log.debug(Thread.currentThread().getName() + " 发送数据: " + Arrays.toString(bytes));
                    }
                    kafkaUtil.send(bytes);
                    Request request = new Request.Builder()
                            .url(String.format(HTTP_CLIENT_URL, clientConfig.getAppIP(), clientConfig.getAppPort()))
                            .post(RequestBody.create(MediaType.parse("text/plain"), bytes))
                            .build();
                    httpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            log.error(e.getMessage(), e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws JsonProcessingException {
                            if (log.isDebugEnabled()) {
                                log.debug(Thread.currentThread().getName() + " 接收数据: " + Arrays.toString(bytes));
                            }
                            kafkaUtil.send(bytes);
                        }
                    });
                }
            });
        }
    }

}
