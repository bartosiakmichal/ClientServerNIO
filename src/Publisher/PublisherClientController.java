

package Publisher;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public class PublisherClientController {

    @FXML
    private ComboBox<String> topicsComboBox;
    @FXML
    private TextArea topicsLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private ListView topicsListView;
    @FXML
    private TreeView<String> treeView;

    private PublisherClient pubClient = new PublisherClient();
    private String username;
    private List<String> listOfTopics = new ArrayList<>();
    private HashMap<String, HashMap<Integer, String>> listOfNews = new HashMap<>();
    private String comboBoxTxt = "";
    private String textAreaTxt;

    @FXML
    public void initialize() {

        boolean userLogin = true;
        while (userLogin) {

            loginWindow();

            String topicsInString = pubClient.getServerResponse("initializeAdmin", username, "_");

            if (!topicsInString.equals("userExist")) {

                System.out.println(topicsInString);
                pubClient.doTopicsList(topicsInString);

                pubClient.updateNews();

                listOfTopics = pubClient.getListOfTopics();
                listOfNews = pubClient.getListOfNews();

                setData();
                setTreeNews();
                break;

            } else {
                alertMsg("Admin o podanym loginie jest już zalogowany");
                username = "";
                Platform.exit();
                break;
            }
        }
    }

    public void shutdown() {
        if (!username.equals("")) {
            System.out.println("Stop klient:" + username);
            String subTopicsInString = pubClient.getServerResponse("shutdownClient", username, " ");
        }
    }

    public void loginWindow() {
        TextInputDialog dialog = new TextInputDialog("Admin");
        dialog.setHeaderText("Enter your name");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> nameLabel.setText(name));
        username = nameLabel.getText();
    }

    public void alertMsg(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    void processAddNews(ActionEvent event) {
        comboBoxTxt = topicsComboBox.getValue();
        textAreaTxt = topicsLabel.getText();

        String newsInString = pubClient.getServerResponse("addNews", username, comboBoxTxt + "{=}" + textAreaTxt);
        pubClient.setListOfNews(newsInString);

        listOfNews = pubClient.getListOfNews();
        updateTreeView();
    }

    @FXML
    void processAddTopic(ActionEvent event) {
        textAreaTxt = topicsLabel.getText();

        if (!listOfTopics.contains(textAreaTxt) && !textAreaTxt.equals("")) {
            String topicsInString = pubClient.getServerResponse("addTopic", username, textAreaTxt);
            pubClient.doTopicsList(topicsInString);

            listOfTopics = pubClient.getListOfTopics();
            updateTopicsList();
        }
    }

    @FXML
    void processRemoveTopic(ActionEvent event) {
        comboBoxTxt = topicsComboBox.getValue();

        if (listOfTopics.contains(comboBoxTxt)) {
            String topicsInString = pubClient.getServerResponse("removeTopic", username, comboBoxTxt);
            pubClient.removeTopic(comboBoxTxt, topicsInString);

            listOfTopics = pubClient.getListOfTopics();
            listOfNews = pubClient.getListOfNews();
            updateTopicsList();
            updateTreeView();
        }
    }


    public void setTreeNews() {
        HashMap<Integer, String> news = new HashMap<>();
        TreeItem<String> root, child;
        root = new TreeItem<>();
        root.setExpanded(true);

        for (String name : listOfNews.keySet()) {
            String title = name;

            child = makeBranch(title, root);

            news = listOfNews.get(title);
            for (Integer key : news.keySet()) {
                String content = news.get(key);
                makeBranch(content, child);
            }
        }
        treeView.setRoot(root);
        treeView.setShowRoot(false);
        treeView.getSelectionModel().selectedItemProperty()
                .addListener((v, oldValue, newValue) -> {
                    if (newValue != null) {
                        topicsLabel.setText(newValue.getValue());
                    }
                });
    }

    public TreeItem<String> makeBranch(String title, TreeItem<String> parent) {
        TreeItem<String> item = new TreeItem<>(title);
        item.setExpanded(true);
        parent.getChildren().addAll(item);
        return item;
    }

    private void updateTreeView() {
        treeView.getRoot().getChildren().clear();
        setTreeNews();
    }

    public void updateTopicsList() {
        topicsComboBox.getItems().clear();

        for (String topic : listOfTopics) {
            topicsComboBox.getItems().addAll(topic);
            topicsComboBox.setValue(topic);
        }

        topicsListView.getItems().clear();
        topicsListView.getItems().addAll(listOfTopics);

    }

    public void setData() {

        for (String topic : listOfTopics) {
            topicsComboBox.getItems().addAll(topic);
            topicsComboBox.setValue(topic);
        }

        String stringListOfTopic = "";

        for (int i = 0; i < listOfTopics.size(); i++) {
            stringListOfTopic += listOfTopics.get(i) + ";";
        }
        topicsListView.getItems().addAll(listOfTopics);

        String setSampleData = pubClient.getServerResponse("setData", username, stringListOfTopic);


    }

}
