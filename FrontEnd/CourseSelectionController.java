import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class CourseSelectionController implements Initializable {

    @FXML private VBox card_oop, card_la, card_dm,
            card_hu, card_dld, card_uoq;

    @FXML private Label count_oop, count_la, count_dm,
            count_hu, count_dld, count_uoq;

    @FXML private Button profileButton;
    @FXML private Button logoutButton;
    @FXML private Button chatButton;

    private String profileName    = "";
    private String profileBio     = "";
    private String profilePfpPath = "";

    private Map<String, Integer> fileCounts = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Default all counts to 0
        fileCounts.put("oop",  0);
        fileCounts.put("la",   0);
        fileCounts.put("dm",   0);
        fileCounts.put("hu",   0);
        fileCounts.put("dld",  0);
        fileCounts.put("uoq",  0);


        fetchFileCounts();


        setupCardClick(card_oop,  "OOP");
        setupCardClick(card_la,   "Linear Algebra");
        setupCardClick(card_dm,   "Discrete Maths");
        setupCardClick(card_hu,   "Functional English");
        setupCardClick(card_dld,  "Computer Architecture and Logic Design");
        setupCardClick(card_uoq,  "Understanding of Quran");


        profileButton.setOnMouseEntered(e -> profileButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #ffffff;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.25);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));
        profileButton.setOnMouseExited(e -> profileButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #aaaaaa;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));

        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle(
                "-fx-background-color: rgba(231,76,60,0.12); -fx-text-fill: #ff6b6b;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(231,76,60,0.6);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e74c3c;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(231,76,60,0.35);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));

        chatButton.setOnMouseEntered(e -> chatButton.setStyle(
                "-fx-background-color: rgba(46,204,113,0.12); -fx-text-fill: #2ecc71;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(46,204,113,0.6);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));
        chatButton.setOnMouseExited(e -> chatButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #2ecc71;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 14 6 14;" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(46,204,113,0.35);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"));
    }

    private void fetchFileCounts() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilder("/courses/file-counts")
                        .GET().build();
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    fileCounts.put("oop",  parseCount(body, "CS_212_OOP"));
                    fileCounts.put("la", parseCount(body, "MATH_121_LINEAR_ALGEBRA_AND_ORDINARY_DIFFERENTIAL_EQUATIONS"));
                    fileCounts.put("dm",   parseCount(body, "MATH_161_DISCRETE_MATHEMATICS"));
                    fileCounts.put("hu",   parseCount(body, "HU_114_FUNCTIONAL_ENGLISH"));
                    fileCounts.put("dld",  parseCount(body, "EE_122_COMPUTER_ARCHITECTURE_AND_LOGIC_DESIGN"));
                    fileCounts.put("uoq", parseCount(body, "HU_132_UNDERSTANDING_OF_QURAN"));
                }

                Platform.runLater(() -> {
                    updateCountLabel(count_oop,  card_oop,  fileCounts.get("oop"));
                    updateCountLabel(count_la,   card_la,   fileCounts.get("la"));
                    updateCountLabel(count_dm,   card_dm,   fileCounts.get("dm"));
                    updateCountLabel(count_hu,   card_hu,   fileCounts.get("hu"));
                    updateCountLabel(count_dld,  card_dld,  fileCounts.get("dld"));
                    updateCountLabel(count_uoq,  card_uoq,  fileCounts.get("uoq"));
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    public void setProfileData(String name, String bio, String pfpPath) {
        this.profileName    = name;
        this.profileBio     = bio;
        this.profilePfpPath = pfpPath;
    }

    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("ProfileViewScreen.fxml")
            );
            Parent root = loader.load();

            ProfileViewController profileViewController = loader.getController();
            profileViewController.setProfileData(profileName, profileBio, profilePfpPath);
            profileViewController.setCourseSelectionController(this);

            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        profileName    = "";
        profileBio     = "";
        profilePfpPath = "";

        try {
            HttpClient client = ApiClient.getClient();
            HttpRequest logoutRequest = AppConfig.requestBuilderRaw("/auth/logout")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            client.send(logoutRequest, HttpResponse.BodyHandlers.discarding());

            URL fxmlUrl = getClass().getResource("LoginScreen.fxml");
            if (fxmlUrl == null) {
                System.err.println("LOGOUT ERROR: LoginScreen.fxml not found on classpath!");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            System.err.println("LOGOUT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("UserSearchScreen.fxml"));
            Parent root = loader.load();
            UserSearchController ctrl = loader.getController();
            ctrl.setProfileData(profileName, profileBio, profilePfpPath);
            Stage stage = (Stage) chatButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCountLabel(Label label, VBox card, int count) {
        if (count == 0) {
            label.setText("0 files");
            label.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
        } else {
            label.setText(count + " file" + (count == 1 ? "" : "s"));
            label.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 12px;");
            card.setStyle(
                    "-fx-background-color: #222736;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: rgba(46,204,113,0.35);" +
                            "-fx-border-radius: 14;" +
                            "-fx-border-width: 1;" +
                            "-fx-cursor: hand;"
            );
        }
    }

    private void setupCardClick(VBox card, String courseName) {
        String normalStyle =
                "-fx-background-color: #222736;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";

        String hoverStyle =
                "-fx-background-color: #2a2f40;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: rgba(46,204,113,0.4);" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;";

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(normalStyle));
        card.setOnMouseClicked(e -> goToCourse(courseName));
    }

    private void goToCourse(String courseName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("NotesScreen.fxml")
            );
            Parent root = loader.load();

            NotesController notesController = loader.getController();
            notesController.setCourseName(courseName);

            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int parseCount(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        String rest = json.substring(idx + search.length()).trim();
        int end = rest.indexOf(',');
        if (end < 0) end = rest.indexOf('}');
        if (end < 0) return 0;
        try { return Integer.parseInt(rest.substring(0, end).trim()); }
        catch (Exception e) { return 0; }
    }
}