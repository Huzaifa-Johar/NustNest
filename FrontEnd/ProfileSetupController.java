import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ResourceBundle;
import java.util.UUID;

public class ProfileSetupController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextArea  bioField;
    @FXML private Label     bioCharCount;
    @FXML private Label     errorLabel;
    @FXML private Label     pfpInitials;
    @FXML private StackPane pfpPane;
    @FXML private Button    saveButton;

    private static final int MAX_BIO = 150;
    private String pfpPath = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {

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

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setText("");
            if (!newVal.trim().isEmpty()) {
                String[] parts = newVal.trim().split(" ");
                String initials = parts.length >= 2
                        ? String.valueOf(parts[0].charAt(0)).toUpperCase()
                        + String.valueOf(parts[1].charAt(0)).toUpperCase()
                        : String.valueOf(parts[0].charAt(0)).toUpperCase();
                pfpInitials.setText(initials);
                pfpInitials.setStyle(
                        "-fx-text-fill: #2ecc71; -fx-font-size: 30px; -fx-font-weight: bold;"
                );
            } else {
                pfpInitials.setText("?");
                pfpInitials.setStyle(
                        "-fx-text-fill: #555555; -fx-font-size: 30px; -fx-font-weight: bold;"
                );
            }
        });
    }

    @FXML
    private void handleUploadPfp() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) saveButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            pfpPath = file.getAbsolutePath();

            // Show preview immediately
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(90);
            imageView.setFitHeight(90);
            imageView.setPreserveRatio(false);
            Circle clip = new Circle(45, 45, 45);
            imageView.setClip(clip);
            pfpPane.getChildren().clear();
            pfpPane.getChildren().add(imageView);

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
                String mimeType = file.getName().endsWith(".png") ? "image/png" : "image/jpeg";

                String partHeader = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n\r\n";
                String partFooter = "\r\n--" + boundary + "--\r\n";

                byte[] headerBytes  = partHeader.getBytes();
                byte[] footerBytes  = partFooter.getBytes();
                byte[] body         = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
                System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
                System.arraycopy(fileBytes,   0, body, headerBytes.length, fileBytes.length);
                System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

                HttpRequest request = AppConfig.requestBuilderRaw("/profile/image")
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                client.send(request, HttpResponse.BodyHandlers.ofString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String bio  = bioField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Please enter your full name.");
            return;
        }
        if (name.length() < 3) {
            errorLabel.setText("Name must be at least 3 characters.");
            return;
        }

        try {
            HttpClient client = ApiClient.getClient();
            // PUT /api/profile/bio  — update bio
            String bioBody = "{\"bio\":\"" + bio + "\"}";

            HttpRequest bioRequest = AppConfig.requestBuilder("/profile/bio")
                    .PUT(HttpRequest.BodyPublishers.ofString(bioBody))
                    .build();

            HttpResponse<String> bioResponse = client.send(
                    bioRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (bioResponse.statusCode() != 200) {
                errorLabel.setText("Failed to save profile. Try again.");
                return;
            }

            goToCourseSelection(name, bio, pfpPath);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Something went wrong. Try again.");
        }
    }

    @FXML
    private void handleSkip() {
        goToCourseSelection("", "", "");
    }

    private void goToCourseSelection(String name, String bio, String pfpPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("CourseSelectionScreen.fxml")
            );
            Parent root = loader.load();

            CourseSelectionController controller = loader.getController();
            controller.setProfileData(name, bio, pfpPath);

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Something went wrong. Try again.");
        }
    }
}
