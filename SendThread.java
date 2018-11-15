import java.io.*;
import java.net.*;
import java.util.*;
import proto.PlayerProtos.Player;
import proto.TcpPacketProtos.TcpPacket;

public class SendThread extends Thread {
	private DataOutputStream out;
	private InputStreamReader in;
	private BufferedReader reader;
	private boolean status;
	private Socket server;
	private ChatClient client;

	public SendThread(Socket server, ChatClient client) {
		this.server = server;
		this.client = client;
		this.status = true;

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

	//if user's message has the prefix /answer, it is considered a user's guess
	boolean checkIfAnswer(String chat){
		boolean isAnswer = chat.startsWith("/answer ") ? true : false;
		return isAnswer;
	}

	public boolean getStatus(){
		return this.status;
	}

	public void run() {
        TcpPacket.ChatPacket chatPacket = null;
		boolean isAnswer;
		try {
			String text;

			do {
				System.out.print("[" + client.getUserName() + "]: ");
				text = reader.readLine();

				// out.writeUTF(text + "\n");
				isAnswer = checkIfAnswer(text);
				if(isAnswer){
					System.out.println(text + " : " + isAnswer);

					/*
						do dictionary.validateAnswer(answer)
					*/
				}else{
					chatPacket = TcpPacket.ChatPacket.newBuilder()
								.setType(TcpPacket.PacketType.CHAT)
								.setMessage(text)
								.setPlayer(client.getPlayer())
								.build();

					out.write(chatPacket.toByteArray());
				}
			} while (!text.equals("exit") && client.isRunning());
			out.write(client.createDisconnectPacket().toByteArray()); //send a disconnect packet to server
			client.stopRunning();
			this.status = false;
			// server.close();
		} catch (IOException e) {
			System.out.println("Error writing to server");
		}
	}
}