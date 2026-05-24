package com.smartdx.property.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "property.search.opensearch")
public class PropertyOpenSearchProperties {

    private boolean enabled = true;

    private String endpoint = "http://localhost:9200";

    private String publishedIndex = "realty_published_listings";

    private String draftIndex = "realty_draft_listings";

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration requestTimeout = Duration.ofSeconds(3);

    @Bean
    public OpenSearchClient openSearchClient(ObjectMapper objectMapper) {
        System.out.println("[OpenSearch] enabled=" + enabled + ", endpoint=" + endpoint);
        if (!enabled) {
            return null;
        }
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() > 0 ? uri.getPort() : 9200;

            System.out.println("[OpenSearch] Connecting to: " + scheme + "://" + host + ":" + port);
            HttpHost httpHost = new HttpHost(scheme, host, port);
            var transport = ApacheHttpClient5TransportBuilder
                    .builder(httpHost)
                    .setMapper(new JacksonJsonpMapper(objectMapper))
                    .build();
            return new OpenSearchClient(transport);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create OpenSearch client", e);
        }
    }
}
