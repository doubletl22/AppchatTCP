package com.example.chatclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuthService {

    private static final String SERVER_URL = "http://localhost:8080";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String login(String username) throws Exception {
        String jsonPayload = String.format("{\"username\":\"%s\", \"password\":\"password123\"}", username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            return root.get("token").asText();
        } else {
            throw new RuntimeException("Login failed with status code: " + response.statusCode());
        }
    }
}