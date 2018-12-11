/**
 * 1. Create Lobby
 * 2. Wait for players to join
 * 3. Players will be assigned an id by the chat server.
 * 4. When all players are connected prepare to start the game.
 * 5. Choose random [(max players) * 3] words from dictionary and put it into static list.
 * 6. Each player will take turns drawing. (modulus)
 * 7. On each round, server will send messages using these formats
 * DRAWER <playerId> if the player id of client is equal to <playerId>, he will be drawer. others will have pens disabled.
 * WORD <word> this is the current word to be guessed. if the word typed on the chat box is equal to this word,
 * chat packet to chat server will not be created. a different message will be sent to the game server
 * MESSAGE <message> when client receives this, client will display this in the text area using maroon font, signifying that
 * the message came from the server.
 * POINTS x,y,x,y,x,y,x,y .... when client receives this, lines should be drawn
 * COLOR <color> when client receives this, change the line color to the sent color
 * <p>
 * 8. Drawer will send a list of points (x,y) to the server. Server will broadcast to players to show drawing.
 * 9. Scoring is as follows: guess within 30 secs, 20 points. Within a minute, 10 points. Scoring will be implemented on the client
 * 10. Drawer is scored by (total points acquired by players / max players) to give incentive to good drawings.
 * 11. If no player guesses correctly, drawer gets -20 penalty.
 */

import proto.TcpPacketProtos.TcpPacket.*;
import proto.TcpPacketProtos.TcpPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Scanner;

public class IllusGameServer implements Runnable {
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private DatagramSocket serverSocket;
    private List<IllusPlayer> players;
    private String lobbyId;
    private int maxPlayers;
    private Time timer;

    private int level;
    private int max_level;

    private String state = "WAITING";

    private int currentPlayerCount = 0;
    private int correctAnswers = 0;

    private Dictionary dictionary;
    private String current_word;

    public IllusGameServer(String serverName, int port, int maxPlayers) throws Exception {
        Socket server = new Socket();
        server.connect(new InetSocketAddress(serverName, port), 10000);

        outputStream = new DataOutputStream(server.getOutputStream());
        inputStream = new DataInputStream(server.getInputStream());
        serverSocket = new DatagramSocket(Constants.GAME_PORT);
        players = new ArrayList<>();
        this.maxPlayers = maxPlayers;
        this.dictionary = new Dictionary();
    }

    private String createLobby(int maxPlayers) throws Exception {
        CreateLobbyPacket createLobbyPacket = CreateLobbyPacket.newBuilder().setType(PacketType.CREATE_LOBBY)
                .setMaxPlayers(maxPlayers).build();

        System.out.println("Creating chat lobby.");
        outputStream.write(createLobbyPacket.toByteArray());

        byte[] buffer = new byte[1024]; //receives/holds data received from server
        int bytes = -1;

        bytes = inputStream.read(buffer); //number of bytes received from server
        byte[] bufferresponse = Arrays.copyOfRange(buffer, 0, bytes); //adjust byte array length depending on the number of bytes received

        TcpPacket packet = null;
        if (bytes > 0) {
            packet = packet.parseFrom(bufferresponse);
        }

        String lobbyId = "";
        if (packet.getType() == PacketType.CREATE_LOBBY) {
            createLobbyPacket = createLobbyPacket.parseFrom(bufferresponse);
            lobbyId = createLobbyPacket.getLobbyId();
            System.out.println("Server responds: " + lobbyId);
            System.out.println("Successfully created lobby with ID " + lobbyId);
        }
        this.lobbyId = lobbyId;
        return lobbyId;
    }

