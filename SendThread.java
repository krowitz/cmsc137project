
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

import proto.TcpPacketProtos.TcpPacket.*;
import proto.PlayerProtos.Player;

public class SendThread extends Thread {
	private DataOutputStream outputStream;
	private boolean status;
	private ChatClient client;
	private LinkedBlockingQueue<ChatPacket> messageQueue = new LinkedBlockingQueue<>();

	public void sendMessage(String message, Player player){
		ChatPacket chatPacket = ChatPacket.newBuilder().setPlayer(player).setMessage(message).build();
		messageQueue.add(chatPacket);
	}

	public SendThread(DataOutputStream outputStream) {
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
		try {
			while(client.isRunning()){
				ChatPacket chatPacket = messageQueue.take();
				outputStream.write(chatPacket.toByteArray());
			}
			this.status = false;
		} catch (Exception e) {
			System.out.println("Error writing to server.");
		}
	}
}