
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

				TcpPacket packet = null;
				if(bytes > 0){
					packet = packet.parseFrom(bufferresponse);
					
					if(packet.getType() == TcpPacket.PacketType.CHAT){
						TcpPacket.ChatPacket response = TcpPacket.ChatPacket.parseFrom(bufferresponse);
						String message = new String(response.getPlayer().getName() + ":" + response.getMessage());
						// System.out.println(response.getPlayer().getName() + ":" + response.getMessage());
						this.client.appendMessage(message);
					}
					// System.out.println(packet);
				}

			} catch (Exception e) {
				System.out.println("Error reading from server");
				e.printStackTrace();
			}
		}
	}
}