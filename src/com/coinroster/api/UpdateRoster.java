package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateRoster extends Utils
	{
	public static String method_level = "standard";
	public UpdateRoster(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int 
			
			contest_id = input.getInt("contest_id"),
			entry_id = input.getInt("entry_id");
			
			JSONArray roster = input.getJSONArray("roster");
			
			JSONObject contest = db.select_contest(contest_id);
			
			if (contest == null)
				{
				output.put("error", "Invalid contest ID: " + contest_id);
				break method;
				}
			
			if (contest.getInt("status") != 1)
				{
				output.put("error", "Registration has closed for this contest");
				break method;
				}
			
			JSONObject entry = db.select_entry(entry_id);
			
			if (entry == null)
				{
				output.put("error", "Invalid entry ID: " + entry_id);
				break method;
				}
			
			if (!entry.getString("user_id").equals(session.user_id()))
				{
				output.put("error", "Entry " + entry_id + " does not belong to you");
				break method;
				}

			if (entry.getInt("contest_id") != contest_id)
				{
				output.put("error", "Entry " + entry_id + " does not belong to contest " + contest_id);
				break method;
				}
						
			// if we get here, the active user is allowed to modify the roster in the supplied entry_id
			
			// validate roster size
			
			int
			
			roster_size_user = roster.length(),
			roster_size = contest.getInt("roster_size");
			
			if (roster_size != 0 && roster_size_user != roster_size)
				{
				output.put("error", "Invalid roster size");
				break method;
				}
			
			// make a map of system player prices to compare against
			
			JSONArray option_table = new JSONArray(contest.getString("option_table"));
			
			TreeMap<Integer, Double> pricing_table = new TreeMap<Integer, Double>();
			
			for (int i=0, limit=option_table.length(); i<limit; i++)
				{
				JSONObject player = option_table.getJSONObject(i);
				pricing_table.put(player.getInt("id"), player.getDouble("price"));
				}
			
			// validate user's player prices against system player prices
			
			double roster_total_salary = 0;

			for (int i=0; i<roster_size_user; i++)
				{
				JSONObject player = roster.getJSONObject(i);

				double 
				
				player_price_user = player.getDouble("price"),
				player_price_system = pricing_table.get(player.getInt("id"));
				
				if (player_price_user != player_price_system)
					{
					output.put("error", "Player prices have changed");
					break method;
					}
				
				roster_total_salary = add(roster_total_salary, player_price_user, 0);
				}
			
			// validate roster against salary cap
							
			double salary_cap = contest.getDouble("salary_cap");
			
			if (roster_total_salary > salary_cap)
				{
				output.put("error", "Roster has exceeded salary cap");
				break method;
				}
			
			// if we get here, roster is valid
			
			PreparedStatement create_entry = sql_connection.prepareStatement("update entry set entry_data = ? where id = ?");	
			create_entry.setString(1, roster.toString());
			create_entry.setInt(2, entry_id);
			create_entry.executeUpdate();
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}