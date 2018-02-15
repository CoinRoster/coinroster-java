package com.coinroster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.Socket;
import java.net.URLDecoder;

import java.util.Hashtable;

public class HttpRequest 
	{
	private BufferedReader reader;
	
	private String 
	
	first_line = "",
	full_url = "",
	method = "", 
	url = "", 
	payload = "";
	
	private Hashtable<String, String>
	
	headers = new Hashtable<String, String>(), 
	cookies = new Hashtable<String, String>(), 
	query_fields = new Hashtable<String, String>();
	
	private int[] version = new int[2];

	protected HttpRequest(Socket socket) throws IOException 
		{
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}

	protected int status() throws IOException 
		{
		// parse first line of request:
			
		first_line = reader.readLine();

		if (first_line == null || first_line.length() == 0 || Character.isWhitespace(first_line.charAt(0))) return 400;

		String request[] = first_line.split("\\s");
			
		// check for standard format: GET /index.html HTTP/1.0
			
		if (request.length != 3) return 400;

		if (request[2].indexOf("HTTP/") == 0 && request[2].indexOf('.') > 5) 
			{
			String temp[] = request[2].substring(5).split("\\.");
			try {
				version[0] = Integer.parseInt(temp[0]);
				version[1] = Integer.parseInt(temp[1]);
				} 
			catch (NumberFormatException e) 
				{
				return 400;
				}
			} 
		else return 400;

		method = request[0];
		full_url = request[1];

		int query_index = full_url.indexOf('?');
		
		// no query string:

		if (query_index < 0) url = full_url;
		
		// yes query string:
		
		else 
			{
			url = URLDecoder.decode(full_url.substring(0, query_index), "ISO-8859-1");
			
			// parse query string:
			
			String[] query_items = full_url.substring(query_index + 1).split("&");

			for (int i=0; i<query_items.length; i++) 
				{
				String[] query_item = query_items[i].split("=");
				
				if (query_item.length == 2) 
					{
					query_fields.put(URLDecoder.decode(query_item[0], "ISO-8859-1"), URLDecoder.decode(query_item[1], "ISO-8859-1"));
					} 
				else if (query_item.length == 1 && query_items[i].indexOf('=') == query_items[i].length() - 1) 
					{
					query_fields.put(URLDecoder.decode(query_item[0], "ISO-8859-1"), "");
					}
				}
			}
			
		// parse headers:
	
		String line = reader.readLine();
		
		while (!line.equals("")) 
			{
			int index = line.indexOf(':');
			if (index == -1) 
				{
				headers = null;
				break;
				} 
			else headers.put(line.substring(0, index).toLowerCase(), line.substring(index + 1).trim());
			
			// read line and go to next
			
			line = reader.readLine();
			}
		
		if (headers == null) return 400;
			
		// parse cookies:
			
		String cookie = header("Cookie");
		
		if (cookie != null) 
			{
			cookie = cookie.trim().replaceAll(" +", "");
			String[] cookie_array = cookie.split(";");
			for (int i=0, limit=cookie_array.length; i<limit; i++)
				{
				String[] this_cookie = cookie_array[i].split("=");
				if (this_cookie.length == 2)
					{
					cookies.put(this_cookie[0], this_cookie[1]);
					}
				}
			}
			
		// parse payload:

		String content_length = header("Content-Length");
		
		if (content_length != null) 
			{
			StringBuilder data = new StringBuilder();
			
			int max_length = Integer.parseInt(content_length);

			while (data.length() < max_length) 
				{
				int value = reader.read();
				data.append((char)value);
				
				}

			payload = data.toString();
			}

		// return

		return 200;
		}

	public String first_line() 
		{
		return first_line;
		}

	public String method() 
		{
		return method;
		}

	public String full_url() 
		{
		return full_url;
		}
	
	public String target_url() 
		{
		return url;
		}

	public String target_object() 
		{
		return url.substring(url.lastIndexOf("/") + 1, url.length());
		}

	public String version() 
		{
		return version[0] + "." + version[1];
		}
	
	public String query_field(String name) 
		{
		if (query_fields != null) return query_fields.get(name);
		else return null;
		}

	public String header(String name) 
		{
		if (headers != null) return headers.get(name.toLowerCase());
		else return null;
		}

	public String cookie(String name) 
		{
		if (cookies != null) return cookies.get(name.toLowerCase());
		else return null;
		}

	public String payload() 
		{
		return payload;
		}
	}