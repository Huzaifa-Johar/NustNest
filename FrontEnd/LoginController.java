import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button    continueButton;
    @FXML private Label     errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        continueButton.setOnMouseEntered(e -> continueButton.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: #0d1117; -fx-font-size: 14px;" +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        continueButton.setOnMouseExited(e -> continueButton.setStyle(
            "-fx-background-color: #2ecc71; -fx-text-fill: #0d1117; -fx-font-size: 14px;" +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setText("");
            String normalized = newVal.trim().toLowerCase();
            if (!normalized.isEmpty() && !normalized.endsWith("@seecs.edu.pk")) {
                emailField.setStyle(
                    "-fx-background-color: #2a2f40; -fx-text-fill: #f0f0f0; -fx-font-size: 13px;" +
                    "-fx-background-radius: 10; -fx-border-color: #e74c3c;" +
                    "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 0 14 0 14;");
            } else {
                emailField.setStyle(
                    "-fx-background-color: #2a2f40; -fx-text-fill: #f0f0f0; -fx-font-size: 13px;" +
                    "-fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.12);" +
                    "-fx-border-radius: 10; -fx-border-width: 1; -fx-padding: 0 14 0 14;");
            }
        });
    }

    @FXML
    private void handleContinue() {
        String email    = emailField.getText().trim().toLowerCase();
        emailField.setText(email);
        String password = passwordField.getText().trim();

        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email address.");
            return;
        }
        if (!email.endsWith("@seecs.edu.pk")) {
            errorLabel.setText("Only @seecs.edu.pk accounts are accepted.");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            return;
        }

        try {
            String body = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
            HttpClient client = ApiClient.getClient();

            HttpRequest loginRequest = AppConfig.requestBuilder("/auth/login")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> loginResponse = client.send(
                loginRequest, HttpResponse.BodyHandlers.ofString()
            );

            if (loginResponse.statusCode() == 200) {
                body = loginResponse.body();
                // Parse userId from response
                String search = "\"userId\":";
                int idx = body.indexOf(search);
                if (idx >= 0) {
                    String rest = body.substring(idx + search.length()).trim();
                    int end = rest.indexOf(',');
                    if (end < 0) end = rest.indexOf('}');
                    String userId = rest.substring(0, end).trim();
                    AppConfig.setCurrentUserId(userId);
                }
                goToApp();
            } else if (loginResponse.statusCode() == 403) {
                goToEmailVerification(email, password);
            } else if (loginResponse.statusCode() >= 500) {
                errorLabel.setText("Server error. Please try again.");
            } else {
                errorLabel.setText("Invalid email or password.");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Something went wrong. Try again.");
        }
    }

    private void goToApp() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("CourseSelectionScreen.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) continueButton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void goToEmailVerification(String email, String password) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EmailVerificationScreen.fxml"));
        Parent root = loader.load();
        EmailVerificationController controller = loader.getController();
        controller.setCredentials(email, password);
        Stage stage = (Stage) continueButton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    @FXML
    private void handleGoToSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SignUpScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) continueButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Could not open sign up screen.");
        }
    }
}
