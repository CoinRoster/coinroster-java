package com.coinroster;

import java.io.File;
import java.net.URLEncoder;

/**
 * Handles access to static assets (img etc.)
 *
 */
public class StaticAsset extends Utils
	{
	/**
	 * Serves static assets after performing access control.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	protected StaticAsset(HttpRequest request, HttpResponse response) throws Exception
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
					response.redirect("/");
					return;
					}
				}
			else if (session.user_level().equals("3")) // unverified user - all requests are answered with welcome.html
				{
				response.redirect("/welcome.html");
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
			response.redirect("/");
			return;
			}

		// if we get here, we serve the file if authenticated ; if not authenticated, serve login.html with redirect
		
		if (authenticated) response.send(file, get_mime_type(target_object));
		else // user is not authenticated - redirect to login
			{
			String redirect_url = "/login.html#redirect=" + URLEncoder.encode(Server.host + full_url, "UTF-8");
			response.redirect(redirect_url);
			return;
			}
		}

	/**
	 * Returns filetype of asset.
	 * 
	 * @param target_object
	 * @return filetype of asset
	 */
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