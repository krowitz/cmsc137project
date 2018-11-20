import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import proto.PlayerProtos.Player;
import proto.TcpPacketProtos.TcpPacket;

public class ChatClient {
    private String serverName;
	private int port;
	private String userName;
    private Player player;
    private boolean isRunning;

	public ChatClient(String serverName, int port) {
		this.serverName = serverName;
		this.port = port;
        this.isRunning = true;
	}

    void stopRunning(){
        this.isRunning = false;
    }

    boolean isRunning(){
        return this.isRunning;
    }
    void setPlayer(Player player){
        this.player = player;
    }

    Player getPlayer(){
        return this.player;
    }

    Player createPlayer(){
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter username: ");
        String name = sc.nextLine();
        this.setUserName(name);

        System.out.print("Enter user id: ");
        int id = sc.nextInt();
        
        Player player = Player.newBuilder()
                        .setName(name)
                        //.setId(id)
                        .build();

        this.setPlayer(player);

        return player;
    }

	void setUserName(String userName) {
		this.userName = userName;
	}

	String getUserName() {
		return this.userName;
	}


    //instantiate a createLobbyPacket and send it to server
    public void createLobby() throws IOException{
        Socket server = new Socket();
	    server.connect(new InetSocketAddress(serverName, port), 10000);


        OutputStream outToServer = server.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        InputStream inFromServer = server.getInputStream();

        TcpPacket.ConnectPacket connectPacket = createConnectPacket();
        out.write(connectPacket.toByteArray());

//        ReceiveThread receive = new ReceiveThread(server, this);
//        SendThread send = new SendThread(server, this);
//
//        receive.start();
//        send.start();
//
//
//        while(true){
//            if(receive.getStatus() || send.getStatus())
//                break;
//        }
//
//        try{
//            receive.join();
//            send.join();
//        }catch(InterruptedException e){}
//
//        server.close();
    }

    //instantiate a connectPacket and send it to server
    public TcpPacket.ConnectPacket createConnectPacket() throws IOException{
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter lobby id: ");
        String lobbyid = sc.nextLine();

        //build connect packet
        TcpPacket.ConnectPacket connectPacket = TcpPacket.ConnectPacket.newBuilder()
                                                    .setLobbyId(lobbyid)
                                                    .setType(TcpPacket.PacketType.CONNECT)
                                                    .setPlayer(createPlayer())
                                                    .build();

        return connectPacket;
    }

    //instantiate a connectPacket and send it to server
    public TcpPacket.ConnectPacket createConnectPacket(String lobbyid) throws IOException{

        //build connect packet
        TcpPacket.ConnectPacket connectPacket = TcpPacket.ConnectPacket.newBuilder()
                                                    .setLobbyId(lobbyid)
                                                    .setType(TcpPacket.PacketType.CONNECT)
                                                    .setPlayer(createPlayer())
                                                    .build();

        return connectPacket;
    }

    //instantiate a disconnectPacket and send it to server
    public TcpPacket.DisconnectPacket createDisconnectPacket() throws IOException{

        //build connect packet
        TcpPacket.DisconnectPacket disconnectPacket = TcpPacket.DisconnectPacket.newBuilder()
                                                    .setType(TcpPacket.PacketType.DISCONNECT)
                                                    .build();

        return disconnectPacket;
    }

    public void init()  throws IOException{
        Socket server = new Socket();
	    server.connect(new InetSocketAddress(serverName, port), 10000);

        System.out.println("Just connected to " + server.getRemoteSocketAddress());
                             
        TcpPacket.ConnectPacket connectPacket = createConnectPacket();

        /* Send data to the ServerSocket */
        OutputStream outToServer = server.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        out.write(connectPacket.toByteArray());

        System.out.println("Sent connect packet to " + server.getRemoteSocketAddress());
        System.out.println(connectPacket.toByteArray());

//        ReceiveThread receive = new ReceiveThread(server, this);
//        SendThread send = new SendThread(server, this);
//
//        receive.start();
//        send.start();
        
//        while(true){
//            if(receive.getStatus() || send.getStatus())
//                break;
//        }
//
//        try{
//            receive.join();
//            send.join();
//        }catch(InterruptedException e){}
//
//        server.close();

        /* Receive data from the ServerSocket */
        // InputStream inFromServer = server.getInputStream();
        // //DataInputStream in = new DataInputStream(inFromServer);
		
		// BufferedReader br = new BufferedReader(new InputStreamReader(inFromServer, "UTF-8"));

        // try{
        //     while(true){
	  	//         String response = br.readLine();
        // 	    System.out.println(response);
		//     }
        // }catch(Exception e){
        // 	e.printStackTrace();
        // }
       
	    // finally{
        //     //closing the socket of the client
        //     server.close();
	    // }
    }

    public static void main(String[] args) throws Exception{

	    args = new String[2];
	    args[0] = "202.92.144.45";
	    args[1] = "80";
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        Scanner sc = new Scanner(System.in);
        String input;
        boolean valid; 

        do{
             System.out.println("[1] Create a lobby\n[2] Connect to a lobby\n[3] Exit");
            valid = true;
            input = sc.nextLine();
            switch(input){
                case "1":
                    client.createLobby(); //create a new lobby
                    break;
                case "2":
                    client.init(); //connect to a lobby
                    break;
                case "3":
                    break;
                default: 
                    valid = false;
                    break;
            }
        }while(!valid);
        
    }
}
