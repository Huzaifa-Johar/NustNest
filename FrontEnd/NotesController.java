import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class NotesController implements Initializable {

    @FXML private Label       courseTitleLabel;
    @FXML private Label       courseIcon;
    @FXML private Label       fileCountLabel;
    @FXML private VBox        notesContainer;
    @FXML private VBox        emptyState;
    @FXML private Button      uploadButton;
    @FXML private Button      backButton;
    @FXML private Button      sortButton;

    private String courseName = "";

    private static class NoteEntry {
        String        id;
        String        fileName;
        long          fileSize;
        int           rating;
        LocalDateTime uploadedAt;

        NoteEntry(String id, String fileName, long fileSize) {
            this.id         = id;
            this.fileName   = fileName;
            this.fileSize   = fileSize;
            this.rating     = 0;
            this.uploadedAt = LocalDateTime.now();
        }
    }

    private final List<NoteEntry> notes = new ArrayList<>();
    private volatile boolean destroyed = false;

    private static final String SORT_HIGHEST_RATED = "⭐  Highest Rated";
    private static final String SORT_LOWEST_RATED  = "☆  Lowest Rated";
    private static final String SORT_RECENT        = "🕐  Recently Uploaded";
    private static final String SORT_OLDEST        = "🕓  Oldest First";

    private ContextMenu sortMenu;
    private String currentSort = SORT_HIGHEST_RATED;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortMenu = new ContextMenu();
        sortMenu.setStyle(
                "-fx-background-color: #222736;" +
                        "-fx-border-color: rgba(255,255,255,0.10);" +
                        "-fx-border-width: 1;"
        );

        for (String option : new String[]{SORT_HIGHEST_RATED, SORT_LOWEST_RATED, SORT_RECENT, SORT_OLDEST}) {
            MenuItem item = new MenuItem(option);
            item.setStyle(
                    "-fx-text-fill: #f0f0f0;" +
                            "-fx-background-color: #222736;" +
                            "-fx-font-size: 12px;"
            );
            item.setOnAction(e -> {
                currentSort = option;
                sortButton.setText(option + "  ▾");
                rebuildNotesList();
            });
            sortMenu.getItems().add(item);
        }
    }

    public void setCourseName(String name) {
        this.courseName = name;
        courseTitleLabel.setText(name);
        courseIcon.setText(String.valueOf(name.charAt(0)).toUpperCase());
        fetchNotes();
    }



    private void fetchNotes() {
        new Thread(() -> {
            try {
                HttpClient client = ApiClient.getClient();
                HttpRequest request = AppConfig.requestBuilderRaw(
                                "/notes/subject/" + getSubjectKey(courseName) + "?sortBy=HIGHEST_RATING")
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Notes response: " + response.body());
                if (response.statusCode() == 200) {
                    List<NoteEntry> fetched = parseNotesJson(response.body());
                    Platform.runLater(() -> {
                        if (destroyed) return;
                        notes.clear();
                        notes.addAll(fetched);
                        updateFileCount();
                        rebuildNotesList();
                    });
                } else {
                    Platform.runLater(() -> { if (destroyed) return; updateFileCount(); rebuildNotesList(); });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if (destroyed) return; updateFileCount(); rebuildNotesList(); });
            }
        }).start();
    }



    private List<NoteEntry> parseNotesJson(String json) {
        List<NoteEntry> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]"))   json = json.substring(0, json.length() - 1);
        String[] objects = json.split("\\},\\s*\\{");
        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "").trim();
            if (obj.isBlank()) continue;
            String id        = extractJsonField(obj, "id");
            String title     = extractJsonField(obj, "title");
            String fileName  = extractJsonField(obj, "fileName");
            String sizeStr   = extractJsonField(obj, "fileSize");
            String ratingStr = extractJsonField(obj, "averageRating");

            if (id == null || id.isBlank()) continue;
            if (!id.matches("\\d+")) continue; // skip if not numeric ID

            String name = (title    != null && !title.isBlank())    ? title
                    : (fileName != null && !fileName.isBlank()) ? fileName : id;

            long size = 0;
            try { if (sizeStr != null) size = Long.parseLong(sizeStr.trim()); }
            catch (Exception ignored) {}

            int rating = 0;
            try { if (ratingStr != null) rating = (int) Math.round(Double.parseDouble(ratingStr.trim())); }
            catch (Exception ignored) {}


            String userRatingStr = extractJsonField(obj, "userRating");
            int userRating = 0;
            try { if (userRatingStr != null && !userRatingStr.equals("null"))
                userRating = Integer.parseInt(userRatingStr.trim());
            } catch (Exception ignored) {}

            NoteEntry entry = new NoteEntry(id, name, size);
            entry.rating = userRating > 0 ? userRating : rating; // prefer user's own rating
            result.add(entry);
        }
        return result;
    }

    private String extractJsonField(String obj, String key) {
        String q = "\"" + key + "\"";
        int idx = obj.indexOf(q);
        if (idx < 0) return null;
        int colon = obj.indexOf(':', idx + q.length());
        if (colon < 0) return null;
        String rest = obj.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end < 0 ? null : rest.substring(1, end);
        }
        int end = rest.indexOf(',');
        return end < 0 ? rest.trim() : rest.substring(0, end).trim();
    }

    @FXML
    private void handleSortButton() {
        sortMenu.show(sortButton, Side.BOTTOM, 0, 4);
    }



    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Notes for " + courseName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files",        "*.pdf"),
                new FileChooser.ExtensionFilter("Word Files",       "*.docx"),
                new FileChooser.ExtensionFilter("PowerPoint Files", "*.pptx")
        );
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                NoteEntry entry = new NoteEntry(file.getName(), file.getName(), file.length());
                notes.add(entry);
                new Thread(() -> {
                    try {
                        String id = uploadNote(file);
                        if (id != null && !id.equals(file.getName())) {
                            entry.id = id;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            updateFileCount();
            rebuildNotesList();
        }
    }



    private void rebuildNotesList() {
        List<NoteEntry> sorted = new ArrayList<>(notes);
        String mode = currentSort;

        if (SORT_HIGHEST_RATED.equals(mode)) {
            sorted.sort(Comparator.comparingInt((NoteEntry n) -> n.rating)
                    .reversed()
                    .thenComparing(Comparator.comparing((NoteEntry n) -> n.uploadedAt).reversed()));
        } else if (SORT_LOWEST_RATED.equals(mode)) {
            sorted.sort(Comparator.comparingInt((NoteEntry n) -> n.rating)
                    .thenComparing(Comparator.comparing((NoteEntry n) -> n.uploadedAt).reversed()));
        } else if (SORT_RECENT.equals(mode)) {
            sorted.sort(Comparator.comparing((NoteEntry n) -> n.uploadedAt, Comparator.reverseOrder()));
        } else {
            sorted.sort(Comparator.comparing((NoteEntry n) -> n.uploadedAt));
        }

        notesContainer.getChildren().removeIf(node -> node != emptyState);

        if (notes.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            for (NoteEntry entry : sorted) {
                notesContainer.getChildren().add(buildNoteRow(entry));
            }
        }
    }



    private HBox buildNoteRow(NoteEntry entry) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(14);
        applyRowStyle(row, false);

        Label iconLabel = new Label(getFileIcon(entry.fileName));
        iconLabel.setStyle("-fx-font-size: 20px;");

        VBox fileInfo = new VBox(4);

        Label nameLabel = new Label(entry.fileName);
        nameLabel.setStyle(
                "-fx-text-fill: #f0f0f0;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 600;"
        );

        Label sizeLabel = new Label(formatFileSize(entry.fileSize));
        sizeLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

        HBox starRow = buildStarRow(entry);

        fileInfo.getChildren().addAll(nameLabel, sizeLabel, starRow);
        HBox.setHgrow(fileInfo, Priority.ALWAYS);

        Button downloadBtn = new Button("⬇");
        styleDownloadBtn(downloadBtn, false);
        downloadBtn.setOnMouseEntered(e -> styleDownloadBtn(downloadBtn, true));
        downloadBtn.setOnMouseExited(e  -> styleDownloadBtn(downloadBtn, false));
        downloadBtn.setOnAction(e -> {
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("Save " + entry.fileName);
            saveChooser.setInitialFileName(entry.fileName);
            Stage stage = (Stage) backButton.getScene().getWindow();
            File dest = saveChooser.showSaveDialog(stage);
            if (dest == null) return;
            new Thread(() -> {
                try {
                    HttpClient client = ApiClient.getClient();
                    HttpRequest request = AppConfig.requestBuilderRaw("/api/notes/download/" + entry.id)
                            .GET()
                            .build();
                    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    System.out.println("Download response: " + response.statusCode());
                    if (response.statusCode() == 200) {
                        Files.write(dest.toPath(), response.body());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        Button deleteBtn = new Button("✕");
        styleDeleteBtn(deleteBtn, false);
        deleteBtn.setOnMouseEntered(e -> styleDeleteBtn(deleteBtn, true));
        deleteBtn.setOnMouseExited(e  -> styleDeleteBtn(deleteBtn, false));
        deleteBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    HttpClient client = ApiClient.getClient();
                    HttpRequest request = AppConfig.requestBuilderRaw("/notes/" + entry.id)
                            .DELETE()
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Delete response: " + response.statusCode() + " " + response.body());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            notes.remove(entry);
            updateFileCount();
            rebuildNotesList();
        });

        row.getChildren().addAll(iconLabel, fileInfo, downloadBtn, deleteBtn);
        row.setOnMouseEntered(ev -> applyRowStyle(row, true));
        row.setOnMouseExited(ev  -> applyRowStyle(row, false));

        return row;
    }



    private HBox buildStarRow(NoteEntry entry) {
        HBox starRow = new HBox(2);
        starRow.setAlignment(Pos.CENTER_LEFT);

        Label[] stars = new Label[5];

        Runnable refreshStars = () -> {
            for (int i = 0; i < 5; i++) {
                if (entry.rating > 0 && i < entry.rating) {
                    stars[i].setText("★");
                    stars[i].setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 15px; -fx-cursor: hand;");
                } else {
                    stars[i].setText("☆");
                    stars[i].setStyle("-fx-text-fill: #444444; -fx-font-size: 15px; -fx-cursor: hand;");
                }
            }
        };

        for (int i = 0; i < 5; i++) {
            Label star = new Label("☆");
            star.setStyle("-fx-text-fill: #444444; -fx-font-size: 15px; -fx-cursor: hand;");
            stars[i] = star;

            final int starIndex = i + 1;

            star.setOnMouseEntered(e -> {
                for (int j = 0; j < 5; j++) {
                    if (j < starIndex) {
                        stars[j].setText("★");
                        stars[j].setStyle("-fx-text-fill: #f39c12; -fx-font-size: 15px; -fx-cursor: hand;");
                    } else {
                        stars[j].setText("☆");
                        stars[j].setStyle("-fx-text-fill: #444444; -fx-font-size: 15px; -fx-cursor: hand;");
                    }
                }
            });

            star.setOnMouseExited(e -> refreshStars.run());

            star.setOnMouseClicked(e -> {
                entry.rating = (entry.rating == starIndex) ? 0 : starIndex;

                new Thread(() -> {
                    try {
                        HttpClient client = ApiClient.getClient();
                        HttpRequest request = AppConfig.requestBuilderRaw(
                                        "/notes/rate/" + entry.id + "?rating=" + entry.rating)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        System.out.println("Rate response: " + response.statusCode() + " " + response.body());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

                rebuildNotesList();
            });

            starRow.getChildren().add(star);
        }

        Label ratingText = new Label();
        ratingText.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");
        ratingText.setText(entry.rating == 0 ? "  Not rated" : "  " + entry.rating + " / 5");
        starRow.getChildren().add(ratingText);

        refreshStars.run();
        return starRow;
    }

    private void updateFileCount() {
        int count = notes.size();
        if (count == 0) {
            fileCountLabel.setText("0 files uploaded");
            fileCountLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");
        } else {
            fileCountLabel.setText(count + " file" + (count == 1 ? "" : "s") + " uploaded");
            fileCountLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 13px;");
        }
    }

    private void applyRowStyle(HBox row, boolean hovered) {
        if (hovered) {
            row.setStyle(
                    "-fx-background-color: #2a2f40;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(46,204,113,0.25);" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-width: 1;" +
                            "-fx-padding: 14 16 14 16;"
            );
        } else {
            row.setStyle(
                    "-fx-background-color: #222736;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(255,255,255,0.08);" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-width: 1;" +
                            "-fx-padding: 14 16 14 16;"
            );
        }
    }

    private void styleDeleteBtn(Button btn, boolean hovered) {
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + (hovered ? "#e74c3c" : "#555555") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 0;"
        );
    }

    private void styleDownloadBtn(Button btn, boolean hovered) {
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + (hovered ? "#2ecc71" : "#555555") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 0;"
        );
    }

    private String getFileIcon(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf"))                            return "📄";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "📝";
        if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) return "📊";
        return "📎";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }



    private String uploadNote(File file) throws Exception {
        HttpClient client = ApiClient.getClient();
        String boundary = "----NustNestBoundary" + System.currentTimeMillis();
        String crlf = "\r\n";

        String mimeType;
        String lowerName = file.getName().toLowerCase();
        if (lowerName.endsWith(".pdf"))
            mimeType = "application/pdf";
        else if (lowerName.endsWith(".docx"))
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        else if (lowerName.endsWith(".pptx"))
            mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        else
            mimeType = "application/octet-stream";

        StringBuilder prefix = new StringBuilder();
        prefix.append("--").append(boundary).append(crlf)
                .append("Content-Disposition: form-data; name=\"title\"").append(crlf).append(crlf)
                .append(file.getName()).append(crlf)
                .append("--").append(boundary).append(crlf)
                .append("Content-Disposition: form-data; name=\"description\"").append(crlf).append(crlf)
                .append("Uploaded from app").append(crlf)
                .append("--").append(boundary).append(crlf)
                .append("Content-Disposition: form-data; name=\"subject\"").append(crlf).append(crlf)
                .append(getSubjectKey(courseName)).append(crlf)
                .append("--").append(boundary).append(crlf)
                .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getName()).append("\"").append(crlf)
                .append("Content-Type: ").append(mimeType).append(crlf).append(crlf);

        byte[] prefixBytes = prefix.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes   = Files.readAllBytes(file.toPath());
        byte[] suffixBytes = (crlf + "--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8);

        byte[] bodyBytes = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, bodyBytes, 0, prefixBytes.length);
        System.arraycopy(fileBytes,   0, bodyBytes, prefixBytes.length, fileBytes.length);
        System.arraycopy(suffixBytes, 0, bodyBytes, prefixBytes.length + fileBytes.length, suffixBytes.length);

        HttpRequest request = AppConfig.requestBuilderRaw("/notes/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Upload response: " + response.statusCode() + " " + response.body());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String responseBody = response.body();
            if (responseBody != null) {
                int idx = responseBody.indexOf("\"id\"");
                if (idx >= 0) {
                    int colon = responseBody.indexOf(':', idx);
                    if (colon > 0) {
                        String tail = responseBody.substring(colon + 1).trim();
                        tail = tail.replaceAll("^[\"\\s]+", "");
                        tail = tail.replaceAll("[\",}].*$", "").trim();
                        if (!tail.isEmpty()) return tail;
                    }
                }
            }
            return file.getName();
        }
        return null;
    }

    private String encodePath(String raw) {
        return URLEncoder.encode(raw == null ? "" : raw, StandardCharsets.UTF_8);
    }

    @FXML
    private void handleBack() {
        destroyed = true;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CourseSelectionScreen.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String getSubjectKey(String courseName) {
        return switch (courseName) {
            case "OOP"                                    -> "CS_212_OOP";
            case "Linear Algebra"                         -> "MATH_121_LINEAR_ALGEBRA_AND_ORDINARY_DIFFERENTIAL_EQUATIONS";
            case "Discrete Maths"                         -> "MATH_161_DISCRETE_MATHEMATICS";
            case "Functional English"                     -> "HU_114_FUNCTIONAL_ENGLISH";
            case "Computer Architecture and Logic Design" -> "EE_122_COMPUTER_ARCHITECTURE_AND_LOGIC_DESIGN";
            case "Understanding of Quran"                 -> "HU_132_UNDERSTANDING_OF_QURAN";
            default                                       -> courseName;
        };
    }
}







