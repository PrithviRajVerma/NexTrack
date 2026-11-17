package com.nextrack.development.infrastructure.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nextrack.integrations.github")
public class GitHubAppProperties {

    private String appId = "123456";
    private String privateKey = "";
    private WebhookProperties webhook = new WebhookProperties();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public WebhookProperties getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookProperties webhook) {
        this.webhook = webhook;
    }

    public static class WebhookProperties {
        private String secret = "mock_webhook_secret";
        private int maxBodyBytes = 2 * 1024 * 1024; // 2 MB
        private long workerDelayMs = 500;
        private long supervisorDelayMs = 60000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(int maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        public long getWorkerDelayMs() {
            return workerDelayMs;
        }

        public void setWorkerDelayMs(long workerDelayMs) {
            this.workerDelayMs = workerDelayMs;
        }

        public long getSupervisorDelayMs() {
            return supervisorDelayMs;
        }

        public void setSupervisorDelayMs(long supervisorDelayMs) {
            this.supervisorDelayMs = supervisorDelayMs;
        }
    }
}
