package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;

public class TriggerBuildLobby extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public TriggerBuildLobby(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			new BuildLobby(sql_connection);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}