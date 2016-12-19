package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TreeMap;

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
		
			JSONArray entry_report = new JSONArray();
			
			PreparedStatement select_entries = null;
			
			String request_source = input.getString("request_source");
			
			boolean is_admin = session.user_level().equals("1") && request_source.equals("admin_panel");

			if (is_admin) select_entries = sql_connection.prepareStatement("select * from entry order by id desc");
			else 
				{
				select_entries = sql_connection.prepareStatement("select * from entry where user_id = ? order by id desc");
				select_entries.setString(1, session.user_id());
				}
			
			ResultSet result_set = select_entries.executeQuery();
			
			TreeMap<Integer, String> contest_titles = new TreeMap<Integer, String>();

			while (result_set.next())
				{
				int id = result_set.getInt(1);
				int contest_id = result_set.getInt(2);
				String user_id = result_set.getString(3);
				Long created = result_set.getLong(4);
				double amount = result_set.getDouble(5);
				String entry_data = result_set.getString(6);
				
				String contest_title;
				
				if (contest_titles.containsKey(contest_id)) contest_title = contest_titles.get(contest_id);
				else
					{
					contest_title = db.get_contest_title(contest_id);
					contest_titles.put(contest_id, contest_title);
					} 

				JSONObject entry = new JSONObject();
				
				entry.put("id", id);
				entry.put("contest_id", contest_id);
				entry.put("contest_title", contest_title);
				entry.put("user_id", user_id);
				entry.put("created", created);
				entry.put("amount", amount);
				entry.put("entry_data", entry_data);
				
				entry_report.put(entry);
				}
			
			output.put("entry_report", entry_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}