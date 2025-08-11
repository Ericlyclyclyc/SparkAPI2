package org.lyc122.dev.tools.SparkAPI;

import java.io.PrintWriter;

public class SparkServiceBuilder {
    private String apiKey = null;
    private String apiSecret = null;
    private String appId = null;
    private ModelType modelType = null;
    private PrintWriter outputWriter = null;
    private boolean stream = false;
    private float temperature = 0.5F;
    public static SparkServiceBuilder ServiceBuilder(){
        return new SparkServiceBuilder();
    }

    public SparkServiceBuilder setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public SparkServiceBuilder setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }

    public SparkServiceBuilder setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public SparkServiceBuilder setModelType(ModelType modelType) {
        this.modelType = modelType;
        return this;
    }

    public SparkServiceBuilder setOutputWriter(PrintWriter outputWriter) {
        this.outputWriter = outputWriter;
        return this;
    }

    public SparkServiceBuilder set_stream(boolean stream) {
        this.stream = stream;
        return this;
    }

    public SparkServiceBuilder setTemperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public SparkService build() {
        String err_str = "";
        if(apiKey == null) err_str += "APIKey must be provided to build a SparkService\n";
        if(apiSecret == null) err_str += "APISecret must be provided to build a SparkService\n";
        if(appId == null) err_str += "APPID must be provided to build a SparkService";
        if(!err_str.isEmpty()) throw new IllegalArgumentException(err_str);
        return new SparkService(apiKey, apiSecret, appId, modelType, outputWriter, stream, temperature);
    }
}