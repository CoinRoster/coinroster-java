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

public class ContestReport extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public ContestReport(MethodInstance method) throws Exception 
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
			
			PreparedStatement select_contests = null;
			
			if (!category.equals("") && !sub_category.equals(""))
				{
				select_contests = sql_connection.prepareStatement("select * from contest where category = ? and sub_category = ? and (status = 1 or status = 2) order by status asc, id desc");
				select_contests.setString(1, category);
				select_contests.setString(2, sub_category);
				
				output.put("category_description", db.get_category_description(category));
				output.put("sub_category_description", db.get_sub_category_description(sub_category));
				}
			else select_contests = sql_connection.prepareStatement("select * from contest order by status asc, id desc");
			
			JSONArray contest_report = new JSONArray();
			
			ResultSet result_set = select_contests.executeQuery();

			while (result_set.next())
				{
				int id = result_set.getInt(1);
				Long created = result_set.getLong(2);
				String created_by = result_set.getString(3);
				category = result_set.getString(4);
				sub_category = result_set.getString(5);
				String contest_type = result_set.getString(6);
				String title = result_set.getString(7);
				String description = result_set.getString(8);
				String settlement_type = result_set.getString(9);
				String pay_table = result_set.getString(10);
				String option_table = result_set.getString(11);
				double rake = result_set.getDouble(12);
				double salary_cap = result_set.getDouble(13);
				double cost_per_entry = result_set.getDouble(14);
				int min_users = result_set.getInt(15);
				int max_users = result_set.getInt(16);
				int entries_per_user = result_set.getInt(17);
				Long registration_deadline = result_set.getLong(18);
				int status = result_set.getInt(19);
				int roster_size = result_set.getInt(20);
				String odds_source = result_set.getString(21);
				
				created_by = db.get_username_for_id(created_by);
				
				JSONObject contest = new JSONObject();
				
				if (session.active())
					{
					if (session.user_level().equals("1")) // only admins can see the following:
						{
						contest.put("odds_source", odds_source);
						contest.put("created_by", created_by);
						}
					}
			
				double total_prize_pool = db.get_contest_prize_pool(id);
					
				contest.put("id", id);
				contest.put("created", created);
				contest.put("category", category);
				contest.put("sub_category", sub_category);
				contest.put("contest_type", contest_type);
				contest.put("title", title);
				contest.put("description", description);
				contest.put("rake", rake);
				contest.put("settlement_type", settlement_type);
				contest.put("pay_table", pay_table);
				contest.put("option_table", option_table);
				contest.put("salary_cap", salary_cap);
				contest.put("cost_per_entry", cost_per_entry);
				contest.put("min_users", min_users);
				contest.put("max_users", max_users);
				contest.put("entries_per_user", entries_per_user);
				contest.put("registration_deadline", registration_deadline);
				contest.put("status", status);
				contest.put("roster_size", roster_size);
				contest.put("total_prize_pool", total_prize_pool);
				
				contest_report.put(contest);
				}
			
			output.put("contest_report", contest_report);			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}