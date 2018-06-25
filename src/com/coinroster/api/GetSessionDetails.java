package com.coinroster.api;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetSessionDetails extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public GetSessionDetails(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;

		method : {
			
//------------------------------------------------------------------------------------

			if (session.active())
				{
				output.put("is_active", "1");
				output.put("username", session.username());
				output.put("user_level", session.user_level());
				}
			else output.put("is_active", "0");
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}