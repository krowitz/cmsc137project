
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

import proto.TcpPacketProtos.TcpPacket.*;
import proto.PlayerProtos.Player;

public class ChatSendThread extends Thread {
	private DataOutputStream outputStream;
	private boolean status;
	private ChatClient client;
	private LinkedBlockingQueue<ChatPacket> messageQueue = new LinkedBlockingQueue<>();

	public void sendMessage(String message, String playerName){

		ChatPacket chatPacket = ChatPacket.newBuilder().setPlayer(Player.newBuilder().setName(playerName).build()).setMessage(message).build();
		messageQueue.add(chatPacket);
	}

	public ChatSendThread(DataOutputStream outputStream) {
            this.outputStream = outputStream;
	}

	public boolean getStatus(){
		return this.status;
	}

	public void stopSender(){
		this.status = false;
		this.interrupt();
	}
	public void run() {
		while (true) {
			try {
				ChatPacket chatPacket = messageQueue.take();
				outputStream.write(chatPacket.toByteArray());
			} catch (Exception e) {
				System.out.println("Error writing to server.");
			}
		}
	}
}