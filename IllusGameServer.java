/**
 * 1. Create Lobby
 * 2. Wait for players to join
 * 3. Assign player ids upon connection
 * 4. When all players are connected, start chat filter. Chat filter will acquire all broadcast messages from Chat Server and reform it.
 * 5. Choose random [(max players) * 3] words from dictionary and put it into static list.
 * 6. Each player will take turns drawing, according to player_id, in ascending order.
 * 7. On each round, server will send disableDraw commands to non-drawers.
 * 8. Drawer will send a list of points (x,y) to the server. Server will broadcast to players to show drawing.
 * 9. Scoring is as follows: guess within 30 secs, 20 points. Within a minute, 10 points.
 * 10. Drawer is scored by (total points acquired by players / max players) to give incentive to good drawings.
 * 11. If no player guesses correctly, drawer gets -20 penalty.
 */
import proto.TcpPacketProtos.TcpPacket.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import proto.PlayerProtos.Player;

public class IllusGameServer {
    private Socket chatServer;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private volatile int currentId;

    public IllusGameServer (String serverName, int port) throws Exception{
        Socket server = new Socket();
        server.connect(new InetSocketAddress(serverName, port), 10000);

        outputStream = new DataOutputStream(server.getOutputStream());
        inputStream = new DataInputStream(server.getInputStream());
    }

    private String createLobby(int maxPlayers) throws Exception{
        CreateLobbyPacket createLobbyPacket = CreateLobbyPacket.newBuilder().setType(PacketType.CREATE_LOBBY)
                .setMaxPlayers(maxPlayers).build();

        System.out.println("Creating chat lobby.");
        outputStream.write(createLobbyPacket.toByteArray());

        byte[] buffer = new byte[1024];

        inputStream.read(buffer);

        String lobbyId = new String(buffer);
        System.out.println("Server responds: " + new String(buffer));
        System.out.println("Successfully created lobby with ID " + lobbyId);
        return lobbyId;
    }

    private void connectToLobby(String lobbyId) throws Exception{
        Player player = Player.newBuilder().setName("GameServer").setId("0").build();
        currentId = 1;

        System.out.println("Connecting to chat lobby: " + lobbyId);

        ConnectPacket connectPacket = ConnectPacket.newBuilder().setUpdate(ConnectPacket.Update.NEW)
                .setLobbyId(lobbyId).setType(PacketType.CONNECT).setPlayer(player).build();

        outputStream.write(connectPacket.toByteArray());

        byte[] buffer = new byte[1024];

        inputStream.read(buffer);

        System.out.println("Server responds: " + new String (buffer));
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
        int maxPlayers = 3;

        IllusGameServer gameServer = null;
        try {
            gameServer = new IllusGameServer("202.92.144.45", 80);
        }catch (Exception e){
            System.out.println("Game server not successfully initialized. See stacktrace below. ");
            e.printStackTrace();
            System.exit(-1);
        }

        String lobbyId = "";
        try{
            lobbyId = gameServer.createLobby(maxPlayers);
        }catch (Exception e){
            System.out.println("Lobby creation failed. See stacktrace below.");
            e.printStackTrace();
            System.exit(-1);
        }

        try{
            gameServer.connectToLobby(lobbyId);
        }catch (Exception e){
            System.out.println("Failed to connect to lobby. Please see stacktrace below.");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
