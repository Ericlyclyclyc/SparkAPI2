package org.lyc122.dev.tools.SparkAPI;

/*

 各版本的hostUrl及其对应的domian参数，具体可以参考接口文档 https://www.xfyun.cn/doc/spark/Web.html
  Spark Lite      https://spark-api.xf-yun.com/v1.1/chat      domain参数为lite
  Spark Pro       https://spark-api.xf-yun.com/v3.1/chat      domain参数为generalv3
  Spark Pro-128K  https://spark-api.xf-yun.com/chat/pro-128k  domain参数为pro-128k
  Spark Max       https://spark-api.xf-yun.com/v3.5/chat      domain参数为generalv3.5
  Spark Max-32K   https://spark-api.xf-yun.com/chat/max-32k   domain参数为max-32k
  Spark4.0 Ultra  https://spark-api.xf-yun.com/v4.0/chat      domain参数为4.0Ultra

 */

public enum ModelType {
    LITE("lite", "https://spark-api.xf-yun.com/v1.1/chat"),
    PRO("generalv3","https://spark-api.xf-yun.com/v3.1/chat"),
    PRO_128K("pro-128k", "ttps://spark-api.xf-yun.com/chat/pro-128k"),
    MAX("max-32k","https://spark-api.xf-yun.com/chat/max-32k"),
    ULTRA_40("4.0Ultra","https://spark-api.xf-yun.com/v4.0/chat");

    private final String model;
    private final String endpoint;

    ModelType(String model, String endpoint) {
        this.model = model;
        this.endpoint = endpoint;
    }

    public String getModel() {
        return model;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
