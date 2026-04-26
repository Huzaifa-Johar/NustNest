import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class EmailVerificationController implements Initializable {

    @FXML private Label             emailLabel;
    @FXML private Label             statusLabel;
    @FXML private Label             errorLabel;
    @FXML private ProgressIndicator spinner;
    @FXML private Button            resendButton;
    @FXML private Button            backButton;

    private String email    = "";
    private String password = "";
    private Timeline pollTimer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        resendButton.setOnMouseEntered(e -> resendButton.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: #0d1117; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0;" +
                        "-fx-cursor: hand; -fx-padding: 10 0 10 0;"));
        resendButton.setOnMouseExited(e -> resendButton.setStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: #0d1117; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-border-width: 0;" +
                        "-fx-cursor: hand; -fx-padding: 10 0 10 0;"));

        backButton.setOnMouseEntered(e -> backButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #f0f0f0; -fx-font-size: 12px;" +
                        "-fx-background-radius: 10; -fx-border-width: 0; -fx-cursor: hand;"));
        backButton.setOnMouseExited(e -> backButton.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #888888; -fx-font-size: 12px;" +
                        "-fx-background-radius: 10; -fx-border-width: 0; -fx-cursor: hand;"));
    }


    public void setCredentials(String email, String password) {
        this.email    = email;
        this.password = password;
        emailLabel.setText(email);
        startPolling();
    }

    private void startPolling() {
        pollTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> checkVerificationStatus()));
        pollTimer.setCycleCount(Timeline.INDEFINITE);
        pollTimer.play();
    }

    private void stopPolling() {
        if (pollTimer != null) pollTimer.stop();
    }

    private void checkVerificationStatus() {
        new Thread(() -> {
            try {
                String body = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
                HttpClient client = ApiClient.getClient();

                HttpRequest request = AppConfig.requestBuilder("/auth/login")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {
                    stopPolling();
                    Platform.runLater(this::goToSuccess);
                }
                // 403 = not verified yet, keep polling silently

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }


    @FXML
    private void handleResend() {
        errorLabel.setText("");
        statusLabel.setText("Resending...");

        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilder("/auth/resend-verification?email=" + email)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 202) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Verification email resent!");
                        errorLabel.setText("");
                    });
                } else {
                    Platform.runLater(() -> errorLabel.setText("Resend failed (" + response.statusCode() + ")."));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Failed to resend. Try again."));
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        stopPolling();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToSuccess() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SuccessScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
