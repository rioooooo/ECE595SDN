import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


public class UDPClient_Ver2 {
	static int client_id;
	static String hostname,clienthostname;
	static int port,clientport;

	public static Timer timer;
	public static Timer timer2;

	static int length_parts;
	static int[] neighbor_ID = {0};
	static String[] neighborID_active = {""};
	static String [] neighbor_port = {""};
	static String[] neighbor_address = {""};

	static ConcurrentHashMap <Integer, Integer> neighbor_sendalivecheck = new ConcurrentHashMap<Integer, Integer>();
	static ConcurrentHashMap <Integer, String> neighbor_activetable = new ConcurrentHashMap<Integer, String>();
	
	
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
			
			
			//System.out.println("client id = " + client_info.client_id);
			//System.out.println("host name = " + client_info.hostname);
			//System.out.println("port num = " + client_info.port);

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


		//Hashtable <String,Integer> neighbor_IDs = new Hashtable <String,Integer>();
		//int hashidx = 1;
		//for(hashidx=1;hashidx<=100;hashidx++) neighbor_IDs.put(String.valueOf(hashidx),hashidx);

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
			//System.out.println("sentence = " + sentence);
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
				//System.out.println("number of neighbors: " + (length_parts-1));

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

					//update the concurrent table of activity
					neighbor_activetable.put(i, neighborID_active[i]);

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

		timer = new Timer("Timer");
			//timer.schedule(new MyTimerTask(),1000); 
			//ClientHandler subthread = new ClientHandler(timer, length_parts, neighborID_active, neighbor_port, neighbor_address, client_id, IPAddress, neighbor_ID, clientport);
			//subthread.run();
		timer.schedule(new ClientHandler(timer,length_parts, neighborID_active, neighbor_port, neighbor_address, client_id, IPAddress, neighbor_ID, clientport, clientSocket, neighbor_sendalivecheck, neighbor_activetable),5000); 
		
		timer2 = new Timer("Timer2");
		timer2.schedule(new ClientHandler2(neighbor_sendalivecheck, length_parts, neighbor_ID, timer2, clientSocket, client_id, IPAddress, clientport, neighbor_activetable ), 10000);
		
		
		String sentence;

		while(true){

			System.out.println("Starting Thread: ###" + Thread.currentThread().getName() + "###");
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

						//System.out.println(neighbor_port[i]);
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.valueOf(neighbor_port[i].trim()));

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
				

				if (parts[0].trim().equals("KEEP_ALIVE_CHECK"))
				{
					String incoming_ID = parts[1];
					int incoming_IDint = Integer.parseInt(incoming_ID.trim());
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
					//check if the message is from any switch that sent KEEP_ALIVE to
					try{
						int alivemsgflag = 0;
						alivemsgflag = neighbor_sendalivecheck.get(neighbor_ID[y]);
					
						if(alivemsgflag == 1)
						{
							//System.out.println("putting key" + neighbor_ID[y] + "value 2");
							neighbor_sendalivecheck.put(neighbor_ID[y],2);
						}
					}
					catch(Exception e){}
					
				}

				if (parts[0].trim().equals("KEEP_ALIVE"))
				{

					String incoming_ID = parts[1];
					int incoming_IDint = Integer.parseInt(incoming_ID.trim());
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

					sentence = "KEEP_ALIVE_CHECK " + String.valueOf(client_id) + " ";
					sendData = sentence.getBytes();



					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, incoming_addr, incoming_port);
					clientSocket.send(sendPacket);
					System.out.println("Sending out KEEP_ALIVE_CHECK ");
					
				}

				if (parts[0].trim().equals("ROUTE_UPDATE"))
				{

					System.out.println("Receiving ROUTE_UPDATE from Controller: ");
					System.out.println(parts[1]);
				}


			}
			catch(Exception e){
				System.err.println("what the fuck");

			}
			//reply keepalive

			//update neighbor table , inac ->ac


			//subthread
			//send topology update to controller
			//recv table from controller, print it out
			//System.out.println("xxxxxxxxx");
			

			//timer.schedule(new MyTimerTask(), 10000); 
			
		}   //while loop

}
}


class ClientHandler2 extends TimerTask{
	ConcurrentHashMap <Integer, Integer> neighbor_sendalivecheck;
	int length_parts;
	int[] neighbor_ID;
	Timer timer2;
	DatagramSocket clientSocket;
	int client_id;
	InetAddress IPAddress;
	int clientport;
	ConcurrentHashMap<Integer, String> neighbor_activetable;
	
