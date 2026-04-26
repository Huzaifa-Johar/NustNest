import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ChatScreenController implements Initializable {

    @FXML private Button backButton;
    @FXML private Label chatPartnerName;
    @FXML private Label avatarLabel;
    @FXML private StackPane onlineDot;
    @FXML private Label onlineStatusLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;

    private long recipientId;
    private String recipientName = "";
    private String profileName    = "";
    private String profileBio     = "";
    private String profilePfpPath = "";

    /** Java 11 built-in WebSocket — zero extra deps */
    private WebSocket webSocket;
    private final StringBuilder wsFrameBuffer = new StringBuilder();

    private static final String STOMP_CONNECT =
            "CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000";

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageInput.setOnAction(e -> handleSend());

        sendButton.setOnMouseEntered(e -> sendButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: #1a1f2e; -fx-font-size: 16px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 44; -fx-min-height: 44;" +
                        "-fx-max-width: 44; -fx-max-height: 44; -fx-background-radius: 22; -fx-border-width: 0;"));
        sendButton.setOnMouseExited(e -> sendButton.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: #1a1f2e; -fx-font-size: 16px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 44; -fx-min-height: 44;" +
                        "-fx-max-width: 44; -fx-max-height: 44; -fx-background-radius: 22; -fx-border-width: 0;"));

        backButton.setOnMouseEntered(e -> backButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #ffffff;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.25);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #aaaaaa;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));

        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                messagesScrollPane.setVvalue(1.0));
    }

    public void setRecipient(long id, String name) {
        this.recipientId   = id;
        this.recipientName = name;
        if (chatPartnerName != null) chatPartnerName.setText(name);
        if (avatarLabel != null)
            avatarLabel.setText(name.isEmpty() ? "?" :
                    String.valueOf(name.charAt(0)).toUpperCase());
        loadHistory();
        checkOnlineStatus();
        connectWebSocket();
        markMessagesRead();
    }

    public void setProfileData(String name, String bio, String pfpPath) {
        this.profileName    = name;
        this.profileBio     = bio;
        this.profilePfpPath = pfpPath;
    }

    @FXML
    private void handleBack() {
        disconnectWebSocket();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("UserSearchScreen.fxml"));
            Parent root = loader.load();
            UserSearchController ctrl = loader.getController();
            ctrl.setProfileData(profileName, profileBio, profilePfpPath);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleSend() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;
        messageInput.clear();
        addMessageBubble(text, true, LocalDateTime.now());
        sendViaRest(text); // Always REST: session cookie carries auth, WS has no session context
    }

    private void loadHistory() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilderRaw("/api/chat/history/" + recipientId).GET().build();
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<MessageData> messages = parseMessages(response.body());
                    long myId = parseMyId();
                    Platform.runLater(() -> {
                        messagesContainer.getChildren().clear();
                        for (MessageData msg : messages)
                            addMessageBubble(msg.content, msg.senderId == myId, msg.sentAt);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void checkOnlineStatus() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilderRaw("/api/profile/online/" + recipientId).GET().build();
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());
                boolean online = response.statusCode() == 200 &&
                        response.body().contains("true");
                Platform.runLater(() -> {
                    if (online) {
                        onlineDot.setStyle("-fx-background-color: #2ecc71; -fx-background-radius: 4;");
                        onlineStatusLabel.setText("online");
                        onlineStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");
                    } else {
                        onlineDot.setStyle("-fx-background-color: #555555; -fx-background-radius: 4;");
                        onlineStatusLabel.setText("offline");
                        onlineStatusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void markMessagesRead() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilderRaw(
                                "/api/chat/read/" + recipientId)
                        .PUT(HttpRequest.BodyPublishers.noBody()).build();
                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }


    private void connectWebSocket() {
        new Thread(() -> {
            try {
                String wsUrl = AppConfig.BASE_URL
                        .replace("http://", "ws://")
                        .replace("https://", "wss://")
                        + "/ws/websocket";

                HttpClient httpClient = HttpClient.newHttpClient();
                webSocket = httpClient.newWebSocketBuilder()
                        .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {

                            @Override
                            public void onOpen(WebSocket ws) {
                                ws.sendText(STOMP_CONNECT, true);
                                ws.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(WebSocket ws,
                                                             CharSequence data,
                                                             boolean last) {
                                wsFrameBuffer.append(data);
                                if (last) {
                                    handleStompFrame(wsFrameBuffer.toString());
                                    wsFrameBuffer.setLength(0);
                                }
                                ws.request(1);
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket ws,
                                                              int statusCode,
                                                              String reason) {
                                webSocket = null;
                                return CompletableFuture.completedFuture(null);
                            }

                            @Override
                            public void onError(WebSocket ws, Throwable error) {
                                System.err.println("WS error: " + error.getMessage());
                                webSocket = null;
                            }
                        }).get();

            } catch (Exception e) {
                System.err.println("WS connect failed: " + e.getMessage());
                webSocket = null;
            }
        }).start();
    }

    private void handleStompFrame(String frame) {
        if (frame.startsWith("CONNECTED")) {
            String sub = "SUBSCRIBE\nid:sub-0\ndestination:/user/queue/messages\n\n\u0000";
            if (webSocket != null) webSocket.sendText(sub, true);

        } else if (frame.startsWith("MESSAGE")) {
            int bodyStart = frame.indexOf("\n\n");
            if (bodyStart < 0) return;
            String body = frame.substring(bodyStart + 2).replace("\u0000", "").trim();
            long senderId  = parseLong(body, "senderId");
            String content = parseStr(body, "content");
            if (content != null && senderId == recipientId) {
                Platform.runLater(() ->
                        addMessageBubble(content, false, LocalDateTime.now()));
            }
        }
    }

    private void sendViaWebSocket(String text) {
        String json = "{\"receiverId\":" + recipientId +
                ",\"content\":\"" + text.replace("\"", "\\\"") + "\"}";
        String frame = "SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n"
                + json + "\u0000";
        webSocket.sendText(frame, true);
    }

    private void sendViaRest(String text) {
        new Thread(() -> {
            try {
                String body = "{\"receiverId\":" + recipientId +
                        ",\"content\":\"" + text.replace("\"", "\\\"") + "\"}";
                System.out.println("[Send] body=" + body);
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilderRaw("/api/chat/send")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Send] status=" + resp.statusCode() + " resp=" + resp.body());
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void disconnectWebSocket() {
        if (webSocket != null) {
            try {
                webSocket.sendText("DISCONNECT\n\n\u0000", true);
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {}
            webSocket = null;
        }
    }


    private void addMessageBubble(String text, boolean isMine, LocalDateTime time) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(340);
        bubble.setPadding(new Insets(10, 14, 10, 14));

        Label timeLabel = new Label(time != null ? time.format(TIME_FMT) : "");
        timeLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 10px;");

        if (isMine) {
            bubble.setStyle(
                    "-fx-background-color: #2ecc71; -fx-text-fill: #1a1f2e;" +
                            "-fx-font-size: 14px; -fx-background-radius: 16 16 4 16;");
            VBox wrapper = new VBox(3, bubble, timeLabel);
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            HBox row = new HBox(wrapper);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(2, 0, 2, 60));
            messagesContainer.getChildren().add(row);
        } else {
            bubble.setStyle(
                    "-fx-background-color: #2a2f40; -fx-text-fill: #f0f0f0;" +
                            "-fx-font-size: 14px; -fx-background-radius: 16 16 16 4;");
            VBox wrapper = new VBox(3, bubble, timeLabel);
            wrapper.setAlignment(Pos.CENTER_LEFT);
            HBox row = new HBox(wrapper);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 60, 2, 0));
            messagesContainer.getChildren().add(row);
        }
    }

    // -------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------
    private long parseMyId() {
        try { return Long.parseLong(AppConfig.getCurrentUserId()); }
        catch (Exception e) { return -1; }
    }

    private List<MessageData> parseMessages(String json) {
        List<MessageData> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;
        int i = 0;
        while ((i = json.indexOf('{', i)) >= 0) {
            int end = findObjectEnd(json, i);
            if (end < 0) break;
            String obj      = json.substring(i, end + 1);
            long senderId   = parseLong(obj, "senderId");
            long receiverId = parseLong(obj, "receiverId");
            String content  = parseStr(obj, "content");
            String sentAtStr = parseStr(obj, "sentAt");
            LocalDateTime sentAt = null;
            if (sentAtStr != null) {
                try { sentAt = LocalDateTime.parse(sentAtStr); } catch (Exception ignored) {}
            }
            if (content != null) list.add(new MessageData(senderId, receiverId, content, sentAt));
            i = end + 1;
        }
        return list;
    }

    private int findObjectEnd(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private long parseLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return -1;
        String rest = json.substring(idx + search.length()).trim();
        int end = rest.indexOf(',');
        if (end < 0) end = rest.indexOf('}');
        if (end < 0) return -1;
        try { return Long.parseLong(rest.substring(0, end).trim()); }
        catch (Exception e) { return -1; }
    }

    private String parseStr(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private static class MessageData {
        final long senderId, receiverId;
        final String content;
        final LocalDateTime sentAt;
        MessageData(long s, long r, String c, LocalDateTime t) {
            senderId = s; receiverId = r; content = c; sentAt = t;
        }
    }
}