package com.coinroster.bots;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;


import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Utils;

public class CrowdSettleBot extends Utils{
	
	
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
		try {
			PreparedStatement get_contest_users = sql_connection.prepareStatement("select entry_data, amount from entry where contest_id=?");
			get_contest_users.setInt(1, contest_id);
			contest_users = get_contest_users.executeQuery();
			
			// assign number of votes to each entry
			Double max_amount = 0.0;
			HashMap<Integer, Double> entries = new HashMap<>();
			
			while(contest_users.next()) {
				if(!entries.containsKey(contest_users.getInt(1))) {
					entries.put(contest_users.getInt(1), contest_users.getDouble(2));
				} else {
					Double updated_amount = add(contest_users.getDouble(2), entries.get(contest_users.getInt(1)), 0);
					entries.put(contest_users.getInt(1), updated_amount);
				}
				
				// keep running max vote count
				if (max_amount < entries.get(contest_users.getInt(1))) {
					log("test5");
					log("max_amount: " + max_amount);
					max_amount = entries.get(contest_users.getInt(1));
					if (winning_outcome != contest_users.getInt(1)) {
						fields.put("multiple_bets", "true");
					}
					winning_outcome = contest_users.getInt(1);
				}
			}

			fields.put("winning_outcome", winning_outcome);
			log("winning outcome for crowd settled contest: " + winning_outcome);
		}
		catch(Exception e){
			e.printStackTrace(System.out);
			log(e.getCause());
		}
		
		return fields;
	}
	
}
