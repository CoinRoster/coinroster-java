package com.coinroster;

import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import org.json.JSONObject;

public class MethodCall extends Utils
	{
	protected MethodCall(HttpRequest request, HttpResponse response)
		{
		Connection sql_connection = null;
		
		try {
			String 

			target = request.target_object(),
			method_name = target.substring(0, target.lastIndexOf(".")).trim();

			Session session = new Session(request.cookie("session_token"));
			
			boolean 
			
			session_active = session.active(),
			authorized = false;
			
			if (Server.guest_user_methods.contains(method_name)) authorized = true;
			else if (Server.standard_user_methods.contains(method_name) && session_active) authorized = true;
			else if (Server.admin_user_methods.contains(method_name) && session_active && session.user_level().equals("1")) authorized = true;

			JSONObject output = new JSONObject("{\"status\":\"0\"}");
			
			if (authorized)
				{
				sql_connection = Server.sql_connection();

				MethodInstance method = new MethodInstance();
				
				method.request = request;
				method.response = response;
				method.input = new JSONObject(URLDecoder.decode(request.payload(), "UTF-8"));
				method.output = output;
				method.session = session;
				method.sql_connection = sql_connection;

				Constructor<?> c = Class.forName("com.coinroster.api." + method_name).getConstructor(MethodInstance.class);
				c.newInstance(method);
				}
			else 
				{
				log("!!!!! Unauthorized method call: " + method_name);
				response.send(output);
				}
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
	}