package com.coinroster;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ServerWorker extends Utils implements Runnable 
	{
	private Socket socket;
	
	public ServerWorker(Socket _socket) 
		{
		socket = _socket;
		}

	public void run() 
		{
		try {
			HttpRequest request = new HttpRequest(socket);

			if (request.status() == 200) // well-formed HTTP request
				{
				switch (request.header("handler"))
					{
					case "api":
						{
						HttpResponse response = new HttpResponse(socket);
						new MethodCall(request, response);
						break;
						}
					case "static":
						{
						OutputStream response = socket.getOutputStream();
						new StaticAsset(request, response);
						break;
						}
					case "ssi":
						{
						OutputStream response = socket.getOutputStream();
						new SSI(request, response);
						break;
						}
					}
				}
			else // not well-formed, open command handler if localhost
				{
				String client_ip = socket.getInetAddress().toString();

				boolean authorized = false;
				
				switch (client_ip)
					{
					case "/0:0:0:0:0:0:0:1":
					case "/127.0.0.1":
					case "/localhost":
						authorized = true;
						break;						
					}
				
				if (authorized) new CommandHandler(request.first_line(), socket);
				else log("Unauthorized connection: " + client_ip);
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		finally
			{
			try {socket.close();} 
			catch (IOException ignore) {}
			}
		}
	}
