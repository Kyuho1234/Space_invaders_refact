package org.newdawn.spaceinvaders.firebase;

import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Firebase HTTP 통신을 담당하는 클래스
 * DIP (Dependency Inversion Principle) 적용 - 인터페이스로 추상화 가능
 */
public class FirebaseHttpClient {
    private String idToken;

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public JSONObject get(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        addAuthHeader(conn);

        return readResponse(conn);
    }

    public JSONObject post(String urlStr, JSONObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        writeBody(conn, body);
        return readResponse(conn);
    }

    public JSONObject patch(String urlStr, JSONObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        addAuthHeader(conn);
        conn.setDoOutput(true);

        writeBody(conn, body);
        return readResponse(conn);
    }

    public boolean delete(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("DELETE");
        addAuthHeader(conn);

        int code = conn.getResponseCode();
        if (code == 200 || code == 204) {
            return true;
        }

        InputStream is = conn.getErrorStream();
        String text = (is != null) ? readAll(is) : conn.getResponseMessage();
        System.err.println("DELETE error(" + code + "): " + text);
        return false;
    }

    private void addAuthHeader(HttpURLConnection conn) {
        if (idToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + idToken);
        }
    }

    private void writeBody(HttpURLConnection conn, JSONObject body) throws IOException {
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }
    }

    private JSONObject readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);

        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            System.err.println("HTTP error(" + code + "): " + text);
            return null;
        }
    }

    private String readAll(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
