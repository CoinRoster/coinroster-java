package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetContestDetails extends Utils
	{
	public static String method_level = "guest";
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
				JSONArray entries = db.select_contest_entries(contest_id);
				
				int number_of_entries = entries.length();
				
				String contest_type = contest.getString("contest_type");
				
				output.put("category", db.get_category_description(contest.getString("category")));
				output.put("sub_category", db.get_sub_category_description(contest.getString("sub_category")));
				output.put("contest_type", contest_type);
				output.put("title", contest.get("title"));
				output.put("description", contest.get("description"));
				output.put("settlement_type", contest.get("settlement_type"));
				output.put("option_table", contest.get("option_table"));
				output.put("contest_status", contest.get("status"));
				output.put("registration_deadline", contest.get("registration_deadline"));

				double 
				
				total_prize_pool = db.get_contest_prize_pool(contest_id),
				cost_per_entry = contest.getDouble("cost_per_entry");
				
				output.put("total_prize_pool", total_prize_pool);
				output.put("cost_per_entry", cost_per_entry);
				output.put("rake", contest.getDouble("rake"));
				
				if (contest_type.equals("ROSTER"))
					{
					output.put("pay_table", contest.get("pay_table"));
					output.put("salary_cap", contest.get("salary_cap"));
					output.put("min_users", contest.get("min_users"));
					output.put("max_users", contest.get("max_users"));
					output.put("entries_per_user", contest.get("entries_per_user"));
					output.put("roster_size", contest.get("roster_size"));
					output.put("number_of_entries", total_prize_pool / cost_per_entry);
					}
				
				output.put("status", "1");
				}
			else output.put("error", "Invalid contest ID");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}