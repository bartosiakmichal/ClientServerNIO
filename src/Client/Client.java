
package Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Client extends Application {
    private int port = 1300;
    private String host = "127.0.0.1";
    private SocketChannel socketChannel = null;
    private static Pattern pattern = Pattern.compile(";");

    public void setListOfTopics(List<String> listOfTopics) {
        this.listOfTopics = listOfTopics;
    }

    private List<String> listOfTopics;
    private List<String> listOfSubTopic;
    private HashMap<String, HashMap<Integer, String>> listOfNews;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("client.fxml"));
        Parent root = loader.load();
        ClientController controller = loader.getController();
        primaryStage.setTitle("Klient");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.setOnHidden(e -> {
            try {
                controller.shutdown();
                socketChannel.close();
                Platform.exit();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        primaryStage.show();
    }

    public Client() {
        getServiceConnections();
    }

    protected void getServiceConnections() {
        try {
            InetSocketAddress address = new InetSocketAddress(host, port);
            socketChannel = SocketChannel.open(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void writeResp(SocketChannel sc, String addMsg) {
        try {
            CharBuffer buffer = CharBuffer.allocate(1024);
            buffer.put(addMsg).flip();
            sc.write(StandardCharsets.UTF_8.encode(buffer));
            System.out.println("[C] Klient wysyła zapytanie: " + host + ":" + socketChannel.getLocalAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String readData(SocketChannel sc) {
        String result = "";
        try {
            ByteBuffer resBuffer = ByteBuffer.allocate(1024);
            sc.read(resBuffer);
            resBuffer.flip();
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(resBuffer);
            result = new String(charBuffer.array());
//            System.out.println("[S] Serwer: " + sc.getRemoteAddress());
            resBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getServerResponse(String cmd, String username, String msg) {
        String request = cmd + "_" + username + "_" + msg;
        writeResp(socketChannel, request);
        System.out.println("[C] Zapytanie klienta: " + request);
        String response = readData(socketChannel);
        System.out.println("[S] Odpowiedź serwera: " + response);
        return response;
    }

    public List<String> getListOfTopics() {
        return listOfTopics;
    }

    public HashMap<String, HashMap<Integer, String>> getListOfNews() {
        return listOfNews;
    }

    public void setListOfSubTopic(List<String> listOfSubTopic) {
        this.listOfSubTopic = listOfSubTopic;
    }

    public List<String> getListOfSubTopic() {
        return listOfSubTopic;
    }

    public void doSubTopicsList(String topicsInString) {
        listOfSubTopic = new ArrayList<>();
        listOfSubTopic = Arrays.asList(pattern.split(topicsInString));
    }

    public void doTopicsList(String topicsInString) {
        listOfTopics = new ArrayList<>();
        listOfTopics = Arrays.asList(pattern.split(topicsInString));
    }

    public void setListOfNews(String str) {
        listOfNews = new HashMap<>();
        List<String> items = Arrays.asList(str.split("\\{#}"));

        for (int i = 0; i < items.size(); i++) {
            String[] array = new String[2];
            Pattern pattern = Pattern.compile("\\{=}");
            array = pattern.split(items.get(i));
            if (array.length == 2) {
                String catTopic = array[0];
                String newsContent = array[1];
                addNews(catTopic, newsContent);
            }
        }
    }

    public void addNews(String catTopic, String newsContent) {
        HashMap<Integer, String> news = new HashMap<>();
        int tmp = 1;

        if (listOfNews.containsKey(catTopic)) {
            news = listOfNews.get(catTopic);
            tmp = Collections.max(news.keySet()) + 1;
        } else {
            tmp = 1;
        }
        news.put(tmp, newsContent);
        listOfNews.put(catTopic, news);
    }

}
