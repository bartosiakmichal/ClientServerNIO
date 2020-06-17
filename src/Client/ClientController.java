
package Client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.*;
import java.util.regex.Pattern;

public class ClientController {

    @FXML
    private ListView subTopicListView;
    @FXML
    protected ComboBox<String> topicsComboBox;
    @FXML
    private TextArea topicsLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private TreeView<String> treeView;

    private Client client = new Client();
    private String comboBoxTxt = "";
    private List<String> listOfTopics;
    private List<String> listOfSubTopic = new ArrayList<>();
    private String username;
    private HashMap<String, HashMap<Integer, String>> listOfNews = new HashMap<>();
    private static Pattern pattern = Pattern.compile(";");

    @FXML
    public void initialize() {

        boolean userLogin = true;
        while (userLogin) {

            loginWindow();
            String topicsInString = client.getServerResponse("initializeClient", username, "_");
            if (!topicsInString.equals("userExist")) {

                System.out.println(topicsInString);
                List<String> list = Arrays.asList(pattern.split(topicsInString));
                client.setListOfTopics(list);

                listOfTopics = client.getListOfTopics();
                listOfNews = client.getListOfNews();

                setData();
                userLogin = false;
            } else {
                alertMsg();
                username = "";
            }
        }

    }

    public void loginWindow() {
        TextInputDialog dialog = new TextInputDialog("User1");
        dialog.setHeaderText("Enter your name");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> nameLabel.setText(name));
        username = nameLabel.getText();
    }


    @FXML
    void processSubscribe(ActionEvent event) {
        comboBoxTxt = topicsComboBox.getValue();

        if (!listOfSubTopic.contains(comboBoxTxt)) {
            String topicsInString = client.getServerResponse("subscribe", username, comboBoxTxt);
            client.doSubTopicsList(topicsInString);

            listOfSubTopic = client.getListOfSubTopic();
            updateSubList();
        }
    }

    @FXML
    void processUnsubscribe(ActionEvent event) {
        comboBoxTxt = topicsComboBox.getValue();

        if (listOfSubTopic.contains(comboBoxTxt)) {
            String topicsInString = client.getServerResponse("unsubscribe", username, comboBoxTxt);
            client.doSubTopicsList(topicsInString);

            listOfSubTopic = client.getListOfSubTopic();
            updateSubList();
        }
    }

    @FXML
    void processUpdate(ActionEvent event) {

        String newsInString = client.getServerResponse("updateClientNews", username, " ");
        client.setListOfNews(newsInString);

        String topicsInString = client.getServerResponse("updateTopics", username, " ");
        client.doTopicsList(topicsInString);

        String subTopicsInString = client.getServerResponse("updateSubTopics", username, " ");
        client.doSubTopicsList(subTopicsInString);

        listOfNews = client.getListOfNews();
        listOfTopics = client.getListOfTopics();
        listOfSubTopic = client.getListOfSubTopic();

        updateTreeView();
        updateTopicsList();
        updateSubList();
    }

    public void alertMsg() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText("Użytkownik o takiej nazwie już istnieje");
        alert.showAndWait();
    }

    public void shutdown() {

        if (!username.equals("")) {
            System.out.println("Stop klient:" + username);
            String subTopicsInString = client.getServerResponse("shutdownClient", username, " ");
        }
    }

    public void addItems(String topic) {
        listOfTopics.add(topic);
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
                //System.out.println(content);

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

    public void updateSubList() {
        subTopicListView.getItems().clear();
        subTopicListView.getItems().addAll(listOfSubTopic);
    }

    private void updateTreeView() {
        if (treeView.getRoot() != null) {
            treeView.getRoot().getChildren().clear();
        }
        setTreeNews();
    }

    public void updateTopicsList() {
        topicsComboBox.getItems().clear();

        for (String topic : listOfTopics) {
            topicsComboBox.getItems().addAll(topic);
            topicsComboBox.setValue(topic);
        }
    }

    public void setData() {

        for (String topic : listOfTopics) {
            topicsComboBox.getItems().addAll(topic);
            topicsComboBox.setValue(topic);
        }

        int randomNum = (int) (Math.random() * ((2 - 0) + 1)) + 0;
        String stringListOfTopic = "";
        for (int i = 0; i < listOfTopics.size() - randomNum; i++) {
            String tmp = listOfTopics.get(i);
            stringListOfTopic += tmp + ";";
            listOfSubTopic.add(tmp);
        }

        subTopicListView.getItems().addAll(listOfSubTopic);

        String setSampleData = client.getServerResponse("setData", username, stringListOfTopic);
//        System.out.println(setSampleData);

        client.setListOfSubTopic(listOfSubTopic);

    }
}
