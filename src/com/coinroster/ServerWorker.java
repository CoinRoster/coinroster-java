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

			log("---");

			if (request.status() == 200)
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
					}
				}
			else new CommandHandler(request.first_line(), socket);
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
