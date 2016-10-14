package com.coinroster;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Date;

public class StaticAsset extends Utils
	{
	protected StaticAsset(HttpRequest request, OutputStream response) throws Exception
		{
		String 
		
		subfolder = "",
		target_object = "",
		response_path = "";

		int response_length = 0;
		
		boolean 
		
		authenticated = false,
		file_exists = false;

		String 
		
		root = request.header("root"),
		target_url = request.target_url();
		target_object = request.target_object();
		subfolder = target_url.split("/")[1];

		if (target_object.equals("")) 
			{
			target_object = subfolder + ".html";
			target_url += target_object;
			}

		String session_token = request.cookie("session_token");
		Session session = new Session(session_token);

		if (session.active())
			{
			if (subfolder.equals("admin"))
				{
				if (session.user_level().equals("1")) authenticated = true;
				}
			else authenticated = true;
			}

		if (authenticated) 
			{
			response_path = no_whitespace(root + target_url);
			
			log("File: " + response_path);

			File file = new File(response_path);

			if (file.exists()) 
				{
				response_length = (int) file.length();
				file_exists = true;
				}
			}

		if (!authenticated) 
			{
			response.write(new String("HTTP/1.1 302 Found\r\n").getBytes());
			response.flush();
			
			response.write(new String("Location: /\r\n").getBytes());
			response.flush();
			
			response.write(new String("\r\n").getBytes());
			response.flush();
			}
		else if (file_exists)
			{
			response.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
			response.flush();
			
			response.write(new String("Date: "+new Date().toString()+"\r\n").getBytes());
			response.flush();
			
			response.write(new String("Accept-Ranges: bytes\r\n").getBytes());
			response.flush();

			if (session.user_level().equals("1"))
				{
				response.write(new String("Cache-Control: no-cache, max-age=0, must-revalidate, no-store\r\n").getBytes());
				response.flush();
				}
			
			response.write(new String("Content-Length: "+String.valueOf(response_length)+"\r\n").getBytes());
			response.flush();
			
			response.write(new String("Content-Type: " + get_mime_type(target_object) + "\r\n").getBytes());
			response.flush();

			response.write(new String("\r\n").getBytes());
			response.flush();

			FileInputStream stream = new FileInputStream(response_path);
			
			byte[] buffer = new byte[1024];

			for (int n; (n = stream.read(buffer, 0, buffer.length)) != -1;)
				{
				response.write(buffer, 0, n);
				response.flush();
				}
			
			stream.close();
			}
		else
			{
			response.write(new String("HTTP/1.1 404 Not Found\r\n").getBytes());
			response.flush();

			response.write(new String("\r\n").getBytes());
			response.flush();
			}
		}

	private String get_mime_type(String target_object)
		{
		String file_ext = target_object.substring(target_object.lastIndexOf(".") + 1, target_object.length());
		
		switch (file_ext)
			{
			case "htm":
			case "html":
				return "text/html";
			case "js":
				return "application/x-javascript";
			case "css":
				return "text/css";
			case "jpeg":
			case "jpg":
			case "jpe":
				return "image/jpeg";
			case "png":
				return "image/png";
			case "tiff":
			case "tif":
				return "image/tiff";
			case "gif":
				return "image/gif";
			case "bmp":
				return "image/bmp";
			case "ief":
				return "image/ief";
			case "xml":
				return "text/xml";
			case "pdf":
				return "application/pdf";
			case "doc":
				return "application/msword";
			case "xls":
				return "application/vnd.ms-excel";
			case "ppt":
				return "application/vnd.ms-powerpoint";
			case "swf":
				return "application/x-shockwave-flash";
			case "zip":
				return "application/zip";
			case "rtx":
				return "text/richtext";
			case "rtf":
				return "text/rtf";
			case "wav":
				return "audio/x-wav";
			case "mpga":
			case "mp2":
			case "mp3":
			case "mp4":
				return "audio/mpeg";
			case "aif":
			case "aiff":
			case "aifc":
				return "audio/x-aiff";
			case "mpeg":
			case "mpg":
			case "mpe":
				return "video/mpeg";
			case "qt":
			case "mov":
				return "video/quicktime";
			case "avi":
				return "video/x-msvideo";
			case "movie":
				return "video/x-sgi-movie";
			default:
				return "text/html";
			}
		}
	}