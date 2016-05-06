import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Hashtable;

public class UDPClient_Ver2 {
	static int client_id;
	static String hostname,clienthostname;
	static int port,clientport;

	public static Timer timer;


	static int length_parts;
	static int[] neighbor_ID = {0};
	static String[] neighborID_active = {""};
	static String [] neighbor_port = {""};
	static String[] neighbor_address = {""};

	//declare the struct of client input
	UDPClient_Ver2(int Client_ID,String Hostname,int Port)
	{
	    client_id = Client_ID;
	    hostname = Hostname;
	    port = Port;
	}


	public static void main(String args[]) throws Exception    
	{	       
		//declare the variable
		//UDPClient client_info = NULL;
		int clientid = 0;
		

		//try to get the client info from command line
		try{
			UDPClient_Ver2 client_info = new UDPClient_Ver2(Integer.parseInt(args[0]),args[1],Integer.parseInt(args[2]));
			
			
			System.out.println("client id = " + client_info.client_id);
			System.out.println("host name = " + client_info.hostname);
			System.out.println("port num = " + client_info.port);

			clientid = client_info.client_id;
			clienthostname = client_info.hostname;
			clientport = client_info.port;


		}		
		catch (Exception e)
		{
			System.out.println("Please input the client info as the following example: ");
			System.out.println("./UDPClient [Client_ID] [hostname] [port number]");
		}

		//BufferedReader inFromUser =          
		//		new BufferedReader(new InputStreamReader(System.in));       
		DatagramSocket clientSocket = new DatagramSocket();        //declare the client socket
		InetAddress IPAddress = InetAddress.getByName(clienthostname);    //get the ip address by name

		byte[] sendData = new byte[1024];       
		byte[] receiveData = new byte[1024];  

		int finishflag = 0;
		
		//declare all info of neighbor


		// int[] neighbor_ID = {0};
		// String[] neighborID_active = {""};
		// String [] neighbor_port = {""};
		// String[] neighbor_address = {""};


		Hashtable <String,Integer> neighbor_IDs = new Hashtable <String,Integer>();
		int hashidx = 1;
		for(hashidx=1;hashidx<=100;hashidx++) neighbor_IDs.put(String.valueOf(hashidx),hashidx);

		int i;
		String[] parts;
		String modifiedSentence = "";
		//DatagramPacket receivePacket;
		//DatagramPacket sendPacket;
		length_parts = 0;

		try{
			
			//String sentence = inFromUser.readLine();       
			//sendData = sentence.getBytes();       
			String sentence = "";
			String idnum = String.valueOf(clientid);
			sentence = idnum + " " + "REGISTER_REQUEST";
			System.out.println("sentence = " + sentence);
			sendData = sentence.getBytes();  //construct the sentence

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientport);       //compose the packet
			clientSocket.send(sendPacket);        //send the packet
			
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);       
			clientSocket.receive(receivePacket);       //receive the packet from server
			
			modifiedSentence = new String(receivePacket.getData());       //get the data from packet
			System.out.println("FROM SERVER:" + modifiedSentence);  

			parts = modifiedSentence.split(" ");  //split the message from server
			System.out.println(parts[0]);

			length_parts = parts.length;


			if (parts[0].equals("REGISTER_RESPONSE"))
			{
				//int size = Integer.parseInt(parts[1]);   //get the number of neighbors
				System.out.println("number of neighbors: " + (length_parts-1));

				neighbor_ID = new int[length_parts-1];
				neighborID_active = new String[length_parts-1];
				neighbor_port = new String [length_parts-1];
				neighbor_address = new String[length_parts-1];

				i = 0;
				while( i < length_parts-1 )
				{	
					String[] pofparts = parts[i + 1].split("_");  //split the 
					neighbor_ID[i] = Integer.parseInt(pofparts[0]); 
					neighborID_active[i] = pofparts[1];
					neighbor_port[i] = pofparts[3];
					neighbor_address[i] = pofparts[2];
					
					i = i + 1; //update the counter	

				}

				
				for (i = 0; i < length_parts-1; i++)
				{
					System.out.println("neighbor " + neighbor_ID[i] + neighborID_active[i] + neighbor_port[i] + neighbor_address[i]);
				}
			}

