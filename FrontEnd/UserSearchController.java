import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class UserSearchController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button backButton;
    @FXML private ListView<UserResult> resultsList;
    @FXML private Label statusLabel;

    private String profileName    = "";
    private String profileBio     = "";
    private String profilePfpPath = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.setOnAction(e -> handleSearch());

        resultsList.setCellFactory(lv -> new ListCell<UserResult>() {
            @Override
            protected void updateItem(UserResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                StackPane avatar = new StackPane();
                avatar.setMinSize(40, 40);
                avatar.setMaxSize(40, 40);
                avatar.setStyle("-fx-background-color: #2a3040; -fx-background-radius: 20;");
                Label avatarLbl = new Label(item.name.isEmpty() ? "?" :
                        String.valueOf(item.name.charAt(0)).toUpperCase());
                avatarLbl.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 15px; -fx-font-weight: bold;");
                avatar.getChildren().add(avatarLbl);

                VBox info = new VBox(3);
                Label nameLbl = new Label(item.name);
                nameLbl.setStyle("-fx-text-fill: #f0f0f0; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label idLbl = new Label("ID: " + item.id);
                idLbl.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");
                info.getChildren().addAll(nameLbl, idLbl);

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label chatBtn = new Label("Chat →");
                chatBtn.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");

                HBox row = new HBox(12, avatar, info, spacer, chatBtn);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
                row.setStyle("-fx-background-color: #222736; -fx-background-radius: 10;");

                row.setOnMouseEntered(e -> row.setStyle(
                        "-fx-background-color: #2a2f40; -fx-background-radius: 10; -fx-cursor: hand;"));
                row.setOnMouseExited(e -> row.setStyle(
                        "-fx-background-color: #222736; -fx-background-radius: 10;"));

                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 4 0 4 0;");
            }
        });

        resultsList.setOnMouseClicked(e -> {
            UserResult selected = resultsList.getSelectionModel().getSelectedItem();
            if (selected != null) openChat(selected);
        });

        searchButton.setOnMouseEntered(e -> searchButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: #1a1f2e; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20 10 20;" +
                        "-fx-background-radius: 10; -fx-border-width: 0;"));
        searchButton.setOnMouseExited(e -> searchButton.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: #1a1f2e; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20 10 20;" +
                        "-fx-background-radius: 10; -fx-border-width: 0;"));

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
    }

    public void setProfileData(String name, String bio, String pfpPath) {
        this.profileName    = name;
        this.profileBio     = bio;
        this.profilePfpPath = pfpPath;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Enter a name to search.");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
            return;
        }
        statusLabel.setText("Searching...");
        statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 13px;");
        resultsList.getItems().clear();

        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();

                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                HttpRequest request = AppConfig.requestBuilderRaw(
                                "/api/profile/search?name=" + encodedQuery)
                        .GET().build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());


                System.out.println("[UserSearch] URL:    " + request.uri());
                System.out.println("[UserSearch] Status: " + response.statusCode());
                System.out.println("[UserSearch] Body:   " + response.body());

                if (response.statusCode() != 200) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Server error (" + response.statusCode() + ")");
                        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                    });
                    return;
                }

                List<UserResult> users = parseUsers(response.body());

                Platform.runLater(() -> {
                    if (users.isEmpty()) {
                        statusLabel.setText("No users found for \"" + query + "\"");
                        statusLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 13px;");
                    } else {
                        statusLabel.setText(users.size() + " user(s) found");
                        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13px;");
                        resultsList.getItems().addAll(users);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("CourseSelectionScreen.fxml"));
            Parent root = loader.load();
            CourseSelectionController ctrl = loader.getController();
            ctrl.setProfileData(profileName, profileBio, profilePfpPath);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openChat(UserResult user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("ChatScreen.fxml"));
            Parent root = loader.load();
            ChatScreenController ctrl = loader.getController();
            ctrl.setRecipient(user.id, user.name);
            ctrl.setProfileData(profileName, profileBio, profilePfpPath);
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<UserResult> parseUsers(String json) {
        List<UserResult> list = new ArrayList<>();
        if (json == null || json.isBlank()) return list;

        int i = 0;
        while ((i = json.indexOf('{', i)) >= 0) {
            int end = findObjectEnd(json, i);
            if (end < 0) break;
            String obj = json.substring(i, end + 1);

            long id = parseLong(obj, "id");
            if (id < 0) id = parseLong(obj, "userId");


            String name = parseString(obj, "name");
            if (name == null) name = parseString(obj, "username");
            if (name == null) name = parseString(obj, "fullName");
            if (name == null) name = parseString(obj, "displayName");
            if (name == null) name = parseString(obj, "firstName");

            if (id > 0 && name != null && !name.isEmpty()) {
                list.add(new UserResult(id, name));
            }
            i = end + 1;
        }
        return list;
    }

    /** Finds the matching closing brace for nested JSON objects */
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

    private String parseString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    public static class UserResult {
        public final long id;
        public final String name;
        public UserResult(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}