    public void send(IllusPlayer player, String msg) {
        DatagramPacket packet;
        byte buf[] = msg.getBytes();
        packet = new DatagramPacket(buf, buf.length, player.getAddress(), player.getPort());
        try {
            serverSocket.send(packet);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void sendToAll(String msg) {
        for (IllusPlayer player : players) {
            send(player, msg);
        }
    }

    public IllusPlayer getPlayerByName(String name) {
        IllusPlayer player = null;
        for (IllusPlayer temp : players) {
            if (name.equals(temp.getName())) {
                player = temp;
                break;
            }
        }
        return player;
    }

    private void setCurrentWord(){
        this.current_word = dictionary.getAnswer();
    }

    public void start_after_countdown(){
        sendToAll("WORD "+dictionary.getAnswer());
        timer = new Time(60, true);
        timer.startTimer();
        
        sendToAll("START");
    }
    // TIMER
    public class Time {
        Timer timer;
        private int time;
        private Boolean isInGame = false;

        public Time(int time, Boolean isInGame){
            this.isInGame = isInGame;
            this.time = time;
        }
        public Time(int time) {
            this.time = time;
        }

        public void setTimerStatus(){
            this.isInGame = !this.isInGame;
        }
        
        public Boolean getTimerStatus(){
            return this.isInGame;
        }

        public void updateTime() {
            this.time--;
            sendToAll("TIME " + this.time + " " + this.isInGame);

            if(time == 0 && !isInGame){
                start_after_countdown();
            }
            
        }

        public int getTime() {
            return this.time;
        }

        void stopTimer() {
            this.timer.cancel();
            this.timer.purge();
        }

        void startTimer() {
            this.timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    updateTime();
                    if (time == 0)
                        stopTimer();
                }
            }, 0, 1000);
        }
    }

    public static void main(String[] args) {

        /*
        //validate command line args
        if (args.length != 2){
            System.out.println("Usage: java -jar IllusGameServer <lobby id> <max players>");
            System.exit(1);
        }
        int maxPlayers = 0;
        try{
            maxPlayers = Integer.parseInt(args[1]);
        }catch (Exception e){
            System.out.println("Usage: java -jar IllusGameServer <lobby id> <max players>");
            System.out.println("Max players should be a qualified Integer");
            System.exit(1);
        }
        //end validate command line args
*/

        Scanner sc = new Scanner(System.in);
        int maxPlayers = 0;


        do {
            System.out.print("Max no. of players (3-5 only): ");
            maxPlayers = sc.nextInt();
        } while (maxPlayers < 3 || maxPlayers > 5);

        IllusGameServer gameServer = null;
        try {
            gameServer = new IllusGameServer("202.92.144.45", 80, maxPlayers);
        } catch (Exception e) {
            System.out.println("Game server not successfully initialized. See stacktrace below. ");
            e.printStackTrace();
            System.exit(-1);
        }

        String lobbyId = "";
        try {
            lobbyId = gameServer.createLobby(maxPlayers);
        } catch (Exception e) {
            System.out.println("Lobby creation failed. See stacktrace below.");
            e.printStackTrace();
            System.exit(-1);
        }

        Thread thread = new Thread(gameServer);
        thread.start();

    }

    @Override
    public void run() {
        int loadedUI = 0;
        level = 0;
        IllusPlayer drawer = null;
        int timeUp = 0;
        while (true) {

            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                serverSocket.receive(packet);
            } catch (Exception ioe) {
            }

            /**
             * Convert the array of bytes to string
             */
            String data = new String(buf);

            //remove excess bytes
            data = data.trim();

            String[] dataArray;

            switch (state) {
                case "WAITING":
                    if (data.startsWith("CONNECT")) {
                        dataArray = data.split(" ");
                        IllusPlayer player = new IllusPlayer();
                        player.setName(dataArray[1]);
                        player.setAddress(packet.getAddress());
                        player.setPort(packet.getPort());
                        players.add(player);
                        currentPlayerCount++;

                        String message = "SUCCESS " + lobbyId;
                        System.out.println("PLAYER " + player.getName() + " is now connected.");
                        send(player, message);

                        if (currentPlayerCount == maxPlayers) {
                            state = "INIT_LEVEL";
                            message = "PLAYER_COUNT_MET";
                            max_level = maxPlayers * 3;
                            sendToAll(message);
                        }
                    }
                    break;
                case "INIT_LEVEL":
                    dataArray = data.split(" ");

                    if (data.startsWith("START")) {
                        loadedUI++;

                        if (loadedUI == maxPlayers) {

                            state = "RUNNING";
                            drawer = players.get(level % maxPlayers);
                            System.out.println("\nPLAYER " + drawer.getName() + " will draw for this level");
                            sendToAll("DRAWER " + drawer.getName() + " ");
                            level++;
                            correctAnswers = 0;
                            timeUp = 0;

                            String wordChoices = dictionary.getWords();

                            send(drawer, "CHOOSE " + wordChoices);

                        }
                    }
                    break;
                case "RUNNING":
                    dataArray = data.split(" ");
                    if (data.startsWith("CLEAR")) {
                        sendToAll("CLEAR CANVAS");
                    } else if (data.startsWith("PAINT")) {

                        sendToAll("PAINT " + dataArray[1] + " " + dataArray[2] + " " + dataArray[3]);

                    } else if (data.startsWith("ANSWER")) {
                        String playerName = dataArray[1].trim();
                        String guess = dataArray[2].trim();

                        System.out.println("\n[!] Received PLAYER " + playerName + " answer: " + guess);

                        //check answer here
                        if (dictionary.validateWord(guess)) {
                            
                            for(IllusPlayer player : players){
                                if(player.getName().equals(playerName)){
                                    // increment player score
                                    if(timer.getTime() > 30){
                                        player.setScore(player.getScore() + 20);
                                    }
                                    else{
                                        player.setScore(player.getScore() + 10);
                                    }

                                }
                            }
                            sendToAll("CORRECT_ANSWER " + playerName + " " + dictionary.getAnswer());
                            
                            updateScores();
                            timeUp++;
                            correctAnswers++;
                            
                           
                            // end of level
                            if(correctAnswers == (maxPlayers-1)){
                                for(IllusPlayer player : players){
                                    // drawer earns points
                                    if(player.getName().equals(drawer.getName())){
                                        player.setScore(player.getScore() + 10);
                                    }
                                    
                                }


                                //TODO: if level == max levels - 1, end game
                                timer.stopTimer();
                                if(level >= max_level-1){
                                    System.out.println("END GAME");
                                    for(IllusPlayer player : players)
                                        send(player, "END_GAME " + player.getScore());
                                }else{
                                    drawer = players.get(level % maxPlayers);
                                    level++;
                                    System.out.println("\nPLAYER " + drawer.getName() + " will draw for level (" + level + "/" + max_level + ")");
                                    sendToAll("DRAWER " + drawer.getName() + " ");
                                    timeUp = 0;
                                    correctAnswers = 0;
                                    dictionary = new Dictionary();
                                    String wordChoices = dictionary.getWords();
                                    send(drawer, "CHOOSE " + wordChoices);
                                }
                            }
                        }
                    } else if (data.startsWith("CHOICE")) {
                        //drawer finalizes word to guess
                        String choice = dataArray[1].trim();
                        System.out.println("\n[!] Drawer chose " + choice);
                        sendToAll("MESSAGE Player " + drawer.getName() + " will draw for this level.");
                        dictionary.setAnswer(choice);
                        setCurrentWord();
                        sendToAll("WORD " + choice);

                        updateScores();

                        // server side timer initialization
                        if(timer != null){
                            timer.stopTimer();
                        }
                        sendToAll("CLEAR CANVAS");
                        timer = new Time(4);
                        timer.startTimer();
   
                    }
                    else if(data.startsWith("TIME_UP")){
                        System.out.println("time up");
                        for(IllusPlayer player : players){
                            send(player, "REVEAL " + current_word);
                        }
                        timeUp++;
                        if(timeUp == maxPlayers -1){
                            if(correctAnswers == 0){
                                for(IllusPlayer player : players){
                                    if(player.getName().equals(drawer)){
                                        player.setScore(player.getScore() - 10);
                                    }
                                }
                            }
                            drawer = players.get(level % maxPlayers);
                            level++;
                            correctAnswers = 0;
                            timeUp = 0;
                            //TODO: if level == max levels - 1, end game
                            if(level >= max_level-1){
                                System.out.println("END GAMETIME UP");
                                for(IllusPlayer player : players)
                                    send(player, "END_GAME " + player.getScore());
                            }else{ 
                                System.out.println("\nPLAYER " + drawer.getName() + " will draw for level (" + level + "/" + max_level + ")");
                                sendToAll("DRAWER " + drawer.getName() + " ");
                                dictionary = new Dictionary();
                                String wordChoices = dictionary.getWords();
                                send(drawer, "CHOOSE " + wordChoices);
                            }
                        }
                    }
                    break;
            }

            // System.out.println("Current state: " + state);
            continue;
        }

    }

    private void updateScores(){
        StringBuilder sb = new StringBuilder("SCORES ");
        for(int i = 0; i < players.size(); i++){
            IllusPlayer player = players.get(i);
            sb.append(player.getName() + "," + player.getScore());
            if (i < (players.size() - 1)){
                sb.append(",");
            }
        }
        sendToAll(sb.toString());
    }
}
