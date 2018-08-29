package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;

public class GetLandingString extends Utils
	{
	public static String method_level = "guest";
	public GetLandingString(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		output = method.output;
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
			
			try{
				String html = db.get_landing_string();
				output.put("html", html);
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.getMessage());
				output.put("status", "0");
				break method;
			}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}