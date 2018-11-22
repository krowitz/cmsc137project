
import java.io.*;
import java.util.concurrent.LinkedBlockingQueue;

import proto.TcpPacketProtos.TcpPacket.*;
import proto.PlayerProtos.Player;

public class ChatSendThread extends Thread {
	private DataOutputStream outputStream;
	private boolean status;
	private GameClient client;
	private LinkedBlockingQueue<ChatPacket> messageQueue = new LinkedBlockingQueue<>();

	boolean checkIfAnswer(String chat){
		boolean isAnswer = chat.startsWith("/answer ") ? true : false;
		return isAnswer;
	}

	public void sendMessage(String message, String playerName){
		//filters the message sent
		if(!checkIfAnswer(message)){
			ChatPacket chatPacket = ChatPacket.newBuilder().setType(PacketType.CHAT).setPlayer(Player.newBuilder().setName(playerName).build()).setMessage(message).build();
			messageQueue.add(chatPacket);
		}
	}

	public ChatSendThread(DataOutputStream outputStream, GameClient client) {
            this.outputStream = outputStream;
			this.client = client;
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