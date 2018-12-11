import proto.PlayerProtos;
import proto.TcpPacketProtos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Timer;
import java.util.Scanner;
import java.util.Collections;

/**
 * 1. Connect to lobby
 * 2. Run chat sender / server update listener threads.
 * 3. Parse messages from server.
 * 4. Run instance of Main UI.
 */

public class GameClient {
    private JFrame window;
    private Runnable gameServerListener;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private String playerName;
    private DatagramSocket gameServerSocket = new DatagramSocket();
    private static ChatSendThread senderThread;
    private static ChatReceiveThread receiveThread;
    private Time timer;
    private static TextArea chatArea;
    private static TextArea scores;
    private static JLabel wordLabel;
    private static JCheckBox answerCheckbox;
    private String chatLobbyId;
    private MainWindow mainWindow;
    private Boolean isDrawer = true;
    private Boolean isAnswer = false;

    private static Boolean initUI = false;

    private StartInterface startUI;
    private static String serverAddress;

    protected void setCurrentWord(String word) {
        wordLabel.setText(word.toUpperCase());
    }

    protected String getCurrentWord() {
       return wordLabel.getText();
    }


    public GameClient(int port) throws Exception {
        startUI = new StartInterface("CMSC 137 Project");

        while(!startUI.isSubmit()){
            System.out.print("");
        }

        System.out.println(startUI.isSubmit());

        String playerName = startUI.getPlayerName();

        try {
            connectToGame(playerName);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        Socket server = new Socket();
        server.connect(new InetSocketAddress("202.92.144.45", 80), 10000);

        outputStream = new DataOutputStream(server.getOutputStream());
        inputStream = new DataInputStream(server.getInputStream());

        try {
            this.playerName = playerName;
            connectToLobby(chatLobbyId);


        } catch (Exception e) {
            System.out.println("Connecting to lobby failed. See stacktrace below.");
            e.printStackTrace();
            System.exit(-1);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.submit(gameServerListener);

        while (true) {
            System.out.print("");
            if (getInitUI()) {
                startUI.dispose();
                System.out.println("Players completed");
                System.out.println("[!] Initializing UI");
                send("START ");
                break;
            }
        }

        senderThread = new ChatSendThread(outputStream, this);
        receiveThread = new ChatReceiveThread(inputStream, this);

        executorService.submit(receiveThread);
        executorService.submit(senderThread);

        window = new JFrame("Illus (Player: " + this.playerName + ")");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(new Dimension(500, 500));
        window.setLayout(new BorderLayout());
        window.add(mainWindow = new MainWindow());
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void send(String msg) {
        try {
            // System.out.println(serverAddress);
            byte[] buf = msg.getBytes();
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Constants.GAME_PORT);
            gameServerSocket.send(packet);
        } catch (Exception e) {
        }
    }

    private void connectToLobby(String lobbyId) throws Exception {
        PlayerProtos.Player player = PlayerProtos.Player.newBuilder().setName(this.playerName).build();

        System.out.println("Connecting to chat lobby: " + lobbyId);

        TcpPacketProtos.TcpPacket.ConnectPacket connectPacket = TcpPacketProtos.TcpPacket.ConnectPacket.newBuilder().setUpdate(TcpPacketProtos.TcpPacket.ConnectPacket.Update.NEW)
                .setLobbyId(lobbyId).setType(TcpPacketProtos.TcpPacket.PacketType.CONNECT).setPlayer(player).build();

        outputStream.write(connectPacket.toByteArray());

        byte[] buffer = new byte[1024]; //receives/holds data received from server
        int bytes = -1;

        bytes = inputStream.read(buffer); //number of bytes received from server
        byte[] bufferResponse = Arrays.copyOfRange(buffer, 0, bytes); //adjust byte array length depending on the number of bytes received

        TcpPacketProtos.TcpPacket packet = null;
        if (bytes > 0) {
            packet = packet.parseFrom(bufferResponse);
            if (packet.getType() == TcpPacketProtos.TcpPacket.PacketType.CONNECT) {
                TcpPacketProtos.TcpPacket.ConnectPacket response = TcpPacketProtos.TcpPacket.ConnectPacket.parseFrom(bufferResponse);
                System.out.println("Successfully connected to lobby with ID " + lobbyId);
            }

        }
    }

    private Thread ticker = new Thread() {
        @Override
        public void run() {
            send("TICK");
        }
    };

    public String getPlayerName() {
        return this.playerName;
    }

    protected void sendAnswer(String answer) {
        if (!isDrawer)
            send("ANSWER " + this.playerName + " " + answer);
    }

    public static void appendMessage(String message) {
        chatArea.append("\n" + message);
        chatArea.setCaretPosition(chatArea.getText().length() - 1);
    }

    public static void setInitUI(Boolean value) {
        GameClient.initUI = value;

    }

    public Boolean getInitUI() {

        return GameClient.initUI;
    }

    private void connectToGame(String playerName) throws Exception {
        Boolean connected = false;

        int count = 0;
        while (!connected) {
            send("CONNECT " + playerName);
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                gameServerSocket.receive(packet);
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
            String connectionStatus = new String(packet.getData());

            if (connectionStatus.startsWith("SUCCESS")) {
                connected = true;

                System.out.println("Connection Status");
                System.out.println(connectionStatus);
                String[] data = connectionStatus.split("[^a-zA-Z0-9']+");
                this.chatLobbyId = data[1];
            }

        }

        gameServerListener = new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        gameServerSocket.receive(packet);
                        if (packet != null) {
                            String serverMessage = new String(packet.getData());
                            if (serverMessage.startsWith("PLAYER_COUNT_MET")) {
                                setInitUI(true);

                            } else {
                                if (serverMessage.startsWith("MESSAGE")) {
                                    String data = serverMessage.substring(8);

                                    appendMessage(data);
                                }

                                if (serverMessage.startsWith("WORD")) {
                                    String data = serverMessage.split(" ")[1].trim();

                                    String word = data.trim();
                                    if (isDrawer) {
                                        setCurrentWord(word);
                                        answerCheckbox.setVisible(false);
                                    } else {
                                        String changed = String.join("", Collections.nCopies(word.length(), "\u25A1"));
                                        setCurrentWord(changed);
                                        answerCheckbox.setVisible(true);
                                        isAnswer = true;
                                    }
                                }
                                if (serverMessage.startsWith("START")) {
                                    if(!isDrawer){
                                        answerCheckbox.setVisible(true);
                                        isAnswer = false;
                                    }
                                }
                                if (serverMessage.startsWith("CORRECT_ANSWER")) {
                                    String data = serverMessage.split(" ")[1].trim();
                                    String word = serverMessage.split(" ")[2].trim();

                                    appendMessage("PLAYER " + data + " got it CORRECTLY!");

                                    if (data.equals(playerName)) {
                                        System.out.println("[!] Answer DISABLED");
                                        answerCheckbox.setVisible(false);
                                        isAnswer = false;
                                        setCurrentWord(word);
                                    }
                                }
                                if (serverMessage.startsWith("SCORES")) {
                                    String data = serverMessage.split(" ")[1].trim();

                                    //update scores
                                    String[] score = data.split(",");

                                    StringBuilder sb = new StringBuilder("SCORES: \n");
                                    for(int i = 0; i < score.length; i++){
                                        sb.append(score[i]);
                                        if(i % 2 == 0) sb.append(": ");
                                        if(i % 2 == 1) sb.append("\n");
                                    }
                                    scores.setText(sb.toString());
                                }
                                if (serverMessage.startsWith("DRAWER")) {
                                    String data = serverMessage.split(" ")[1].trim();

                                    if (!data.equals(playerName)) {
                                        //disable pen
                                        isDrawer = false;
                                    } else
                                        isDrawer = true;
                                }
                                
                                if (serverMessage.startsWith("PAINT")) {

                                    String[] dataArray = serverMessage.split(" ");
                                    int x = (int) Double.parseDouble(dataArray[2]);
                                    int y = (int) Double.parseDouble(dataArray[3]);

                                    Point point = new Point(x, y);

                                    String color = dataArray[1];
                                    mainWindow.getCanvas().drawDot(point, color);
                                }
                                if (serverMessage.startsWith("CLEAR")) {
                                    //clear
                                    mainWindow.getCanvas().clearCanvas();
                                }
                                if (serverMessage.startsWith("CHOOSE")) {
                                    String[] dataArray = serverMessage.split(" ");
                                    String[] options = {dataArray[1], dataArray[2], dataArray[3]};

                                    int finalChoice = JOptionPane.showOptionDialog(null, "Choose a word", "Drawer Choice", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

                                    send("CHOICE " + options[finalChoice]);
                                } 
                                if (serverMessage.startsWith("TIME")) {
                                    // server side timer
                                    String[] dataArray = serverMessage.split(" ");
                                    int currentTime = Integer.parseInt(dataArray[1]);
                                    timer.setTime(currentTime);
                                    timer.setTimerStatus(Boolean.parseBoolean(dataArray[2].trim()));
                                    
                                    // System.out.println(playerName + "current time" + currentTime);
                                    if(currentTime <= 0 && timer.getTimerStatus()){
                                        
                                        send("TIME_UP");
                                    }
                                }
                                if (serverMessage.startsWith("REVEAL")){
                                    String word = serverMessage.split(" ")[1].trim();
                                    System.out.println(serverMessage);
                                    setCurrentWord(word);


                                }
                                if (serverMessage.startsWith("END_GAME")){
                                    System.out.println("End game");
                                    String score = serverMessage.split(" ")[1].trim();
                                    window.remove(mainWindow);
                                    
                                    window.add(new EndGamePanel(playerName, score));
                                     window.repaint();
                                    window.validate();
                                }
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

    }

    //main window contents
    public class MainWindow extends JPanel {

        private Canvas canvasPane;

        //set up main window contents
        public MainWindow() {
            setLayout(new BorderLayout());

            add(new UpperPane(), BorderLayout.NORTH);

            add((canvasPane = new Canvas()), BorderLayout.CENTER);

            add(new RightPane(), BorderLayout.EAST);

            add(new BrushOptions(canvasPane), BorderLayout.SOUTH);
        }

        protected Canvas getCanvas() {
            return canvasPane;
        }
    }

    //upper portion
    public class UpperPane extends JPanel {

        //contains timer and word to guess
        public UpperPane() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            timer = new Time(60);
            timer.setPreferredSize(new Dimension(55, 55));
            timer.setFont(new Font("San-Serif", Font.PLAIN, 20));

            add(timer, BorderLayout.WEST);

            wordLabel = new JLabel("Welcome!");
            wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
            wordLabel.setFont(new Font("San-Serif", Font.PLAIN, 25));

            add(wordLabel, BorderLayout.CENTER);
        }
    }

    //timer component
    public class Time extends JLabel {
        Timer timer;
        private int time;
        private Boolean isInGame = false;

        public Time(int time) {
            this.time = time;
        }

        public int getTime() {
            return this.time;
        }

        public void setTime(int time) {
            this.time = time;
            this.repaint();
        }

        public void setTimerStatus(Boolean status){

            this.isInGame = status;
        }
        
        public Boolean getTimerStatus(){
            return this.isInGame;
        }

        //render time inside circle
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;

            g2d.setColor(Color.RED);
            g2d.drawOval(0, 0, 50, 50);

            if(!isInGame && this.getTime() <= 3 && this.getTime() > 0){
                setCurrentWord("Game will start in " + this.getTime());
            }
            if(this.getTime() == 3 && !isInGame)
                g2d.setColor(new Color(209, 0, 0));
            else if(this.getTime() == 2 && !isInGame)
                g2d.setColor(new Color(255, 102, 34));
            else if(this.getTime() == 1 && !isInGame)
                g2d.setColor( new Color(51, 221, 0));
            else
                g2d.setColor(Color.BLACK);
            g2d.drawString(Integer.toString(this.getTime()), 15, 32);
        }
    }

    //word-to-guess component
    public class Word extends JLabel {
        private String word;

        public Word(String word) {
            super(word);
            this.word = word;

        }

        public void updateWord(String word) {
            this.word = word;
        }

        public String getTime() {
            return this.word;
        }
    }

    //right portion of UI
    public class RightPane extends JPanel {
        private TextField inputField;
        private JButton enterButton;

        //contains player score and chat room
        public RightPane() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setPreferredSize(new Dimension(200, 250));

            scores = new TextArea("SCORES:", 5, 1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            scores.setEditable(false);
            add(scores);

            chatArea = new TextArea("CHATROOM:", 13, 1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            chatArea.setEditable(false);

            add(chatArea);

            inputField = new TextField();
            inputField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = inputField.getText();

                        if (isAnswer) {
                            System.out.println("You sent answer: " + message);
                            sendAnswer(message);
                        } else if(message.toUpperCase().equals(getCurrentWord())){
                            System.out.println("attempt to say answer " + message);
                        }else {
                            senderThread.sendMessage(message, playerName);
                        }

                        inputField.setText("");
                    }
                }

            });

            add(inputField);

            answerCheckbox = new JCheckBox("Answer?");

            answerCheckbox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    isAnswer = e.getStateChange() == 1 ? true : false;
                }
            });

            add(answerCheckbox);

        }
    }

    //brush options component
    public class BrushOptions extends JPanel {

        //ROYGBV colors and erasers
        public BrushOptions(Canvas canvasPane) {
            JButton blackColor = new JButton(new ColorAction(canvasPane, null, Color.BLACK));
            blackColor.setBackground(Color.BLACK);
            blackColor.setPreferredSize(new Dimension(25, 25));

            JButton redColor = new JButton(new ColorAction(canvasPane, null, new Color(209, 0, 0)));
            redColor.setBackground(new Color(209, 0, 0));
            redColor.setPreferredSize(new Dimension(25, 25));

            JButton orangeColor = new JButton(new ColorAction(canvasPane, null, new Color(255, 102, 34)));
            orangeColor.setBackground(new Color(255, 102, 34));
            orangeColor.setPreferredSize(new Dimension(25, 25));

            JButton yellowColor = new JButton(new ColorAction(canvasPane, null, new Color(255, 218, 33)));
            yellowColor.setBackground(new Color(255, 218, 33));
            yellowColor.setPreferredSize(new Dimension(25, 25));

            JButton greenColor = new JButton(new ColorAction(canvasPane, null, new Color(51, 221, 0)));
            greenColor.setBackground(new Color(51, 221, 0));
            greenColor.setPreferredSize(new Dimension(25, 25));

            JButton blueColor = new JButton(new ColorAction(canvasPane, null, new Color(17, 51, 204)));
            blueColor.setBackground(new Color(17, 51, 204));
            blueColor.setPreferredSize(new Dimension(25, 25));

            JButton violetColor = new JButton(new ColorAction(canvasPane, null, new Color(51, 0, 68)));
            violetColor.setBackground(new Color(51, 0, 68));
            violetColor.setPreferredSize(new Dimension(25, 25));


            JButton eraser = new JButton(new ColorAction(canvasPane, "Eraser", Color.WHITE));

            JButton clear = new JButton(new ClearAction(canvasPane, "Clear"));

            add(blackColor);
            add(redColor);
            add(orangeColor);
            add(yellowColor);
            add(greenColor);
            add(blueColor);
            add(violetColor);
            add(eraser);
            add(clear);
        }

        public class ClearAction extends AbstractAction {
            private Canvas canvasPane;

            private ClearAction(Canvas canvasPane, String name) {
                if (name != null) putValue(NAME, name);
                this.canvasPane = canvasPane;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isDrawer) {
                    canvasPane.clearCanvas();
                    send("CLEAR CANVAS");
                }
            }
        }

        public class ColorAction extends AbstractAction {

            private Canvas canvasPane;
            private Color color;

            private ColorAction(Canvas canvasPane, String name, Color color) {
                if (name != null) putValue(NAME, name);
                this.canvasPane = canvasPane;
                this.color = color;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                canvasPane.setForeground(color);
            }

        }

    }

    //canvas component
    public class Canvas extends JPanel {
        //contains drawing data
        private BufferedImage background;

        public Canvas() {
            this.setBackground(Color.WHITE);
            this.setForeground(Color.BLACK);

            //mouse event listeners
            MouseAdapter handler = new MouseAdapter() {

                //render brush drawings
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isDrawer) {
                        drawDot(e.getPoint());
                        sendPoint(e.getPoint());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDrawer) {
                        drawDot(e.getPoint());
                        sendPoint(e.getPoint());
                    }
                }

            };
            //create action listeners
            addMouseListener(handler);
            addMouseMotionListener(handler);
        }

        protected void sendPoint(Point pt) {
            String color = "";

            if (getForeground().equals(Color.BLACK)) {
                color = "BLACK";
            } else if (getForeground().equals(Color.WHITE)) {
                color = "WHITE";
            } else if (getForeground().equals(new Color(209, 0, 0))) {
                color = "RED";
            } else if (getForeground().equals(new Color(255, 102, 34))) {
                color = "ORANGE";
            } else if (getForeground().equals(new Color(255, 218, 33))) {
                color = "YELLOW";
            } else if (getForeground().equals(new Color(51, 221, 0))) {
                color = "GREEN";
            } else if (getForeground().equals(new Color(17, 51, 204))) {
                color = "BLUE";
            } else if (getForeground().equals(new Color(51, 0, 68))) {
                color = "VIOLET";
            }

            send("PAINT " + color + " " + pt.getX() + " " + pt.getY() + " ");


        }

        protected void drawDot(Point pt) {
            if (background == null) {
                updateBuffer();
            }

            if (background != null) {
                //canvas
                Graphics2D g2d = background.createGraphics();

                //change color of brush
                g2d.setColor(getForeground());

                //change width and height to adjust thickness of brush
                g2d.fillOval(pt.x - 5, pt.y - 5, 10, 10);

                //release system resources of by graphics g2d
                g2d.dispose();
            }

            //update canvas
            repaint();
        }

        protected Color getColorValue(String color) {
            Color colorValue = null;
            if (color.equals("BLACK")) {
                colorValue = Color.BLACK;
            } else if (color.equals("WHITE")) {
                colorValue = Color.WHITE;
            } else if (color.equals("RED")) {
                colorValue = new Color(209, 0, 0);
            } else if (color.equals("ORANGE")) {
                colorValue = new Color(255, 102, 34);
            } else if (color.equals("YELLOW")) {
                colorValue = new Color(255, 218, 33);
            } else if (color.equals("GREEN")) {
                colorValue = new Color(51, 221, 0);
            } else if (color.equals("BLUE")) {
                colorValue = new Color(17, 51, 204);
            } else if (color.equals("VIOLET")) {
                colorValue = new Color(51, 0, 68);
            }


            return colorValue;
        }

        protected void drawDot(Point pt, String color) {
            if (background == null) {
                updateBuffer();
            }

            if (background != null) {
                //canvas
                Graphics2D g2d = background.createGraphics();

                //change color of brush
                g2d.setColor(getColorValue(color));

                //change width and height to adjust thickness of brush
                g2d.fillOval(pt.x - 5, pt.y - 5, 10, 10);

                //release system resources of by graphics g2d
                g2d.dispose();
            }

            //update canvas
            repaint();
        }

        protected void clearCanvas() {
            background = null;
            updateBuffer();
            repaint();


        }

        protected void updateBuffer() {

            if (getWidth() > 0 && getHeight() > 0) {
                BufferedImage newBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

                //render canvas
                Graphics2D g2d = newBuffer.createGraphics();
                //set canvas BG
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                if (background != null) {
                    g2d.drawImage(background, 0, 0, this);
                }
                g2d.dispose();
                background = newBuffer;
            }

        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 200);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            if (background == null) {
                updateBuffer();
            }
            g2d.drawImage(background, 0, 0, this);
            g2d.dispose();
        }
    }

    public class StartInterface extends JFrame{
        private String playerName;
        private TextField nameInput;
        private TextField addressInput;
        private JButton submitButton;
        private JButton howToPlayButton;
        private JLabel notif;
        private Boolean didSubmit = false;
        private JLabel header;

        public StartInterface(String title){
            super(title);

            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setPreferredSize(new Dimension(300, 300));
            
            header = new JLabel("Welcome to ILLUS!");
            header.setHorizontalAlignment(SwingConstants.CENTER);
            header.setFont(new Font("San-Serif", Font.PLAIN, 25));

            howToPlayButton = new JButton(new StartInterface.HowToPlayAction("How To Play"));

            howToPlayButton.setPreferredSize(new Dimension(200, 40));
            addressInput = new TextField();

            nameInput = new TextField();
            nameInput.setPreferredSize(new Dimension(200, 30));
            addressInput = new TextField();
            addressInput.setPreferredSize(new Dimension(200, 30));

            notif = new JLabel("");

            submitButton = new JButton(new StartInterface.SendAction("SUBMIT", nameInput, addressInput, notif));
            submitButton.setPreferredSize(new Dimension(200, 40));
            
            JPanel initContent = new JPanel();
            FlowLayout flow = new FlowLayout(FlowLayout.CENTER);
            flow.setVgap(5);
            initContent.setLayout(flow);
            
            initContent.add(header);
            initContent.add(howToPlayButton);
            initContent.add(new JLabel("Enter name:"));
            initContent.add(nameInput);

            initContent.add(new JLabel("Enter server address:"));
            initContent.add(addressInput);
            initContent.add(submitButton);

            initContent.add(notif);

            this.add(initContent);

            this.pack();
            this.setLocationRelativeTo(null);
            this.setVisible(true);
        }

        public String getPlayerName(){
            return this.playerName;
        }

        public Boolean isSubmit(){
            
            return this.didSubmit;
        }

        public class HowToPlayAction extends AbstractAction{
            private JTextArea howToContent;

            private HowToPlayAction(String name){
                if(name != null) putValue(NAME, name);

                String howTo = "\n\nA. Possible Player Types:\n     1. Player who guesses the word\n     2. Player who draws the image describing the word to be guessed\n\nB. Instructions\n     1. The maximum number of rounds is divisible by the number of players (i.e. rounds = number of players * 3). For each round, assign a player who will draw. The remaining players will try to guess the word being drawn. The first player who is able to guess the word within the shortest amount of time will gain the most points.\n     2. The player who will draw chooses 1 from 3 words. After choosing a word, the player will illustrate the word using the canvas and brush options on the interface.\n     3. The player who will guess will check the 'Answer?' checkbox​​ for the player’s answer to be considered valid.\n\nC. Scoring\n     1. The player who is able to guess the word first will earn a score equivalent to the remaining time in seconds.\n     2. The player who draws will also earn a score based on the number of players who are able to guess the word. The more players who guessed correctly, the higher the additional points (but only of small increments)\n\nD. Endgame\n     1. If the maximum number of levels are completed.\n     2. The scores of each player will also be displayed at the end of the game";

                this.howToContent = new JTextArea(howTo);
                this.howToContent.setEditable(false); 
                this.howToContent.setLineWrap(true);
                this.howToContent.setWrapStyleWord(true);   
            }
            @Override
            public void actionPerformed(ActionEvent e){
                window = new JFrame("How To Play ILLUS");
                window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                window.setPreferredSize(new Dimension(500, 500));
                window.setLayout(new BorderLayout());

                window.add(this.howToContent);
               
                window.pack();
                window.setLocationRelativeTo(null);
                window.setVisible(true);
            }
        }

        public class SendAction extends AbstractAction {
            private TextField nameInput;
            private TextField addressInput;
            private JLabel notif;

            private SendAction(String name, TextField nameInput, TextField addressInput, JLabel notif){
                if(name != null) putValue(NAME, name);
                this.nameInput = nameInput;
                this.addressInput = addressInput;
                this.notif = notif;
            }

            @Override
            public void actionPerformed(ActionEvent e){
                playerName = nameInput.getText();
                serverAddress = addressInput.getText();

                if(playerName.equals("")){
                    JOptionPane.showMessageDialog(null, "Please enter your name", "Error", JOptionPane.ERROR_MESSAGE);
                }else if(serverAddress.equals("")){
                    JOptionPane.showMessageDialog(null, "Please enter a server address", "Error", JOptionPane.ERROR_MESSAGE);
                }else{
                    didSubmit = true;

                    submitButton.setEnabled(false);
                    notif.setText("Waiting for other players...");
                }
                
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = Constants.GAME_PORT;

        new GameClient(port);
    }
}
