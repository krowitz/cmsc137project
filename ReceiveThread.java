import java.io.*;
import java.net.*;
import proto.PlayerProtos.Player;
import proto.TcpPacketProtos.TcpPacket;

public class ReceiveThread extends Thread {
	private BufferedReader reader;
	private Socket server;
	private ChatClient client;
	private DataInputStream in;
    private InputStream inFromServer;
	private boolean status;

	public ReceiveThread(Socket server, ChatClient client) {
		this.server = server;
		this.client = client;
		this.status = true;

		try {
			inFromServer = server.getInputStream();

            reader = new BufferedReader(new InputStreamReader(inFromServer));
           	in = new DataInputStream(inFromServer);

		} catch (IOException e) {
			System.out.println("Error getting input stream");
			e.printStackTrace();
		}
	}

	public boolean getStatus(){
		return this.status;
	}

	public void run() {
        TcpPacket.ChatPacket chatPacket = null;
        int nRead;
        byte[] data;
        byte[] temp;
        ByteArrayOutputStream buffer;

		while (client.isRunning()) {
			try {
				// TcpPacket reply = TcpPacket.parseFrom(inFromServer);

        		// System.out.println("Server replied with a TCP Packet Type " + reply.getType());
                buffer = new ByteArrayOutputStream();
                data = new byte[1024];
            
                while ((nRead = inFromServer.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                temp = buffer.toByteArray();

                chatPacket = TcpPacket.ChatPacket.parseFrom(temp);
                
				String response = chatPacket.getMessage();
				
				if(response != null){
					System.out.println("\nServer says:");
					System.out.println(response);
    
					// prints the username after displaying the server's message
					if (client.getUserName() != null) {
						System.out.print("[" + client.getUserName() + "]: ");
					}
				}
			} catch (Exception e) {
				// System.out.println("Error reading from server");
				// e.printStackTrace();
				break;
			}
		}
		this.status = false;


	}
}