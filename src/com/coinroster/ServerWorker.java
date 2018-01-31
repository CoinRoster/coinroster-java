package com.coinroster;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;

public class ServerWorker extends Utils implements Callable<Integer> 
	{
	private Socket socket;
	
	public ServerWorker(Socket socket) 
		{
		this.socket = socket;
		}
	
	public Integer call() 
		{
		try {
			HttpRequest request = new HttpRequest(socket);
			
			if (request.status() == 200) // well-formed HTTP request
				{
				String 
				
				http_version = request.version(),
				handler = request.header("handler");

				HttpResponse response = new HttpResponse(socket, http_version);
				
				switch (handler)
					{
					case "api":
						{
						new MethodCall(request, response);
						break;
						}
					case "ssi":
						{
						new SSI(request, response);
						break;
						}
					case "static":
						{
						new StaticAsset(request, response);
						break;
						}
					default: log("Invalid handler: " + handler);
					}
				}
			else // not well-formed, open command handler if localhost
				{
				String client_ip = socket.getInetAddress().toString();

				switch (client_ip)
					{
					case "/0:0:0:0:0:0:0:1":
					case "/127.0.0.1":
					case "/localhost":
						
						new CommandPrompt(request.first_line(), socket);
						break;
						
					default: log("Unauthorized connection: " + client_ip);
					}
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
		
		return 1;
		}
	}
