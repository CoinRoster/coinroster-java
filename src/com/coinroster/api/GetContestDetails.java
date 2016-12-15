package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetContestDetails extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetContestDetails(MethodInstance method) throws Exception 
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
			
			int contest_id = input.getInt("contest_id");
			
			JSONObject contest = db.select_contest(contest_id);
			
			if (contest != null)
				{
				output.put("category", contest.get("category"));
				output.put("sub_category", contest.get("sub_category"));
				output.put("title", contest.get("title"));
				output.put("description", contest.get("description"));
				output.put("settlement_type", contest.get("settlement_type"));
				output.put("pay_table", contest.get("pay_table"));
				output.put("odds_table", contest.get("odds_table"));
				output.put("salary_cap", contest.get("salary_cap"));
				output.put("cost_per_entry", contest.get("cost_per_entry"));
				output.put("min_users", contest.get("min_users"));
				output.put("max_users", contest.get("max_users"));
				output.put("entries_per_user", contest.get("entries_per_user"));
				output.put("contest_status", contest.get("status"));
				output.put("roster_size", contest.get("roster_size"));
				
				output.put("status", "1");
				}
			else output.put("error", "Invalid contest ID");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}