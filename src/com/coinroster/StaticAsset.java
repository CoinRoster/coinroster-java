package com.coinroster;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

public class StaticAsset extends Utils
	{
	protected StaticAsset(HttpRequest request, OutputStream response) throws Exception
		{
		// we handle authentication first - this is primarily to redirect unauthorized
		// admin panel requests to '/' as NGINX would for any other non-existent asset
		
		boolean 
		
		admin_request = request.header("admin").equals("true"),
		authenticated = false;

		Session session = new Session(request.cookie("session_token"));

		if (session.active())
			{
			if (admin_request) // admin header set in NGINX conf
				{
				if (session.user_level().equals("1")) authenticated = true;
				else // non-admin user is trying to access admin - likely a hacking attempt
					{
					log("!!!!! Unauthorized attempt to access admin panel: " + session.username());
					redirect(response, "/");
					return;
					}
				}
			else if (session.user_level().equals("3")) // unverified user - all requests are answered with welcome.html
				{
				redirect(response, "/welcome.html");
				return;
				}
			else authenticated = true; // standard user
			}
		
		// next check to see if the file exists
		// if file does not exist, redirect to index

		String 
		
		subfolder = "",
		target_object = "",
		response_path = "",
		
		root = request.header("root"),
		full_url = request.full_url(),
		target_url = request.target_url();
		target_object = request.target_object();
		subfolder = target_url.split("/")[1];

		if (target_object.equals("")) 
			{
			target_object = subfolder + ".html";
			target_url += target_object;
			}

		response_path = no_whitespace(root + target_url);
		
		File file = new File(response_path);
		
		if (!file.exists()) 
			{
			log("!!!!! Unauthorized file request: " + response_path);
			redirect(response, "/");
			return;
			}

		// if we get here, we serve the file if authenticated ; if not authenticated, serve login.html with redirect
		
		if (authenticated) // user is allowed to request the file in question
			{
			int response_length = (int) file.length();
			
			response.write(new String("HTTP/1.1 200 OK\r\n").getBytes());
			response.flush();
			
			response.write(new String("Cache-Control: no-cache, max-age=0, must-revalidate, no-store\r\n").getBytes());
			response.flush();

			response.write(new String("Content-Length: " + String.valueOf(response_length) + "\r\n").getBytes());
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
		else // user is not authenticated - redirect to login
			{
			String redirect_url = "/login.html#redirect=" + URLEncoder.encode(Server.host + full_url, "UTF-8");
			redirect(response, redirect_url);
			return;
			}
		}
	
	private void redirect(OutputStream response, String redirect_url) throws Exception
		{
		response.write(new String("HTTP/1.1 302 Found\r\n").getBytes());
		response.flush();
		
		response.write(new String("Location: " + redirect_url + "\r\n").getBytes());
		response.flush();
		
		response.write(new String("\r\n").getBytes());
		response.flush();
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