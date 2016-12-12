package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class CreateEntry extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public CreateEntry(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int pool_id = input.getInt("pool_id");
			JSONArray roster = input.getJSONArray("roster");
			
			log("Pool id: " + pool_id);
			log("Roster: " + roster);
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}