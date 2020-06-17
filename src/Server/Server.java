package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {
    private int port = 1300;
    private String host = "127.0.0.1";
    private ServerSocketChannel serverSocketChannel = null;
    private Selector selector = null;

    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private StringBuffer reqString = new StringBuffer();

    private ArrayList<String> listOfTopic = new ArrayList<>();
    private ArrayList<String> listOfSubTopic;
    private HashMap<String, ArrayList<String>> subscribesMap = new HashMap<>();

    private HashMap<String, HashMap<Integer, String>> listOfNews = new HashMap<>();
    private HashMap<String, HashMap<Integer, String>> listClientsNews;

    private Pattern pattern = Pattern.compile(";");


    public static void main(String[] args) {
        Server server = new Server();
    }

    public Server() {
        try {
            loadData();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.socket().bind(new InetSocketAddress(host, port));
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.printf("[S] Start serwera: %s:%d\n", host, port);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        serviceConnections();
    }

    private void serviceConnections() {

        while (true) {
            try {

                selector.select();

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        continue;
                    } else if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        serviceRequest(socketChannel);
                        continue;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

        }
    }

    private void serviceRequest(SocketChannel socketChannel) {
        if (!socketChannel.isOpen()) return;

        reqString.setLength(0);
        buffer.clear();
        try {
            boolean tmp = true;
            readLoop:
            while (tmp) {
                int n = socketChannel.read(buffer);
                if (n > 0) {
                    buffer.flip();
                    CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
                    while (charBuffer.hasRemaining()) {
                        char c = charBuffer.get();
                        if (c == '\r' || c == '\n') break readLoop;
                        reqString.append(c);
                    }
                } else tmp = false;
            }
            Pattern reqPatt = Pattern.compile("_", 3);
            String[] req = reqPatt.split(reqString, 3);
            String cmd = req[0];
            String clientName = req[1];
            String msg = req[2];
            System.out.println("[C/P] Orzymana wiadomość: " + reqString);

            switch (cmd) {
                case "initializeAdmin": {
                    String str = "";
                    if (!subscribesMap.containsKey(clientName)) {
                        str = encodeListOfTopic(listOfTopic);
                    } else {
                        str = "userExist";
                        System.out.println("Admin jest już zalogowany");
                    }
                    writeResp(socketChannel, str);
                    break;
                }
                //>>ADMIN
                case "addTopic": {
                    addTopic(msg);
                    String str = encodeListOfTopic(listOfTopic);
                    writeResp(socketChannel, str);
                    break;
                }
                case "removeTopic": {
                    removeTopic(msg);
                    String str = encodeListOfTopic(listOfTopic);
                    writeResp(socketChannel, str);
                    break;
                }
                case "addNews": {
                    String[] array;
                    Pattern pattern = Pattern.compile("\\{=}");
                    array = pattern.split(msg);
                    if (array.length == 2) {
                        String catTopic = array[0];
                        String newsContent = array[1];
                        addNews(catTopic, newsContent);
                    }
                    writeResp(socketChannel, encodeNewsMap(listOfNews));
                    break;
                }
                //<<ADMIN
                //>>CLIENT
                case "initializeClient": {
                    String str = "";
                    if (!subscribesMap.containsKey(clientName)) {
                        str = encodeListOfTopic(listOfTopic);
                    } else {
                        str = "userExist";
                        System.out.println("Podany klient jest już zalogowany");
                    }
                    writeResp(socketChannel, str);
                    break;
                }
                case "subscribe": {
                    addClientSubscribe(clientName, msg);
                    String str = encodeListOfTopic(giveClientSubList(clientName));
                    writeResp(socketChannel, str);
                    break;
                }
                case "unsubscribe": {
                    removeClientSubscribe(clientName, msg);
                    String str = encodeListOfTopic(giveClientSubList(clientName));
                    writeResp(socketChannel, str);
                    break;
                }
                case "updateClientNews": {
                    updateClientsNews(clientName);
                    String str = encodeNewsMap(listClientsNews);
                    writeResp(socketChannel, str);
                    break;
                }
                case "updateTopics": {
                    writeResp(socketChannel, encodeListOfTopic(listOfTopic));
                    break;
                }
                case "updateSubTopics": {
                    updateClientsSubTopicsList(clientName);
                    writeResp(socketChannel, encodeListOfTopic(listOfSubTopic));
                    break;
                }
                case "shutdownClient": {
                    shutdownClient(clientName);
                    //writeResp(socketChannel, "Klient " + clientName + " usunięty z serwera");
                    System.out.println(subscribesMap);
                    break;
                }
                //<<CLIENT
                case "updateNews": {
                    addClientSampleData(clientName, msg);
                    writeResp(socketChannel, encodeNewsMap(listOfNews));
                    break;
                }
                case "setData": {
                    addClientSampleData(clientName, msg);
                    System.out.println("mapa subskrypcji" + subscribesMap);
                    writeResp(socketChannel, "setData");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeResp(SocketChannel sc, String addMsg) throws IOException {
        StringBuffer remsg = new StringBuffer();
        if (addMsg != null)
            remsg.append(addMsg);
        else
            remsg.append("null");
        ByteBuffer buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(remsg));
        sc.write(buf);
        buf.clear();
        System.out.println("[S] Wysyłana wiadomość: " + remsg);

    }

    private void addClientSubscribe(String clientName, String msg) {
        if (!subscribesMap.isEmpty()) {
            if (subscribesMap.containsKey(clientName)) {
                List<String> list = subscribesMap.get(clientName);
                if (!list.contains(msg)) {
                    list.add(msg);
                    subscribesMap.put(clientName, new ArrayList<>(list));
                }
            }
        }
    }

    private ArrayList<String> giveClientSubList(String clientName) {
        if (subscribesMap.containsKey(clientName)) {
            ArrayList<String> list = subscribesMap.get(clientName);
            return list;
        }
        return null;
    }

    private void removeClientSubscribe(String clientName, String msg) {
        if (!subscribesMap.isEmpty()) {
            if (subscribesMap.containsKey(clientName)) {
                List<String> list = subscribesMap.get(clientName);
                if (list.contains(msg)) {
                    list.remove(msg);
                    subscribesMap.put(clientName, new ArrayList<>(list));
                }
            }
        }
    }

    private void updateClientsSubTopicsList(String clientName) {
        listOfSubTopic = new ArrayList<>();
        listOfSubTopic = subscribesMap.get(clientName);
        for (int i = 0; i < listOfSubTopic.size(); i++) {
            String clientSubTopic = listOfSubTopic.get(i);
            if (!listOfTopic.contains(clientSubTopic)) {
                subscribesMap.get(clientName).remove(clientSubTopic);
            }
        }
    }

    private void updateClientsNews(String clientName) {
        listClientsNews = new HashMap<>();
        if (!subscribesMap.isEmpty()) {
            ArrayList<String> list = subscribesMap.get(clientName);

            for (String clientSubTopic : list) {
                if (!listClientsNews.containsKey(clientSubTopic)) {
                    for (String name : listOfNews.keySet()) {
                        if (clientSubTopic.equals(name)) {
                            listClientsNews.put(name, listOfNews.get(name));
                        }
                    }
                }
            }
        }
    }

    private void shutdownClient(String clientName) {
        if (subscribesMap.containsKey(clientName)) {
            subscribesMap.remove(clientName);
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

    public static String encodeNewsMap(HashMap<String, HashMap<Integer, String>> mapToEncode) {
        String result = "";
        HashMap<Integer, String> news = new HashMap<>();
        if (!mapToEncode.isEmpty()) {
            for (String name : mapToEncode.keySet()) {
                String topic = name;
                news = mapToEncode.get(topic);
                for (Integer key : news.keySet()) {
                    String newsArticle = news.get(key).toString();
                    result += topic + "{=}" + newsArticle + "{#}";
                }
            }
        } else {
            result += " " + "{=}" + " " + "{#}";
        }
        return result;
    }

    private void addTopic(String msg) {
        listOfTopic.add(msg);
    }

    public void removeTopic(String topic) {
        listOfTopic.remove(topic);
        listOfNews.remove(topic);
    }

    private String encodeListOfTopic(ArrayList<String> list) {
        String result = "";
        if (list.size() > 0) {
            for (String topic : list) {
                result += topic + ";";
            }
            return result;
        } else {
            result += " ; ";
        }
        return result;
    }

    private void addClientSampleData(String clientName, String msg) {
        ArrayList<String> subTopic =
                Stream.of(msg.split(";"))
                        .collect(Collectors.toCollection(ArrayList<String>::new));
        subscribesMap.put(clientName, subTopic);
    }

    private void loadData() {
        listOfTopic.add("Polityka");
        listOfTopic.add("Sport");
        listOfTopic.add("Muzyka");


        addNews("Polityka", "Wybory w czerwcu");
        addNews("Polityka", "Po miesiącach oczekiwania Michael Cohen, były prawnik i „człowiek do wszystkiego” prezydenta Donalda Trumpa, zeznawał wczoraj przed Kongresem.");
        addNews("Muzyka", "Koncert Kazika w Warszawie");
        addNews("Muzyka", "Do sieci właśnie trafił utwór \"Niebo co dzień\", który jest pierwszą zapowiedzią wspólnego albumu Budki Suflera i Felicjana Andrzejczaka.");
        addNews("Sport", "Rozgrywki Premier League, które zostały przerwane z powodu pandemii koronawirusa, mają zostać wznowione w dniach 19-21 czerwca. 17 czerwca natomiast odbędą się dwa zaległe spotkania.");

    }
}
