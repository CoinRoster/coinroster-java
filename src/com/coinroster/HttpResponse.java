package com.coinroster;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.json.JSONObject;

import java.util.Map.Entry;

public class HttpResponse 
	{
	private OutputStream out;
	
	private String version;

	Map<String, String> cookie_map = new TreeMap<String, String>();

	public HttpResponse(Socket socket, String version) 
		{
		try {
			this.version = version;
			this.out = socket.getOutputStream();
			} 
		catch (IOException ignore) {} // broken pipe - nothing to be done
		}

	public void set_cookie(String name, String value)
		{
		cookie_map.put(name, value);
		}

	public void send(JSONObject object)
		{
		send(object.toString());
		}

	public void send(String response_data)
		{
		try {
			send(response_data.length(), "text/html", new ByteArrayInputStream(response_data.getBytes(Utils.ENCODING)));
			}
		catch (IOException ignore) {} // broken pipe - nothing to be done
		}

	public void send(File file, String mime_type) 
		{
		try {
			send(file.length(), mime_type, new FileInputStream(file));
			}
		catch (IOException ignore) {} // broken pipe - nothing to be done
		}
	
	private void send(Object response_length, String mime_type, InputStream stream) throws IOException
		{
		out.write(new String("HTTP/" + version + " 200 OK\r\n").getBytes());
		out.flush();
		
		out.write(new String("Cache-Control: no-cache, max-age=0, must-revalidate, no-store\r\n").getBytes());
		out.flush();
		
//		out.write(new String("Content-Length: " + String.valueOf(response_length) + "\r\n").getBytes());
//		Utils.log("resp le: " + String.valueOf(response_length));
//		Utils.log("response length: " + String.valueOf(response_length).getBytes().length);
//		out.flush();
		
		out.write(new String("Content-Type: " + mime_type + "\r\n").getBytes());
		out.flush();

		write_cookies();
		
		out.write(new String("\r\n").getBytes());
		out.flush();
		
		byte[] buffer = new byte[1024];

		for (int n; (n = stream.read(buffer, 0, buffer.length)) != -1;)
			{
			out.write(buffer, 0, n);
			out.flush();
			}
		
		stream.close();
		out.close();
		}
	
	public void redirect(String target) 
		{
		try {
			out.write(new String("HTTP/" + version + " 302 Found\r\n").getBytes());
			out.flush();
			
			out.write(new String("Location: " + target + "\r\n").getBytes());
			out.flush();

			write_cookies();
			
			out.write(new String("\r\n").getBytes());
			out.flush();
			
			out.close();
			}
		catch (IOException ignore) {} // broken pipe - nothing to be done
		}
	
	private void write_cookies() throws IOException
		{
		Set<Entry<String, String>> cookies = cookie_map.entrySet();
		
		for (Map.Entry<String, String> entry : cookies) 
			{
			out.write(new String("Set-Cookie: " + entry.getKey() + "=" + entry.getValue() + "; Expires=Fri, 31-Dec-9999 23:59:59 GMT; path=/;\r\n").getBytes());
			out.flush();
			}
		}

	}
