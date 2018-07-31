package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BackoutContest;

public class AdminBackoutContest extends Utils {
	
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public AdminBackoutContest(MethodInstance method) throws Exception {
		
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		
		DB db = new DB(sql_connection);

		method : {
			
			int contest_id = input.getInt("contest_id");
//------------------------------------------------------------------------------------
			new BackoutContest(sql_connection, contest_id);
			
			output.put("status", "1");
//------------------------------------------------------------------------------------

		} method.response.send(output);
	}
}