			finishflag = 1;	
		
			//clientSocket.close(); 
		}
		catch (Exception e)
		{
			System.out.println("Server is not responding. Please try again.");
			System.out.println("Exception caught:" + e);
		   
		} 


		String sentence;

		while(true){

			
			if (finishflag == 1)
			{
				//send info to active neighbors
				for (i = 0; i < length_parts-1 ; i ++)
				{
					if(neighborID_active[i].equals("active"))
					{
						sentence = "KEEP_ALIVE " + String.valueOf(client_id) + " ";
						System.out.println(sentence);
						sendData = sentence.getBytes();

						System.out.println(neighbor_port[i]);
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.valueOf(neighbor_port[i].trim()));

						System.out.println(neighbor_port[i]);
						clientSocket.send(sendPacket);
					 	//call timer
						System.out.println("Sending out KEEP_ALIVE to SWITCH " + neighbor_ID[i]);
					}
				}
				
				
			}
			finishflag = 0;
			
			//try accept data from neighbors
			try{
				receiveData = new byte[1024]; 
				DatagramPacket receivePacket2 = new DatagramPacket(receiveData, receiveData.length);       
				
				clientSocket.receive(receivePacket2);       //receive the packet from server

				
				
				String receivedsentence = new String(receivePacket2.getData());

				System.out.println("FROM SOMEONE:" + receivedsentence);

				int incoming_port = 0;
				incoming_port = receivePacket2.getPort();
				InetAddress incoming_addr = receivePacket2.getAddress();

				parts = receivedsentence.split(" ");  //split the message
				System.out.println("FROM SOMEONE: PARTS[0]" + parts[0]);  

				if (parts[0].equals("KEEP_ALIVE_CHECK"))
				{
					String incoming_ID = parts[1];
					
					//int incoming_IDint = Integer.parseInt(incoming_ID);
					int incoming_IDint = (neighbor_IDs.get(incoming_ID));
					int y ;
					for (y = 0; y < length_parts-1 ;y++)
					{
						if(neighbor_ID[y] == incoming_IDint)
						{
							break;
						}
					}
					
					neighborID_active[y] = "active";
					neighbor_port[y] = String.valueOf(incoming_port);
					neighbor_address[y] = incoming_addr.toString();
				
					//reply with KEEP ALIVE
				}

				if (parts[0].equals("KEEP_ALIVE"))
				{

					String incoming_ID = parts[1];
					int incoming_IDint = (neighbor_IDs.get(incoming_ID));
					int y ;
					for (y = 0; y < length_parts-1 ;y++)
					{
						if(neighbor_ID[y] == incoming_IDint)
						{
							break;
						}
					}
					
					neighborID_active[y] = "active";
					//System.out.println("xxxxxxxxxxx2");
					neighbor_port[y] = String.valueOf(incoming_port);
					//System.out.println("xxxxxxxxxxx3");
					neighbor_address[y] = incoming_addr.toString();
					//System.out.println("xxxxxxxxxxx4");

					sentence = "KEEP_ALIVE_CHECK " + String.valueOf(client_id) + " ";
					sendData = sentence.getBytes();



					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, incoming_addr, incoming_port);
					clientSocket.send(sendPacket);
					
					
				}

				if (parts[0].equals("ROUTE_UPDATE"))
				{

					System.out.println(parts[1]);
				}
			}
			catch(Exception e){
				System.err.println(e);

			}
			//reply keepalive

			//update neighbor table , inac ->ac


			//subthread
			//send topology update to controller
			//recv table from controller, print it out
			ClientHandler subthread = new ClientHandler(timer);
			subthread.run();
		}   //while loop

}
}










class ClientHandler implements Runnable {
	Timer timer;
	ClientHandler(){
		this.timer = timer;
		System.out.println("Starting Thread: ###" + Thread.currentThread().getName() + "###");
	}

