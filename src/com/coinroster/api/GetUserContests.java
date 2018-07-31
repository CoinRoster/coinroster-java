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

public class GetUserContests extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetUserContests(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		
		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			PreparedStatement select_user_contest  = sql_connection.prepareStatement("select * from contest where created_by = ? and status = ?");
			select_user_contest.setString(1, session.user_id());
			select_user_contest.setInt(2, 2);

			ResultSet result_set = select_user_contest.executeQuery();
			
			JSONArray user_contests = new JSONArray();
			
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
				rake = result_set.getString(13),
				cost_per_entry = result_set.getString(15);
		
				JSONObject contest = new JSONObject();

				contest.put("contest_id", contest_id);
				contest.put("created", created);
				contest.put("created_by", db.get_username_for_id(created_by));
				contest.put("category", category);
				contest.put("title", title);
				contest.put("description", description);
				contest.put("settlement_type", settlement_type);
				contest.put("option_table", option_table);
				contest.put("rake", rake);
				contest.put("cost_per_entry", cost_per_entry);
				
				user_contests.put(contest);
				}
			
			output.put("pending_contests", user_contests);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}