package com.coinroster;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;

import org.json.JSONObject;

public class HttpResponse 
	{
	private Socket socket;
	
	public String new_session_token = null;

	public HttpResponse(Socket _socket) throws IOException 
		{
		socket = _socket;
		}

	public void send(JSONObject output) throws IOException
		{
		String response_text = output.toString();
		
		OutputStream out = socket.getOutputStream();
		
		out.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
		out.flush();
		
		out.write(new String("Date: " + new Date().toString() + "\r\n").getBytes());
		out.flush();
		
		out.write(new String("Accept-Ranges: bytes\r\n").getBytes());
		out.flush();

		/*out.write(new String("Cache-Control: no-cache, max-age=0, must-revalidate, no-store\r\n").getBytes());
		out.flush();*/
		
		out.write(new String("Content-Length: " + String.valueOf(response_text.length()) + "\r\n").getBytes());
		out.flush();
		
		out.write(new String("Content-Type: text/html\r\n").getBytes());
		out.flush();
		
		if (new_session_token != null)
			{
			out.write(new String("Set-Cookie: session_token=" + new_session_token + "; Expires=Fri, 31-Dec-9999 23:59:59 GMT; path=/; secure; httpOnly; \r\n").getBytes());
			out.flush();
			}
		
		out.write(new String("\r\n").getBytes());
		out.flush();
		
		out.write(new String(response_text).getBytes());
		out.flush();
		
		out.close();
		
		socket.close();
		}
	}