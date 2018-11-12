import java.io.*;
import java.net.*;
import java.util.*;
import proto.*;

public class SendThread extends Thread {
	private DataOutputStream out;
	private InputStreamReader in;
	private BufferedReader reader;

	private Socket server;
	private ChatClient client;

	public SendThread(Socket server, ChatClient client) {
		this.server = server;
		this.client = client;

		try {
			OutputStream outToServer = server.getOutputStream();
            out = new DataOutputStream(outToServer);
            
            InputStreamReader in = new InputStreamReader(System.in);
        	reader = new BufferedReader(in);

		} catch (IOException e) {
			System.out.println("Error getting output stream");
			e.printStackTrace();
		}
	}

	public void run() {
        TcpPacketProtos.TcpPacket.ChatPacket chatPacket = null;
		try {
			String text;

			do {
				System.out.print("[" + client.getUserName() + "]: ");
				text = reader.readLine();

				// out.writeUTF(text + "\n");

                chatPacket = TcpPacketProtos.TcpPacket.ChatPacket.newBuilder().setType(TcpPacketProtos.TcpPacket.PacketType.CHAT).setMessage(text).setPlayer(client.getPlayer()).build();

                out.write(chatPacket.toByteArray());

			} while (!text.equals("exit"));

			server.close();
		} catch (IOException e) {
			System.out.println("Error writing to server");
		}
	}
}