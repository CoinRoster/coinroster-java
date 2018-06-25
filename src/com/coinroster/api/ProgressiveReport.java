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

public class ProgressiveReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public ProgressiveReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String
			
			category = input.getString("category"),
			sub_category = input.getString("sub_category");
			
			JSONArray progressive_report = new JSONArray();

			PreparedStatement select_progressives = null;
			
			if (!category.equals("") && !category.equals(""))
				{
				select_progressives = sql_connection.prepareStatement("select id from progressive where category = ? and sub_category = ?");
				select_progressives.setString(1, category);
				select_progressives.setString(2, sub_category);
				}
			else select_progressives = sql_connection.prepareStatement("select id from progressive");
			
			ResultSet result_set = select_progressives.executeQuery();
			
			while (result_set.next())
				{
				int id = result_set.getInt(1);
				
				JSONObject progressive = db.select_progressive(id);
				
				String created_by = db.get_username_for_id(progressive.getString("created_by"));
				progressive.put("created_by", created_by);
				
				progressive_report.put(progressive);
				}
			
			output.put("progressive_report", progressive_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}