package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class Template extends Utils
	{
	//public static String method_level = guest/standard/admin;
	@SuppressWarnings("unused")
	public Template(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			// method logic goes here
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}