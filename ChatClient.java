import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import proto.*;

public class ChatClient {
    private String serverName;
	private int port;
	private String userName;
    private PlayerProtos.Player player;

	public ChatClient(String serverName, int port) {
		this.serverName = serverName;
		this.port = port;
	}

    void setPlayer(PlayerProtos.Player player){
        this.player = player;
    }

    PlayerProtos.Player getPlayer(){
        return this.player;
    }

	void setUserName(String userName) {
		this.userName = userName;
	}

	String getUserName() {
		return this.userName;
	}

    public void init()  throws IOException{
        Socket server = new Socket();
	    server.connect(new InetSocketAddress(serverName, port), 10000);

        System.out.println("Just connected to " + server.getRemoteSocketAddress());

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter username: ");
        String name = sc.nextLine();
        
        this.setUserName(name);

        PlayerProtos.Player player = PlayerProtos.Player.newBuilder().setName(name).setId(1).build();
        this.setPlayer(player);

        //create lobby
        TcpPacketProtos.TcpPacket.ConnectPacket connectPacket = TcpPacketProtos.TcpPacket.ConnectPacket.newBuilder().setLobbyId("CD1L").setType(TcpPacketProtos.TcpPacket.PacketType.CONNECT).setPlayer(player).build();

        /* Send data to the ServerSocket */
        OutputStream outToServer = server.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        out.write(connectPacket.toByteArray());

        System.out.println("Sent connect packet to " + server.getRemoteSocketAddress());
        System.out.println(connectPacket.toByteArray());

        ReceiveThread receive = new ReceiveThread(server, this);
        SendThread send = new SendThread(server, this);

        receive.start();
        send.start();

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
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));

        client.init();
    }
}
