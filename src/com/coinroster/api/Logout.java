package com.coinroster.api;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class Logout extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public Logout(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;

		method : {
			
//------------------------------------------------------------------------------------
		
			if (session.active()) Server.kill_session(session.token());
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}