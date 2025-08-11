package org.lyc122.dev.tools.SparkAPI;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The Service class which provides a connection to <i>Xunfei Xinghuo</i> AI API using WSS
 * @author Li Yichen
 * @see <a href=https://www.xfyun.cn/doc/platform/xfyunreadme.html>Spark API doc</a>
 * @see <a href=https://global.xfyun.cn/>Spark API English page</a>
 *
 */
public class SparkService {
    private final String apiKey;
    private final String apiSecret;
    private final String appId;
    private final String hostUrl;
    private final String domain;
    private final float temperature;

    private final List<RoleContent> historyList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient.Builder().build();

    private WebSocket webSocket;
    private String totalAnswer;
    private CountDownLatch responseLatch;
    private final PrintWriter out;
    private final boolean streaming_output;
    public int TotalPacketRecieved = 0;

    public SparkService(String apiKey, String apiSecret, String appId, String endPoint, String domain, PrintWriter OutputWriter, boolean _stream){
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.appId = appId;
        this.hostUrl = endPoint;
        this.domain = domain;
        this.out = OutputWriter;
        this.streaming_output = _stream;
        this.temperature = 0.5F;
    }

    /**
     * @param apiKey the apikey in spark console
     * @param apiSecret the apisecret in spark console
     * @param appId the appid in spark console
     * @param modelType the model type you use
     * @param OutputWriter Optional. The output will be written to this writer
     * @param _stream Optional.
     *                </br>
     *                <b>true</b> if enable streaming output.
     *                </br>
     *                <b>false</b> if disable streaming output.
     */
    public SparkService(String apiKey, String apiSecret, String appId, ModelType modelType, PrintWriter OutputWriter, boolean _stream, float temperature){
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.appId = appId;
        this.hostUrl = modelType.getEndpoint();
        this.domain = modelType.getModel();
        this.out = OutputWriter;
        this.streaming_output = _stream;
        this.temperature = temperature;
    }
    /**
     * 向大模型发送消息并获取响应
     *
     * @param question 用户问题
     * @return 大模型响应内容
     * @throws Exception 通信异常
     */
    public String sendMessage(String question) throws Exception {
        responseLatch = new CountDownLatch(1);
        totalAnswer = "";

        // 构建鉴权URL并建立WebSocket连接
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, new SparkWebSocketListener(question));

        // 等待响应完成 (超时15秒)
        if (!responseLatch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Response timeout");
        }

        return totalAnswer;
    }

    /**
     * 清理对话历史
     */
    public void clearHistory() {
        historyList.clear();
    }

    /**
     * 获取当前对话历史
     */
    public List<RoleContent> getHistory() {
        return Collections.unmodifiableList(historyList);
    }

    public WebSocket getWebSocket() {
        return webSocket;
    }

    // WebSocket事件监听器
    private class SparkWebSocketListener extends WebSocketListener {
        private final String question;
        private boolean wsCloseFlag = false;

        public SparkWebSocketListener(String question) {
            this.question = question;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            // 构造并发送请求消息
            String requestJson = buildRequestJson(question);
            webSocket.send(requestJson);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            JsonParse jsonParse = JSON.parseObject(text, JsonParse.class);
            TotalPacketRecieved ++;
            // 处理错误响应
            if (jsonParse.header.code != 0) {
                System.err.println("Error occurred: " + jsonParse.header.message);
                responseLatch.countDown();
                return;
            }
            if(out != null && streaming_output) {
                for(Text t : jsonParse.payload.choices.text){
                    out.print(t.content);
                }
                out.flush();
            }
            // 拼接响应内容
            for (Text t : jsonParse.payload.choices.text) {
                totalAnswer += t.content;
            }

            // 检查响应是否结束
            if (jsonParse.header.status == 2) {
                // 将响应添加到历史记录
                if(out != null) out.println(totalAnswer);
                RoleContent responseContent = new RoleContent();
                responseContent.role = "assistant";
                responseContent.content = totalAnswer;
                historyList.add(responseContent);

                wsCloseFlag = true;
                responseLatch.countDown();
            }
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            if (!wsCloseFlag) {
                responseLatch.countDown();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
            if (response != null) {
                try {
                    if (response.body() != null) {
                        System.err.println("WebSocket failure: " + response.body().string());
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
            responseLatch.countDown();
        }



        /**
         * 构建请求JSON
         */
        private String buildRequestJson(String question) {
            JSONObject requestJson = new JSONObject();

            // Header
            JSONObject header = new JSONObject();
            header.put("app_id", appId);
            header.put("uid", UUID.randomUUID().toString().substring(0, 10));

            // Parameter
            JSONObject parameter = new JSONObject();
            JSONObject chat = new JSONObject();
            chat.put("domain", domain);
            chat.put("temperature", temperature);
            chat.put("max_tokens", 4096);
            parameter.put("chat", chat);

            // Payload
            JSONObject payload = new JSONObject();
            JSONObject message = new JSONObject();
            List<Object> textList = new ArrayList<>();

            // 添加历史对话
            for (RoleContent content : historyList) {
                textList.add(JSON.toJSON(content));
            }

            // 添加当前问题
            RoleContent questionContent = new RoleContent();
            questionContent.role = "user";
            questionContent.content = question;
            textList.add(JSON.toJSON(questionContent));

            // 将当前问题添加到历史记录
            historyList.add(questionContent);

            message.put("text", textList);
            payload.put("message", message);

            // 组装完整请求
            requestJson.put("header", header);
            requestJson.put("parameter", parameter);
            requestJson.put("payload", payload);

            return requestJson.toString();
        }
    }

    /**
     * 生成鉴权URL
     */
    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URI(hostUrl).toURL();
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 构建签名内容
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        // 计算HMAC-SHA256签名
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        // 构建Authorization头
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha
        );

        // 构建最终URL
        return Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath()))
                .newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build()
                .toString();
    }

    // 数据结构定义（内部类）
    private static class JsonParse {
        public Header header;
        public Payload payload;
    }

    private static class Header {
        public int code;
        public int status;
        public String sid;
        public String message;
    }

    private static class Payload {
        public Choices choices;
    }

    private static class Choices {
        public List<Text> text;
    }

    private static class Text {
        public String role;
        public String content;
    }

    public static class RoleContent {
        public String role;
        public String content;
    }
}