	ClientHandler2(ConcurrentHashMap<Integer, Integer> neighbor_sendalivecheck, int length_parts, int[] neighbor_ID, Timer timer2,DatagramSocket clientSocket, int client_id, InetAddress IPAddress, int clientport, ConcurrentHashMap<Integer, String> neighbor_activetable){
		this.neighbor_sendalivecheck = neighbor_sendalivecheck;
		this.length_parts = length_parts;
		this.neighbor_ID = neighbor_ID;
		this.timer2 = timer2;
		this.clientSocket = clientSocket;
		this.client_id = client_id;
		this.IPAddress = IPAddress;
		this.clientport = clientport;
		this.neighbor_activetable = neighbor_activetable;
		
	}
	
	public void run(){
		//String [] neighborID_active = {""};
		System.out.println("Starting Thread: ###" + Thread.currentThread().getName() + "###");
		//reset timer
		
		System.out.println("every 10 secs, check if the KEEPALIVE_CHECK has been received from those alive neighbors");
		int i ;
		for (i = 0; i < length_parts - 1; i++){
			//check if the send alive check has been received

			//System.out.println("i = "+i + "length_parts = " + length_parts);
			//System.out.println("neighbor_ID[" + i + "] = " + neighbor_ID[i] );
			if(neighbor_sendalivecheck.get(neighbor_ID[i]) == 2)
			{
				System.out.println("Received keep alive check from Switch " + neighbor_ID[i]);
				//neighborID_active[i] = "active";
				neighbor_activetable.put(i, "active");

			}
			else  //if(neighbor_sendalivecheck.get(neighbor_ID[i]) == 1)
			{
				System.out.println("Did not receive alive check from Switch " + neighbor_ID[i]);
				System.out.println("Marking Switch " + neighbor_ID[i] + "as inactive");
				neighbor_activetable.put(i, "inactive");
				
			}
			// else
			// {
			// 	//System.out.println("Myvalue corresponding to the key is " +neighbor_sendalivecheck.get(neighbor_ID[i]));
			// 	System.out.println("Something Wrong with the concurrent table" + " i =   " + i);
			// 	neighborID_active[i] = "inactive";
			// }
		}

		//send topology update to controller
		String sentence = "";
		sentence = "TOPOLOGY_UPDATE " + client_id + " ";
		for (i = 0; i < length_parts-1 ; i ++)
		{
			//construct the topology update message
			if(neighbor_activetable.get(i).equals("active"))
			{
				sentence = sentence + neighbor_ID[i] + "_active" + " "; 
			}
			else if (neighbor_activetable.get(i).equals("inactive"))
			{
				sentence = sentence + neighbor_ID[i] + "_inactive" + " ";
			}

		}
		
		try
		{
			//send out the topo update to controller
			System.out.println("Sending out topology update to controller in TIMER2");
			System.out.println(sentence);
			
			byte[] sendData = new byte[1024];  
			sendData = sentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientport);
			clientSocket.send(sendPacket);
			 	
			//print out info for debugging
			System.out.println("Sending out TOPO_UPDATE to Controller");
		}
		catch(Exception e)
		{
			System.out.println("Error in sending out TOPO_UPDATE to Controller");
		}



		timer2.cancel();
		timer2 = new Timer("timer2");
		timer2.schedule(new ClientHandler2(neighbor_sendalivecheck, length_parts, neighbor_ID, timer2, clientSocket, client_id, IPAddress, clientport, neighbor_activetable), 10000);
	}
	
}




class ClientHandler extends TimerTask {
	Timer timer;
	int length_parts;
	String[] neighborID_active;
	String[] neighbor_port;
	String[] neighbor_address;
	int client_id;
	InetAddress IPAddress;
	int[] neighbor_ID;
	int clientport;
	DatagramSocket clientSocket;
	ConcurrentHashMap<Integer, Integer> neighbor_sendalivecheck;
	ConcurrentHashMap<Integer, String> neighbor_activetable;
	
