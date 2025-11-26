package com.chat.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GifService {
    // Key này là key test công khai của Tenor
    private static final String API_KEY = "LIVDSRZULELA";
    private static final String CLIENT_KEY = "AppChatTCP";
    private static final String BASE_URL = "https://g.tenor.com/v1/";

    private final HttpClient client = HttpClient.newHttpClient();

    // Lấy danh sách GIF thịnh hành (Trending)
    public List<String> getTrendingGifs(int limit) {
        // Endpoint 'trending' của Tenor
        String url = BASE_URL + "trending?key=" + API_KEY + "&client_key=" + CLIENT_KEY + "&limit=" + limit + "&media_filter=minimal";
        return fetchGifs(url);
    }

    // Tìm kiếm GIF theo từ khóa
    public List<String> searchGifs(String query, int limit) {
        String encodedQuery = query.replaceAll(" ", "%20");
        // Endpoint 'search'
        String url = BASE_URL + "search?q=" + encodedQuery + "&key=" + API_KEY + "&client_key=" + CLIENT_KEY + "&limit=" + limit + "&media_filter=minimal";
        return fetchGifs(url);
    }

    private List<String> fetchGifs(String urlString) {
        List<String> gifUrls = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray results = jsonObject.getAsJsonArray("results");

                for (int i = 0; i < results.size(); i++) {
                    JsonObject mediaFormats = results.get(i).getAsJsonObject().getAsJsonArray("media").get(0).getAsJsonObject();

                    // Lấy link 'tinygif' (ảnh nhỏ) để hiển thị trên list cho mượt
                    // Hoặc 'gif' (ảnh gốc) nếu muốn nét
                    if (mediaFormats.has("tinygif")) {
                        String url = mediaFormats.getAsJsonObject("tinygif").get("url").getAsString();
                        gifUrls.add(url);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy GIF: " + e.getMessage());
            e.printStackTrace();
        }
        return gifUrls;
    }
}