	int i;
	public void run(){
		while(true){
			try{
				for (i = 0; i < length_parts-1 ; i ++)
				{
					if(neighborID_active[i].equals("active"))
					{
						sentence = "KEEP_ALIVE " + String.valueOf(client_id) + " ";
						System.out.println(sentence);
						sendData = sentence.getBytes();

						System.out.println(neighbor_port[i]);
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.valueOf(neighbor_port[i].trim()));

						System.out.println(neighbor_port[i]);
						clientSocket.send(sendPacket);
					 	//call timer
						System.out.println("Sending out KEEP_ALIVE to SWITCH " + neighbor_ID[i]);

						active_client_counter ++;
					}
				}


				for(int j = 0; j < active_client_counter ; j ++){
					try{
						receiveData = new byte[1024]; 
						DatagramPacket receivePacket2 = new DatagramPacket(receiveData, receiveData.length);       
						
						clientSocket.receive(receivePacket2);       //receive the packet from server

						
						
						String receivedsentence = new String(receivePacket2.getData());

						System.out.println("FROM SOMEONE:" + receivedsentence);

						int incoming_port = 0;
						incoming_port = receivePacket2.getPort();
						InetAddress incoming_addr = receivePacket2.getAddress();

						parts = receivedsentence.split(" ");  //split the message
						System.out.println("FROM SOMEONE: PARTS[0]" + parts[0]);  

						if (parts[0].equals("KEEP_ALIVE_CHECK"))
						{
							String incoming_ID = parts[1];
							
							//int incoming_IDint = Integer.parseInt(incoming_ID);
							int incoming_IDint = (neighbor_IDs.get(incoming_ID));
							int y ;
							for (y = 0; y < length_parts-1 ;y++)
							{
								if(neighbor_ID[y] == incoming_IDint)
								{
									break;
								}
							}
							
							neighborID_active[y] = "active";
							neighbor_port[y] = String.valueOf(incoming_port);
							neighbor_address[y] = incoming_addr.toString();
						
							//reply with KEEP ALIVE
						}

					}
					catch(Exception e)
					{
						System.out.println("Exception in receiving other swtich's replys");
					}
				}


				sentence = "TOPO_UPDATE ";
				for (i = 0; i < length_parts-1 ; i ++)
				{
					
					if(neighborID_active[i].equals("active"))
					{
						sentence = sentence + neighbor_ID + "_active"; 
					}
					else if (neighborID_active[i].equals("inactive"))
					{
						sentence = sentence + neighbor_ID + "_inactive";
					}
					else
					{
						sentence = sentence + neighbor_ID + "_unknown";
					}

				}
				
				try
				{
					System.out.println(sentence);
				//send out the topo update to controller
					sendData = sentence.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clienthostname, clientport);
					clientSocket.send(sendPacket);
					 	//call timer
					System.out.println("Sending out TOPO_UPDATE to Controller");
				}
				catch(Exception e)
				{
					System.out.println("Error in sending out TOPO_UPDATE to Controller");
				}

				//try to receive updated routemap from controller and print it out
				try{
					receiveData = new byte[1024]; 
					DatagramPacket receivePacket2 = new DatagramPacket(receiveData, receiveData.length);       					
					clientSocket.receive(receivePacket2);       //receive the packet from server
					String receivedsentence = new String(receivePacket2.getData());
					System.out.println("FROM SOMEONE:" + receivedsentence);

				}
				catch(Exception e)
				{
					System.out.println("Error in receiving the route map from Controller");
				}

				//update timer
				timer.cancel();
				timer = new Timer("Timer");
				timer.schedule(new MyTimerTask(),20000);



			}
			catch(Exception e)
			{
				System.out.println(e);
			}



		}  //while true 

	}
}

class MyTimerTask extends TimerTask
{
	MyTimerTask(){
		System.out.println("Starting TimerTask");
	}

	public void run(){
		System.out.println("Client inactive for 20 seconds... Updating topology map and sending it back to Controller");

	}
}














