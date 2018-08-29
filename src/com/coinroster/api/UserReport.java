package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UserReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UserReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray user_report = new JSONArray();
			
			PreparedStatement select_all_users = sql_connection.prepareStatement("select id from user");
			ResultSet result_set = select_all_users.executeQuery();

			while (result_set.next())
				{
				String user_id = result_set.getString(1);
				
				JSONObject user = db.select_user("id", user_id);
				
				user_report.put(user);
				}
			
			output.put("user_report", user_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}