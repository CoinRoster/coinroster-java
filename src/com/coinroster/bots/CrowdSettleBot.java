package com.coinroster.bots;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Utils;

public class CrowdSettleBot {
	
	
	public static String method_level = "admin";

	// instance variables
	@SuppressWarnings("unused")
	private DB db;
	private static Connection sql_connection = null;
	
	// constructor
	public CrowdSettleBot(Connection sql_connection) throws IOException, JSONException{
		CrowdSettleBot.sql_connection = sql_connection;
		db = new DB(sql_connection);
	}
	
	public JSONObject settlePariMutuel(int contest_id) throws Exception{
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		int winning_outcome = 1;
		ResultSet contest_users = null;
		Utils.log("test");
		try {
			PreparedStatement get_contest_users = sql_connection.prepareStatement("select entry_data, user_id from entry where contest_id=?");
			get_contest_users.setInt(1, contest_id);
			contest_users = get_contest_users.executeQuery();

			// assign number of votes to each entry
			int max_winner = 0;
			HashMap<Integer, List<String>> entries = new HashMap<>();
			
			while(contest_users.next()) {
				if(!entries.containsKey(contest_users.getInt(1))) {
					List<String> users_for_option = new ArrayList<String>();
					users_for_option.add(contest_users.getString(2));
					entries.put(contest_users.getInt(1), users_for_option);
					
				} else {
					List<String> users_for_option = entries.get(contest_users.getInt(1));
					users_for_option.add(contest_users.getString(2));
					entries.put(contest_users.getInt(1), users_for_option);
				}
				
				// keep running max vote count
				if (max_winner < entries.get(contest_id).size()) {
					max_winner = entries.get(contest_id).size();
					winning_outcome = contest_id;
				}
			}
			
//			for (int i = 0; i < entries.keySet().size(); i ++) {
//				if (max_winner < entries.get(i).size()) {
//					max_winner = entries.get(i).size();
//					winning_outcome = i;
//				}
//			}
			fields.put("winning_outcome", winning_outcome);
			Utils.log("winning outcome for crowd settled contest: " + winning_outcome);
		}
		catch(Exception e){
			Utils.log(e.toString());
			e.printStackTrace();
		}
		
		return fields;
	}
	
}
