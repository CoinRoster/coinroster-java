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

public class GetUnapprovedContests extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetUnapprovedContests(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		
		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			PreparedStatement select_transaction  = sql_connection.prepareStatement("select * from contest where status = 5");

			ResultSet result_set = select_transaction.executeQuery();
			
			JSONArray pending_contests = new JSONArray();
			
			while (result_set.next())
				{
				String 
		
				contest_id = result_set.getString(1),
				created = result_set.getString(2),
				created_by = result_set.getString(3),
				category = result_set.getString(4),
				title = result_set.getString(7),
				description = result_set.getString(8),
				settlement_type = result_set.getString(9),
				option_table = result_set.getString(11),
				rake = result_set.getString(12),
				cost_per_entry = result_set.getString(14);
		
				JSONObject transaction = new JSONObject();

				transaction.put("contest_id", contest_id);
				transaction.put("created", created);
				transaction.put("created_by", db.get_username_for_id(created_by));
				transaction.put("category", category);
				transaction.put("title", title);
				transaction.put("description", description);
				transaction.put("settlement_type", settlement_type);
				transaction.put("option_table", option_table);
				transaction.put("rake", rake);
				transaction.put("cost_per_entry", cost_per_entry);
				
				pending_contests.put(transaction);
				}
			
			output.put("pending_contests", pending_contests);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}