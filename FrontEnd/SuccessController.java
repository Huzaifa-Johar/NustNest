import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class SuccessController implements Initializable {

    @FXML
    private Button continueButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }


    @FXML
    private void handleContinue() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ProfileSetupScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) continueButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}