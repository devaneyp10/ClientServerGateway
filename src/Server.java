import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import tcdIO.Terminal;

public class Server extends Node 
{
	static final int GATEWAY_PORT = 8;
	static final int SERVER_PORT = 9;
	static final String DEFAULT_DST_NODE = "localhost";
	public int expectedSeqNumber=0;
	public int ackCount = 1;
	public int errorCount = 1;
	InetSocketAddress gatewayAddress;
	Terminal terminal;
	
	
	Server(Terminal terminal, int port) 
	{
		try 
		{
			gatewayAddress = new InetSocketAddress(DEFAULT_DST_NODE, GATEWAY_PORT);
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/*
	 * This method receives packets from the gateway. If the sequence
	 * number is the same as the expected sequence number it will print 
	 * the packet and send an acknowledgement back to the gateway. Otherwise
	 * it will send an error message back to the gateway.
	 */
	public void onReceipt(DatagramPacket packet) 
	{
		try 
		{
			
			StringContent content= new StringContent(packet);
			byte[] buffer = null;
			byte[] payload= null;
			byte[] header= null;
			
			int seqNumber = getSeqNumber(packet);
			if (seqNumber==expectedSeqNumber)
			{
				terminal.println("Received from: Client\n");
				terminal.println(content.toString());
				terminal.println("\nAcknowledgement being sent back to gateway.");
				terminal.println("----------------------------------------------------------------------------------");

				
			    String ackString = "ACK"+ackCount++;
				payload = ackString.getBytes();
	            header= new byte[PacketContent.HEADERLENGTH];
				buffer= new byte[header.length + payload.length];
				System.arraycopy(header, 0, buffer, 0, header.length);
				System.arraycopy(payload, 0, buffer, header.length, payload.length);
				
				DatagramPacket response;
				response= new DatagramPacket(buffer, buffer.length, gatewayAddress);
				setServerFlag(response);
				response.setSocketAddress(gatewayAddress);
				socket.send(response);
				expectedSeqNumber++;
			}
			else
			{
				terminal.println("Error: Sequence numbers do not match up.\nWarning being sent to gateway so that \n"
						+ "the packet can be resent.");
				terminal.println("----------------------------------------------------------------------------------");

				String nackString = "ERR"+errorCount++;
				payload = nackString.getBytes();
	            header= new byte[PacketContent.HEADERLENGTH];
				
				buffer= new byte[header.length + payload.length];
				System.arraycopy(header, 0, buffer, 0, header.length);
				System.arraycopy(payload, 0, buffer, header.length, payload.length);
				
				DatagramPacket response;
				response= new DatagramPacket(buffer, buffer.length, gatewayAddress);
				setServerFlag(response);
				setErrorFlag(packet);
				response.setSocketAddress(gatewayAddress);
				socket.send(response);
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}


	/*
	 * This method sets the first byte in the buffer to a zero to notify the 
	 * gateway that the packet is coming from the server.
	 */
	public void setServerFlag(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		data[0]=(byte)0;
		packet.setData(data);
	}
	
	/*
	 * This method returns the sequence number of a packet.
	 */
	public int getSeqNumber(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		return (int)data[1];
	}
	
	/*
	 * This method sets the 3rd byte in the buffer of a packet being sent
	 *  back to the gateway to a 1 if an error has occured
	 * in the sequence numbers of incoming packets. The packet is then 
	 * interpreted as an error message by the gateway.
	 */
	public void setErrorFlag(DatagramPacket packet)
	{
		byte[] data = packet.getData();
		data[2]=(byte)1;
		packet.setData(data);
	}

	public synchronized void start() throws Exception 
	{
		terminal.println("Waiting to reveive packet 1...\n");
		this.wait();
	}
	
	public static void main(String[] args) 
	{
		try 
		{					
			Terminal terminal= new Terminal("Server");
			(new Server(terminal, SERVER_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}