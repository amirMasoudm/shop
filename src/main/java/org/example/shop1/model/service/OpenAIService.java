package org.example.shop1.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import io.netty.channel.ChannelOption;
import io.netty.resolver.DefaultAddressResolverGroup;

import java.time.Duration;
import java.util.*;

@Service
public class OpenAIService {

    private final WebClient client;
    private final String model;
    private final Duration timeout = Duration.ofSeconds(20);

    public OpenAIService(
            @Value("${openai.api.base}") String baseUrl,
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model
    ) {
        this.model = model;

        TcpClient tcpClient = TcpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        HttpClient httpClient = HttpClient.from(tcpClient)
                .responseTimeout(timeout);

        this.client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        System.out.println(">>> OpenAI KEY Exists: " + (apiKey != null));
    }

    public String generateDescription(String prompt) {

        Map<String, Object> req = new HashMap<>();
        req.put("model", model);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system",
                "content", "You write professional Persian product descriptions."));
        messages.add(Map.of("role", "user", "content", prompt));
        req.put("messages", messages);

        Map response = client.post()
                .uri("/chat/completions")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(timeout)
                .block();

        System.out.println(">>> OpenAI RAW RESPONSE: " + response);

        try {
            List choices = (List) response.get("choices");
            Map first = (Map) choices.get(0);
            Map message = (Map) first.get("message");

            String text = (String) message.get("content");
            return text;

        } catch (Exception e) {
            throw new RuntimeException("Parsing error: " + response, e);
        }
    }
}
