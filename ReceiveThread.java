import java.io.*;
import java.net.*;
import proto.*;

public class ReceiveThread extends Thread {
	private BufferedReader reader;
	private Socket server;
	private ChatClient client;
	private DataInputStream in;
    private InputStream inFromServer;

	public ReceiveThread(Socket server, ChatClient client) {
		this.server = server;
		this.client = client;

		try {
			inFromServer = server.getInputStream();

            reader = new BufferedReader(new InputStreamReader(inFromServer));
           	in = new DataInputStream(inFromServer);

		} catch (IOException e) {
			System.out.println("Error getting input stream");
			e.printStackTrace();
		}
	}

	public void run() {
        TcpPacketProtos.TcpPacket.ChatPacket chatPacket = null;
        int nRead;
        byte[] data;
        byte[] temp;
        ByteArrayOutputStream buffer;

		while (true) {
			try {
                buffer = new ByteArrayOutputStream();
                data = new byte[1024];
            
                while ((nRead = inFromServer.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                temp = buffer.toByteArray();

                chatPacket = TcpPacketProtos.TcpPacket.ChatPacket.parseFrom(temp);
                
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
				e.printStackTrace();
				break;
			}
		}
	}
}