	ClientHandler(Timer timer, int length_parts, String[] neighborID_active, String[] neighbor_port, String[] neighbor_address, int client_id, InetAddress IPAddress, int[] neighbor_ID, int clientport,DatagramSocket clientSocket, ConcurrentHashMap<Integer, Integer> neighbor_sendalivecheck, ConcurrentHashMap<Integer, String> neighbor_activetable)
	{
		this.timer = timer;
		this.length_parts = length_parts;
		this.neighborID_active = neighborID_active;
		this.neighbor_port = neighbor_port;
		this.neighbor_address = neighbor_address;
		this.client_id = client_id;
		this.IPAddress = IPAddress;
		this.neighbor_ID = neighbor_ID;
		this.clientport = clientport;
		this.clientSocket = clientSocket;
		this.neighbor_sendalivecheck = neighbor_sendalivecheck;
		this.neighbor_activetable = neighbor_activetable;
		
	}


	
	public void run(){
		System.out.println("Starting Thread: ###" + Thread.currentThread().getName() + "###");
		//reset timer
		
		System.out.println("every 5 secs, send TOPO_UPDATE to contorller and send keep alive to neighbors");
		int i = 0;
		String sentence = "";
		byte[] sendData = new byte[1024];       
		//byte[] receiveData = new byte[1024];  
		//String[] parts = {""};
	
		//DatagramSocket clientSocket;
		
		//int active_client_counter = 0;

			try{
				//System.out.println("Entering thread run()"+ "---" + Thread.currentThread().getName());
				for (i = 0; i < length_parts-1 ; i ++)
				{
					if(neighborID_active[i].equals("active"))
					{
						//send out KEEP_ALIVE to all active neighbors
						sentence = "KEEP_ALIVE " + String.valueOf(client_id) + " ";
						//System.out.println(sentence);
						sendData = sentence.getBytes();

						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Integer.valueOf(neighbor_port[i].trim()));
  
						clientSocket.send(sendPacket);

					 	//call timer
						System.out.println("Sending out KEEP_ALIVE to SWITCH " + neighbor_ID[i] + "---" + Thread.currentThread().getName().toString());
						//System.out.println("Sending to : Swtich with port" +  Integer.valueOf(neighbor_port[i].trim()) );
						//active_client_counter ++;
						
						//update the concurrent table
						
						//System.out.println("putting key" + neighbor_ID[i] + "value 1");
						neighbor_sendalivecheck.put(neighbor_ID[i],1);
							
					}
					//update the concurrent table if the neighbor is not active
					else{
						neighbor_sendalivecheck.put(neighbor_ID[i], 0);
						//System.out.println("putting key" + neighbor_ID[i] + "value 0");
					}
				}

				//System.out.println("Getting out for "+ "---" + Thread.currentThread().getName());
				/*
				for(int j = 0; j < active_client_counter ; j ++){
					try{
						receiveData = new byte[1024]; 
						DatagramPacket receivePacket2 = new DatagramPacket(receiveData, receiveData.length);       
						
						DatagramSocket clientSocket = new DatagramSocket();  
						clientSocket.receive(receivePacket2);       //receive the packet from server

						
						
						String receivedsentence = new String(receivePacket2.getData());

						System.out.println("FROM SOMEONE:" + receivedsentence + "---" + Thread.currentThread().getName());

						int incoming_port = 0;
						incoming_port = receivePacket2.getPort();
						InetAddress incoming_addr = receivePacket2.getAddress();

						parts = receivedsentence.split(" ");  //split the message
						System.out.println("FROM SOMEONE: PARTS[0]" + parts[0]);  

						if (parts[0].equals("KEEP_ALIVE_CHECK"))
						{
							String incoming_ID = parts[1];
							
							//int incoming_IDint = Integer.parseInt(incoming_ID);
							int incoming_IDint = Integer.parseInt(incoming_ID.trim());
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
				*/


				sentence = "TOPOLOGY_UPDATE " + client_id + " ";
				for (i = 0; i < length_parts-1 ; i ++)
				{
					//construct the topology update message
					if(neighbor_activetable.get(i).equals("active"))
					{
						sentence = sentence + neighbor_ID[i] + "_active" + " "; 
					}
					else if (neighbor_activetable.get(i).equals("inactive"))
					{
						sentence = sentence + neighbor_ID[i] + "_inactive" + " ";
					}
					else
					{
						sentence = sentence + neighbor_ID[i] + "_unknown" + " ";
					}

				}
				
				try
				{
					//send out the topo update to controller
					
					//System.out.println(sentence);

					sendData = sentence.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, clientport);
					clientSocket.send(sendPacket);
					 	
					//print out info for debugging
					System.out.println("Sending out TOPO_UPDATE to Controller");
				}
				catch(Exception e)
				{
					System.out.println("Error in sending out TOPO_UPDATE to Controller");
				}

			

			}
			catch(Exception e)
			{
				System.err.println(e);
				System.out.println("what the fuck ???!!!");
				System.out.println("Exiting thread");
				
			}
			//update the timer
			
			timer.cancel();
			timer = new Timer("Timer");
			//timer.schedule(new MyTimerTask(),1000); 
			//ClientHandler subthread = new ClientHandler(timer, length_parts, neighborID_active, neighbor_port, neighbor_address, client_id, IPAddress, neighbor_ID, clientport);
			//subthread.run();
			timer.schedule(new ClientHandler(timer,length_parts, neighborID_active, neighbor_port, neighbor_address, client_id, IPAddress, neighbor_ID, clientport, clientSocket, neighbor_sendalivecheck, neighbor_activetable),5000); 
		

	}
}



	





	