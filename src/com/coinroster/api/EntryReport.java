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

public class EntryReport extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public EntryReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int contest_id = input.getInt("contest_id");
			
			JSONArray entry_report = new JSONArray();
			
			PreparedStatement select_entries = sql_connection.prepareStatement("select * from entry where user_id = ? and contest_id = ? order by id desc");
			select_entries.setString(1, session.user_id());
			select_entries.setInt(2, contest_id);
			ResultSet result_set = select_entries.executeQuery();
			
			while (result_set.next())
				{
				int id = result_set.getInt(1);
				//int contest_id = result_set.getInt(2);
				String user_id = result_set.getString(3);
				Long created = result_set.getLong(4);
				double amount = result_set.getDouble(5);
				String entry_data = result_set.getString(6);
				double score = result_set.getDouble(7);
				double payout = result_set.getDouble(8);
				
				JSONObject entry = new JSONObject();
				
				entry.put("id", id);
				entry.put("contest_id", contest_id);
				entry.put("user_id", user_id);
				entry.put("created", created);
				entry.put("amount", amount);
				entry.put("entry_data", entry_data);
				entry.put("score", score);
				entry.put("payout", payout);
				
				entry_report.put(entry);
				}
			
			output.put("entry_report", entry_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}