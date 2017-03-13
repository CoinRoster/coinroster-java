package com.coinroster;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.json.JSONObject;

public class SSI extends Utils
	{
	protected SSI(HttpRequest request, OutputStream response) throws Exception
		{
		String 
		
		response_data = null,
		root = request.header("root"), 				// e.g. /usr/share/nginx/html/coinroster.com
		ssi_directory = root + "/ssi-java/",  		// e.g. /usr/share/nginx/html/coinroster.com/ssi-java/
		target_url = request.target_url(), 			// e.g. /ssi-java/nav
		target_object = request.target_object(); 	// e.g. nav

		String session_token = request.cookie("session_token");
		Session session = new Session(session_token);
		
		boolean session_active = session.active();

		switch (target_object)
			{
			case "session" :
				{
				if (session_active)
					{
					Connection sql_connection = null;

					JSONObject session_properties = new JSONObject();
					
					try {
						sql_connection = Server.sql_connection();
						
						DB db = new DB(sql_connection);
						
						JSONObject user = db.select_user("id", session.user_id());
						
						session_properties.put("username", session.username());
						session_properties.put("btc_balance", user.getDouble("btc_balance"));
						session_properties.put("rc_balance", user.getDouble("rc_balance"));
						session_properties.put("contest_status", user.getInt("contest_status"));
						
						response_data = "<script>window.session = " + session_properties.toString() + ";</script>";
						}
					catch (Exception e)
						{
						Server.exception(e);
						}
					finally
						{
						if (sql_connection != null)
							{
							try {sql_connection.close();} 
							catch (SQLException ignore) {}
							}
						}
					}
				else response_data = " ";
				} break;
			case "nav" :
				{
				if (!session_active) response_data = Utils.read_to_string(ssi_directory + "nav_inactive.html");
				else 
					{
					response_data = Utils.read_to_string(ssi_directory + "nav_active.html");
					response_data = response_data.replace("<!--ssi:username-->", session.username());
					if (session.user_level().equals("1")) response_data = response_data.replaceAll("ssi_header_admin_wrapper", "");
					}
				} break;
			}

		if (response_data != null)
			{
			ByteArrayInputStream stream = new ByteArrayInputStream(response_data.getBytes(Utils.ENCODING));

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
			log("!!!!! Unauthorized SSI request: " + target_url);
			
			response.write(new String(" ").getBytes());
			response.flush();
			}
		}
	}