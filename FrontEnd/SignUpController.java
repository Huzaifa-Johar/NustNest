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

public class SignUpController implements Initializable {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button createButton;
    @FXML private Label errorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createButton.setOnMouseEntered(_ -> createButton.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: #0d1117; -fx-font-size: 14px;" +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0; -fx-cursor: hand;"));
        createButton.setOnMouseExited(_ -> createButton.setStyle(
            "-fx-background-color: #2ecc71; -fx-text-fill: #0d1117; -fx-font-size: 14px;" +
            "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0; -fx-cursor: hand;"));
    }

    @FXML
    private void handleCreate() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (fullName.isEmpty()) {
            errorLabel.setText("Please enter your full name.");
            return;
        }
        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email address.");
            return;
        }
        if (!email.endsWith("@seecs.edu.pk")) {
            errorLabel.setText("Only @seecs.edu.pk accounts are accepted.");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please create a password.");
            return;
        }

        try {
            String body = "{\"fullName\":\"" + fullName + "\", \"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
            HttpClient client = ApiClient.getClient();

            HttpRequest request = AppConfig.requestBuilder("/auth/register")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code == 200 || code == 201 || code == 202 || code == 403) {
                goToEmailVerification(email, password);
            } else if (code == 409) {
                errorLabel.setText("Account already exists. Please login.");
            } else {
                errorLabel.setText("Could not create account. Try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Something went wrong. Try again.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) createButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Could not open login screen.");
        }
    }

    private void goToEmailVerification(String email, String password) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EmailVerificationScreen.fxml"));
        Parent root = loader.load();

        EmailVerificationController controller = loader.getController();
        controller.setCredentials(email, password);

        Stage stage = (Stage) createButton.getScene().getWindow();
        stage.setScene(new Scene(root));
    }
}

