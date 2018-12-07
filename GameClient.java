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
import java.util.TimerTask;
import java.util.Scanner;
import java.util.Collections;

/**
 * 1. Connect to lobby
 * 2. Run chat sender / server update listener threads.
 * 3. Parse messages from server.
 * 4. Run instance of Main UI.
 *
 */

public class GameClient {

    private Runnable gameServerListener;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private static String playerName;
    private DatagramSocket gameServerSocket = new DatagramSocket();
    private static ChatSendThread senderThread;
    private static ChatReceiveThread receiveThread;
    private Time timer;
    private static TextArea chatArea;
    private static JLabel wordLabel;
    private static JCheckBox answerCheckbox;    
    private String chatLobbyId;
    private MainWindow mainWindow;
    private Boolean isDrawer = true;
    private Boolean isAnswer = false;

    private static Boolean initUI = false;

    protected void setCurrentWord(String word){
        wordLabel.setText(word.toUpperCase());
    }

    public GameClient(String playerName, String serverAddress, int port) throws Exception{

        try{
            connectToGame(playerName);
        }catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        Socket server = new Socket();
        server.connect(new InetSocketAddress("202.92.144.45", 80), 10000);

        outputStream = new DataOutputStream(server.getOutputStream());
        inputStream = new DataInputStream(server.getInputStream());

        try{
            this.playerName = playerName;
            connectToLobby(chatLobbyId);
            
            
        }catch (Exception e){
            System.out.println("Connecting to lobby failed. See stacktrace below.");
            e.printStackTrace();
            System.exit(-1);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.submit(gameServerListener);

        System.out.println("Waiting for other players");
        
        while(true){
            System.out.print("");
            if(getInitUI()){
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

        JFrame window = new JFrame("Illus (Player: "+this.playerName+")");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setPreferredSize(new Dimension(500,500));
        window.setLayout(new BorderLayout());
        window.add(mainWindow = new MainWindow());
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void send(String msg){
        try{
            byte[] buf = msg.getBytes();
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Constants.GAME_PORT);
            gameServerSocket.send(packet);
        }catch(Exception e){}
    }

    private void connectToLobby(String lobbyId) throws Exception{
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
        if(bytes > 0){
            packet = packet.parseFrom(bufferResponse);
            // System.out.println(packet);
            if(packet.getType() == TcpPacketProtos.TcpPacket.PacketType.CONNECT){
                TcpPacketProtos.TcpPacket.ConnectPacket response = TcpPacketProtos.TcpPacket.ConnectPacket.parseFrom(bufferResponse);
                // System.out.println(response);
                System.out.println("Successfully connected to lobby with ID " + lobbyId);
            }
            
        }

       
    }
    public String getPlayerName(){
        return this.playerName;
    }
    protected void sendAnswer(String answer){
        if(!isDrawer)
            send("ANSWER " + this.playerName + " " + answer);
    }
    public static void appendMessage(String message){
        chatArea.append("\n" + message);
    }

    public static void setInitUI(Boolean value){
        GameClient.initUI = value;
        
    }

    public Boolean getInitUI(){

        return GameClient.initUI;
    }

    private void connectToGame(String playerName) throws Exception{
        Boolean connected = false;

        int count = 0;
        // System.out.println(connected + " " + count);
        while(!connected){
            send("CONNECT " + playerName);
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try{
                gameServerSocket.receive(packet);
            }catch(Exception ioe){
                ioe.printStackTrace();
            }
            String connectionStatus = new String(packet.getData());

            if(connectionStatus.startsWith("SUCCESS")){
                connected = true;
            }
            System.out.println("Connection Status");
            System.out.println(connectionStatus);
            this.chatLobbyId = connectionStatus.split("[^a-zA-Z0-9']+")[1];

            // count++;
            // if(count == 3) throw new Exception("Failed to connect to game server.");
        }
        
        gameServerListener = new Runnable() {

            @Override
            public void run() {
                while(true){

                    try {
                        byte[] buf = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        gameServerSocket.receive(packet);
                        if(packet != null){
                            String serverMessage = new String(packet.getData());
                            if(serverMessage.startsWith("PLAYER_COUNT_MET")){
                                setInitUI(true);
                                
                            }else{
                                String data = serverMessage.split(" ")[1].trim();
                                
                                if(serverMessage.startsWith("MESSAGE")){
                                    appendMessage(data);
                                }

                                if(serverMessage.startsWith("WORD")){
                                    String word = data.trim();
                                    if(isDrawer){
                                        setCurrentWord(word);
                                        answerCheckbox.setVisible(false);
                                    }else{
                                        String changed = String.join("", Collections.nCopies(word.length(), "\u25A1"));
                                        setCurrentWord(changed);
                                    }
                                }
                                if(serverMessage.startsWith("START")){
                                    answerCheckbox.setVisible(true);
                                }
                                if(serverMessage.startsWith("CORRECT_ANSWER")){
                                    appendMessage("PLAYER " + data + " got it CORRECT!");
                                    
                                    if(data.equals(playerName)){
                                        System.out.println("[!] Answer DISABLED");
                                        answerCheckbox.setVisible(false);
                                        isAnswer = false;
                                    }
                                }
                                if(serverMessage.startsWith("SCORES")){
                                    //update scores
                                }
                                if(serverMessage.startsWith("DRAWER")){
                                    if(!data.equals(playerName)){
                                        //disable pen
                                        isDrawer = false;
                                    }else
                                        isDrawer = true;
                                }
                                if(serverMessage.startsWith("POINTS")){
                                    //draw line given points
                                }
                                if(serverMessage.startsWith("PAINT")){
        
                                    String[] dataArray = serverMessage.split(" ");
                                    int x = (int)Double.parseDouble(dataArray[2]);
                                    int y = (int)Double.parseDouble(dataArray[3]);

                                    Point point = new Point(x,y);

                                    String color = dataArray[1];
                                    mainWindow.getCanvas().drawDot(point, color);
                                }
                                if(serverMessage.startsWith("CLEAR")){
                                    //clear
                                    mainWindow.getCanvas().clearCanvas();
                                }
                                if(serverMessage.startsWith("CHOOSE")){
                                    String[] dataArray = serverMessage.split(" ");
                                    String[] options = {dataArray[1], dataArray[2], dataArray[3]};

                                    int finalChoice = JOptionPane.showOptionDialog(null, "Choose a word", "Drawer Choice", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

                                    send("CHOICE " + options[finalChoice]);
                                }
                                if(serverMessage.startsWith("TIME")){
                                    String[] dataArray = serverMessage.split(" ");
                                    int currentTime = Integer.parseInt(dataArray[1]);
                                    timer.setTime(currentTime);

                                }
                            }

                        }
                    }catch (Exception e){
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

        protected Canvas getCanvas(){
            return canvasPane;
        }
    }

    //upper portion
    public class UpperPane extends JPanel{

        //contains timer and word to guess
        public UpperPane() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            timer = new Time(60);
            timer.setPreferredSize(new Dimension(55,55));
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

        public Time(int time) {
            this.time = time;
        }

        public int getTime(){
            return this.time;
        }

        public void setTime(int time){
            this.time = time;
            this.repaint();
        }

        //render time inside circle
        public void paintComponent( Graphics g ) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D)g;

            g2d.setColor(Color.RED);
            g2d.drawOval(0,0, 50, 50);

            g2d.setColor(Color.BLACK);
            g2d.drawString(Integer.toString(this.getTime()), 15, 32);
        }
    }

    //word-to-guess component
    public class Word extends JLabel{
        private String word;

        public Word(String word) {
            super(word);
            this.word = word;

        }

        public void updateWord(String word){
            this.word = word;
        }

        public String getTime(){
            return this.word;
        }
    }

    //right portion of UI
    public class RightPane extends JPanel {
        private TextArea scores;
        private TextField inputField;
        private JButton enterButton;

        //contains player score and chat room

        public RightPane(){
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            setPreferredSize(new Dimension(200,250));

            scores = new TextArea("SCORES:",5,1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            scores.setEditable(false);
            add(scores);

            chatArea = new TextArea("CHATROOM:",13,1, TextArea.SCROLLBARS_VERTICAL_ONLY);
            chatArea.setEditable(false);

            add(chatArea);

            inputField = new TextField();
            inputField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER){
                        String message = inputField.getText();

                        if(isAnswer){
                            System.out.println("You sent answer: " + message);
                            sendAnswer(message);
                        }else{
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
                    isAnswer = e.getStateChange() == 1 ? true:false;  
                }    
            });

            add(answerCheckbox);

            // enterButton = new JButton(new RightPane.SendAction("Enter", inputField));
            // enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            // add(enterButton);

        }

        // public class SendAction extends AbstractAction{
        //     private TextField inputField;

        //     private SendAction(String name, TextField inputField){
        //         if(name != null) putValue(NAME, name);
        //         this.inputField = inputField;
        //     }

        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         String message = inputField.getText();

        //         if(isAnswer){
        //             sendAnswer(message);
        //         }else{
        //             senderThread.sendMessage(message, playerName);
        //         }

        //         inputField.setText("");
        //     }
        // }
    }

    //brush options component
    public class BrushOptions extends JPanel {

        //ROYGBV colors and erasers
        public BrushOptions(Canvas canvasPane) {
            JButton blackColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, Color.BLACK));
            blackColor.setBackground(Color.BLACK);
            blackColor.setPreferredSize(new Dimension(25,25));

            JButton redColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(209, 0, 0)));
            redColor.setBackground(new Color(209, 0, 0));
            redColor.setPreferredSize(new Dimension(25,25));

            JButton orangeColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(255, 102, 34)));
            orangeColor.setBackground(new Color(255, 102, 34));
            orangeColor.setPreferredSize(new Dimension(25,25));

            JButton yellowColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(255, 218, 33)));
            yellowColor.setBackground(new Color(255, 218, 33));
            yellowColor.setPreferredSize(new Dimension(25,25));

            JButton greenColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(51, 221, 0)));
            greenColor.setBackground(new Color(51, 221, 0));
            greenColor.setPreferredSize(new Dimension(25,25));

            JButton blueColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(17, 51, 204)));
            blueColor.setBackground(new Color(17, 51, 204));
            blueColor.setPreferredSize(new Dimension(25,25));

            JButton violetColor = new JButton(new BrushOptions.ColorAction(canvasPane, null, new Color(51, 0, 68)));
            violetColor.setBackground(new Color(51, 0, 68));
            violetColor.setPreferredSize(new Dimension(25,25));


            JButton eraser = new JButton(new BrushOptions.ColorAction(canvasPane, "Eraser", Color.WHITE));

            JButton clear = new JButton(new BrushOptions.ClearAction(canvasPane, "Clear"));

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

        public class ClearAction extends AbstractAction{
            private Canvas canvasPane;

            private ClearAction(Canvas canvasPane, String name){
                if(name != null) putValue(NAME, name);
                this.canvasPane = canvasPane;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if(isDrawer){
                    canvasPane.clearCanvas();
                    send("CLEAR CANVAS");
                }
            }
        }

        public class ColorAction extends AbstractAction {

            private Canvas canvasPane;
            private Color color;

            private ColorAction(Canvas canvasPane, String name, Color color) {
                if(name != null) putValue(NAME, name);
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
                    if(isDrawer){
                        drawDot(e.getPoint());
                        sendPoint(e.getPoint());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if(isDrawer){
                        drawDot(e.getPoint());
                        sendPoint(e.getPoint());
                    }
                }

            };
            //create action listeners
            addMouseListener(handler);
            addMouseMotionListener(handler);
        }
        protected void sendPoint(Point pt){
            String color = "";
            
            if(getForeground().equals(Color.BLACK)){
                color = "BLACK";
            }else if(getForeground().equals(Color.WHITE)){
                color = "WHITE";
            }else if(getForeground().equals(new Color(209, 0, 0))){
                color = "RED";
            }else if(getForeground().equals(new Color(255, 102, 34))){
                color = "ORANGE";
            }else if(getForeground().equals(new Color(255, 218, 33))){
                color = "YELLOW";
            }else if(getForeground().equals(new Color(51, 221, 0))){
                color = "GREEN";
            }else if(getForeground().equals(new Color(17, 51, 204))){
                color = "BLUE";
            }else if(getForeground().equals(new Color(51, 0, 68))){
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

        protected Color getColorValue(String color){
            Color colorValue = null;
            if(color.equals("BLACK")){
                 colorValue = Color.BLACK;
            }else if(color.equals("WHITE")){
                 colorValue = Color.WHITE;
            }else if(color.equals("RED")){
                 colorValue  = new Color(209, 0, 0);
            }else if(color.equals("ORANGE")){
                 colorValue  = new Color(255, 102, 34);
            }else if(color.equals("YELLOW")){
                 colorValue  = new Color(255, 218, 33);
            }else if(color.equals("GREEN")){
                 colorValue  = new Color(51, 221, 0);
            }else if(color.equals("BLUE")){
                 colorValue  = new Color(17, 51, 204);
            }else if(color.equals("VIOLET")){
                 colorValue  = new Color(51, 0, 68);
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
        protected void clearCanvas(){
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

    public static void main(String[] args) throws Exception{
        String playerName;
        String serverAddress = "127.0.0.1";
        int port = Constants.GAME_PORT;

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter player name: ");
        playerName = sc.nextLine();

        new GameClient(playerName,serverAddress,port);
    }
}
