package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Utils;

public class UpdateDraftStatistics extends Utils
	{
	public UpdateDraftStatistics(Connection sql_connection, int contest_id) throws Exception
		{
		HashMap<Integer, Integer> draft_counts = new HashMap<Integer, Integer>();

		DB db = new DB(sql_connection);
		
		JSONObject contest = db.select_contest(contest_id);
		
		if (!contest.getString("contest_type").equals("ROSTER")) return;
		
		double cost_per_entry = contest.getDouble("cost_per_entry");
		
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		
		for (int i=0, limit=option_table.length(); i<limit; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			draft_counts.put(player.getInt("id"), 0);
			}
		
		JSONArray entries = db.select_contest_entries(contest_id);
		
		for (int i=0; i<entries.length(); i++)
			{
			JSONObject entry = entries.getJSONObject(i);
			
			int number_of_entries = (int) divide(entry.getDouble("amount"), cost_per_entry, 0);
			
			JSONArray entry_data = new JSONArray(entry.getString("entry_data"));
			
			for (int j=0; j<entry_data.length(); j++)
				{
				int 
				
				player_id = entry_data.getJSONObject(j).getInt("id"),
				
				current_draft_count = draft_counts.get(player_id);
				
				current_draft_count += number_of_entries;
				
				draft_counts.put(player_id, current_draft_count);
				}
			}
		
		for (int i=0, limit=option_table.length(); i<limit; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			
			int draft_count = draft_counts.get(player.getInt("id"));
			
			player.put("count", draft_count);
			
			option_table.put(i, player);
			}
		
		PreparedStatement update_contest = sql_connection.prepareStatement("update contest set option_table = ? where id = ?");
		update_contest.setString(1, option_table.toString());
		update_contest.setInt(2, contest_id);
		update_contest.executeUpdate();
		}
	}
