import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.UUID;

public class ProfileViewController implements Initializable {

    @FXML private Label     nameLabel;
    @FXML private Label     bioLabel;
    @FXML private Label     pfpInitials;
    @FXML private TextField nameField;
    @FXML private TextArea  bioField;
    @FXML private Label     bioCharCount;
    @FXML private StackPane pfpImagePane;
    @FXML private Button    changePfpButton;
    @FXML private Button    editSaveButton;
    @FXML private Button    cancelButton;
    @FXML private Button    backButton;
    @FXML private Label     errorLabel;

    private static final int MAX_BIO = 150;
    private boolean editMode = false;

    private String currentName    = "";
    private String currentBio     = "";
    private String currentPfpPath = "";

    private String snapshotName;
    private String snapshotBio;
    private String snapshotPfpPath;

    private CourseSelectionController courseSelectionController;



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupHoverEffects();
        Platform.runLater(() -> {
            fixTextAreaStyle(bioField);
            fetchProfileFromBackend();
        });

        bioField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > MAX_BIO) {
                bioField.setText(oldVal);
                return;
            }
            bioCharCount.setText(newVal.length() + " / " + MAX_BIO);
            bioCharCount.setStyle(
                    "-fx-text-fill: " + (newVal.length() > 120 ? "#e74c3c" : "#555555") +
                            "; -fx-font-size: 11px; -fx-alignment: center-right;"
            );
        });
    }

    public void setProfileData(String name, String bio, String pfpPath) {
        this.currentName    = name;
        this.currentBio     = bio;
        this.currentPfpPath = pfpPath;

    }

    public void setCourseSelectionController(CourseSelectionController controller) {
        this.courseSelectionController = controller;
    }


    private void refreshViewLabels() {
        nameLabel.setText(currentName.isEmpty() ? "No name set" : currentName);
        bioLabel.setText(currentBio.isEmpty()   ? "No bio added yet." : currentBio);


        if (currentPfpPath.isEmpty()) {
            pfpInitials.setText(getInitials(currentName));
            pfpInitials.setVisible(true);
            pfpInitials.setManaged(true);
            pfpImagePane.setVisible(false);
            pfpImagePane.setManaged(false);
        }
    }



    @FXML
    private void handleEditSave() {
        if (!editMode) enterEditMode();
        else saveChanges();
    }

    private void enterEditMode() {
        snapshotName    = currentName;
        snapshotBio     = currentBio;
        snapshotPfpPath = currentPfpPath;

        nameField.setText(currentName);
        nameField.setEditable(false);
        nameField.setStyle(
                "-fx-background-color: #2a2f40; -fx-text-fill: #888888;" +
                        "-fx-font-size: 14px; -fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.06); -fx-border-radius: 10;" +
                        "-fx-border-width: 1; -fx-padding: 0 14 0 14;"
        );

        bioField.setText(currentBio);
        bioCharCount.setText(currentBio.length() + " / " + MAX_BIO);

        nameLabel.setVisible(false);   nameLabel.setManaged(false);
        bioLabel.setVisible(false);    bioLabel.setManaged(false);
        nameField.setVisible(true);    nameField.setManaged(true);
        bioField.setVisible(true);     bioField.setManaged(true);
        bioCharCount.setVisible(true); bioCharCount.setManaged(true);

        cancelButton.setVisible(true);     cancelButton.setManaged(true);
        changePfpButton.setVisible(true);  changePfpButton.setManaged(true);

        editSaveButton.setText("Save Changes");
        errorLabel.setText("");
        editMode = true;

        Platform.runLater(() -> fixTextAreaStyle(bioField));
    }

    private void saveChanges() {
        String newBio = bioField.getText().trim();

        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();

                String body = "{\"bio\":\"" + newBio + "\"}";

                HttpRequest request = AppConfig.requestBuilder("/profile/bio")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                System.out.println("Save bio response: " + response.statusCode() + " " + response.body());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        exitEditMode();
                        fetchProfileFromBackend();
                    } else {
                        errorLabel.setText("Failed to save. Try again.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Something went wrong. Try again."));
            }
        }).start();
    }


    @FXML
    private void handleCancel() {
        currentName    = snapshotName;
        currentBio     = snapshotBio;
        currentPfpPath = snapshotPfpPath;
        refreshViewLabels();
        exitEditMode();
    }

    private void exitEditMode() {
        nameField.setVisible(false);    nameField.setManaged(false);
        bioField.setVisible(false);     bioField.setManaged(false);
        bioCharCount.setVisible(false); bioCharCount.setManaged(false);
        nameLabel.setVisible(true);     nameLabel.setManaged(true);
        bioLabel.setVisible(true);      bioLabel.setManaged(true);

        cancelButton.setVisible(false);    cancelButton.setManaged(false);
        changePfpButton.setVisible(false); changePfpButton.setManaged(false);

        editSaveButton.setText("Edit Profile");
        errorLabel.setText("");
        editMode = false;
    }


    @FXML
    private void handleChangePfp() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) editSaveButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Show preview immediately from local file
            showPfpImageFromFile(file);
            // Upload to backend
            uploadProfileImage(file);
        }
    }

    private void uploadProfileImage(File file) {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                String boundary = UUID.randomUUID().toString();
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String mimeType  = file.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

                String partHeader = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n\r\n";
                String partFooter = "\r\n--" + boundary + "--\r\n";

                byte[] headerBytes = partHeader.getBytes();
                byte[] footerBytes = partFooter.getBytes();
                byte[] body        = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
                System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
                System.arraycopy(fileBytes,   0, body, headerBytes.length, fileBytes.length);
                System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

                HttpRequest request = AppConfig.requestBuilderRaw("/profile/image")
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Upload image response: " + response.statusCode() + " " + response.body());

                if (response.statusCode() == 200) {
                    // Re-fetch profile to update currentPfpPath
                    Platform.runLater(() -> fetchProfileFromBackend());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }



    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("CourseSelectionScreen.fxml")
            );
            Parent root = loader.load();

            CourseSelectionController controller = loader.getController();
            controller.setProfileData(currentName, currentBio, currentPfpPath);

            if (courseSelectionController != null) {
                courseSelectionController.setProfileData(currentName, currentBio, currentPfpPath);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void fetchProfileFromBackend() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilder("/profile/me")
                        .GET().build();
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Profile response: " + response.body());

                if (response.statusCode() == 200) {
                    String body    = response.body();
                    String name    = extractField(body, "fullName");
                    String bio     = extractField(body, "bio");
                    String pfpPath = extractField(body, "profileImagePath");

                    Platform.runLater(() -> {
                        currentName    = name    != null ? name    : "";
                        currentBio     = bio     != null ? bio     : "";
                        currentPfpPath = pfpPath != null ? pfpPath : "";
                        refreshViewLabels();
                        // Fetch image from backend if path exists
                        if (pfpPath != null && !pfpPath.isEmpty()) {
                            fetchProfileImageFromBackend();
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void fetchProfileImageFromBackend() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                String userId = AppConfig.getCurrentUserId();
                HttpRequest request = AppConfig.requestBuilderRaw(
                                "/profile/image/" + userId)
                        .GET().build();
                HttpResponse<byte[]> response = client.send(
                        request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Image image = new Image(new java.io.ByteArrayInputStream(response.body()));
                    Platform.runLater(() -> showPfpImageFromBackend(image));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }


    private void showPfpImageFromFile(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            showPfpImageFromBackend(image);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showPfpImageFromBackend(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(false);
        Circle clip = new Circle(50, 50, 50);
        imageView.setClip(clip);
        pfpImagePane.getChildren().clear();
        pfpImagePane.getChildren().add(imageView);
        pfpImagePane.setVisible(true);
        pfpImagePane.setManaged(true);
        pfpInitials.setVisible(false);
        pfpInitials.setManaged(false);
    }



    private String extractField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    private void fixTextAreaStyle(TextArea textArea) {
        Node content = textArea.lookup(".content");
        if (content != null) content.setStyle("-fx-background-color: #2a2f40;");
        Node scrollPane = textArea.lookup(".scroll-pane");
        if (scrollPane != null) scrollPane.setStyle("-fx-background-color: #2a2f40;");
        Node corner = textArea.lookup(".scroll-pane-corner");
        if (corner != null) corner.setStyle("-fx-background-color: #2a2f40;");
    }

    private void setupHoverEffects() {
        editSaveButton.setOnMouseEntered(e -> editSaveButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: #0d1117; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0;" +
                        "-fx-cursor: hand; -fx-padding: 8 24 8 24;"));
        editSaveButton.setOnMouseExited(e -> editSaveButton.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: #0d1117; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0;" +
                        "-fx-cursor: hand; -fx-padding: 8 24 8 24;"));

        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #cccccc;" +
                        "-fx-font-size: 13px; -fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.18); -fx-border-radius: 10;" +
                        "-fx-border-width: 1; -fx-cursor: hand; -fx-padding: 8 20 8 20;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #888888;" +
                        "-fx-font-size: 13px; -fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 10;" +
                        "-fx-border-width: 1; -fx-cursor: hand; -fx-padding: 8 20 8 20;"));

        backButton.setOnMouseEntered(e -> backButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #f0f0f0;" +
                        "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 2 10 2 10;" +
                        "-fx-background-radius: 8; -fx-border-width: 0;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #aaaaaa;" +
                        "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 2 10 2 10;" +
                        "-fx-background-radius: 8; -fx-border-width: 0;"));
    }
}