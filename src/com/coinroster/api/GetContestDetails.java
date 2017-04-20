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
				String 
				
				contest_type = contest.getString("contest_type"),
				category = contest.getString("category"),
				sub_category = contest.getString("sub_category");;
				
				if (!contest.isNull("progressive"))
					{
					String progressive_code = contest.getString("progressive");
					
					JSONObject progressive = db.select_progressive(progressive_code);
					
					output.put("progressive_code", progressive_code);
					output.put("progressive_payout_info", progressive.getString("payout_info"));
					output.put("progressive_balance", progressive.getDouble("balance"));
					output.put("progressive_paid", contest.getDouble("progressive_paid"));
					}
				else output.put("progressive_code", "");

				output.put("category", category);
				output.put("sub_category", sub_category);
				output.put("category_description", db.get_category_description(category));
				output.put("sub_category_description", db.get_sub_category_description(sub_category));
				output.put("contest_type", contest_type);
				output.put("title", contest.get("title"));
				output.put("description", contest.get("description"));
				output.put("settlement_type", contest.get("settlement_type"));
				output.put("contest_status", contest.get("status"));
				output.put("registration_deadline", contest.get("registration_deadline"));
				output.put("settled", contest.get("settled"));

				double 
				
				total_prize_pool = db.get_contest_prize_pool(contest_id),
				cost_per_entry = contest.getDouble("cost_per_entry");

				int 
				
				current_users = db.get_contest_current_users(contest_id),
				min_users = contest.getInt("min_users");
				
				output.put("total_prize_pool", total_prize_pool);
				output.put("cost_per_entry", cost_per_entry);
				output.put("rake", contest.getDouble("rake"));
				output.put("current_users", current_users);
				output.put("min_users", min_users);
				output.put("max_users", contest.get("max_users"));
				
				if (contest_type.equals("ROSTER"))
					{
					int number_of_entries = (int) divide(total_prize_pool, cost_per_entry, 0);
					double entries_per_user = contest.getDouble("entries_per_user");
					
					if (current_users < min_users)
						{
						JSONArray option_table = new JSONArray(contest.getString("option_table"));
						
						for (int i=0, limit=option_table.length(); i<limit; i++)
							{
							JSONObject player = option_table.getJSONObject(i);
							player.put("count", 0);
							option_table.put(i, player);
							}
						
						output.put("option_table", option_table.toString());
						}
					else output.put("option_table", contest.get("option_table"));
					
					output.put("pay_table", contest.get("pay_table"));
					output.put("salary_cap", contest.get("salary_cap"));
					output.put("roster_size", contest.get("roster_size"));
					output.put("number_of_entries", number_of_entries);
					output.put("option_table", contest.get("option_table"));
					output.put("entries_per_user", entries_per_user);
					output.put("scores_updated", contest.get("scores_updated"));
					
					String score_header = "Raw score";
					if (!contest.isNull("score_header")) score_header = contest.getString("score_header");
					output.put("score_header", score_header);
					
					if (session.active())
						{
						PreparedStatement get_user_amount = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ? and user_id = ?");
						get_user_amount.setInt(1, contest_id);
						get_user_amount.setString(2, session.user_id());
						ResultSet result_set = get_user_amount.executeQuery();

						result_set.next();
						
						double total_entered_amount = result_set.getDouble(1);
						
						int active_user_entries = (int) divide(total_entered_amount, cost_per_entry, 0);
						output.put("active_user_entries", active_user_entries);
						}
					
					}
				else if (contest_type.equals("PARI-MUTUEL"))
					{
					JSONArray 
					
					option_table = new JSONArray(contest.getString("option_table")),
					option_table_with_wager_totals = new JSONArray();
					
					int number_of_options = option_table.length();
					
					double wager_grand_total = 0;
					
					for (int i=0; i<number_of_options; i++)
						{
						JSONObject option_item = option_table.getJSONObject(i);
						int option_id = option_item.getInt("id");
						double wager_total = db.get_option_wager_total(contest_id, option_id);
						option_item.put("wager_total", wager_total);
						wager_grand_total += wager_total;
						option_table_with_wager_totals.put(option_item);
						}

					output.put("entries_per_user", 0); // show "Unlimited"
					output.put("option_table", option_table_with_wager_totals.toString());
					output.put("wager_grand_total", wager_grand_total);
					}
				
				output.put("status", "1");
				}
			else output.put("error", "Invalid contest ID");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}