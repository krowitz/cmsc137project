
import java.io.*;
import java.util.Arrays;
import proto.TcpPacketProtos.TcpPacket;
import proto.PlayerProtos.Player;
public class ChatReceiveThread extends Thread {
	private GameClient client;
	private DataInputStream inputStream;
	private boolean status;
	private String currentWord;

	public ChatReceiveThread(DataInputStream inputStream, GameClient client) {
		this.status = true;
		this.inputStream = inputStream;
		this.client = client;
	}

	public boolean getStatus(){
		return this.status;
	}

	public void stopReceiving(){
		this.status = false;
		this.interrupt();
	}


	public void run() {
		while (true) {
			try {

				byte[] data = new byte[1024];
				// inputStream.read(data);
				
				// System.out.println("Acquired from server: " + new String(data));

				int bytes = -1;

				bytes = inputStream.read(data); //number of bytes received from server
				byte[] bufferresponse = Arrays.copyOfRange(data, 0, bytes); //adjust byte array length depending on the number of bytes received
				String message;
				TcpPacket packet = null;
				if(bytes > 0){
					packet = packet.parseFrom(bufferresponse);
					
					switch(packet.getType()){
						case CHAT:
							TcpPacket.ChatPacket chatresponse = TcpPacket.ChatPacket.parseFrom(bufferresponse);
							message = new String(chatresponse.getPlayer().getName() + ":" + chatresponse.getMessage());
							// System.out.println(response.getPlayer().getName() + ":" + response.getMessage());
							this.client.appendMessage(message);
							break;
						case DISCONNECT:
							TcpPacket.DisconnectPacket disconnectresponse = TcpPacket.DisconnectPacket.parseFrom(bufferresponse);
							if(disconnectresponse.getUpdate() == TcpPacket.DisconnectPacket.Update.NORMAL){
								message = new String(disconnectresponse.getPlayer().getName() + " has disconnected.");
								this.client.appendMessage(message);
							}else if(disconnectresponse.getUpdate() == TcpPacket.DisconnectPacket.Update.LOST){
								message = new String(disconnectresponse.getPlayer().getName() + " has lost connection.");
								this.client.appendMessage(message);
							}
							break;
						case CONNECT:
							TcpPacket.ConnectPacket connectresponse = TcpPacket.ConnectPacket.parseFrom(bufferresponse);
							// if(connectresponse.getUpdate() == TcpPacket.ConnectPacket.Update.NEW){
							// 	message = new String(connectresponse.getPlayer().getName() + " has entered the lobby.");
							// 	this.client.appendMessage(message);
							// }
							break;

					}
				}

			} catch (Exception e) {
				System.out.println("Error reading from server");
				e.printStackTrace();
			}
		}
	}
}