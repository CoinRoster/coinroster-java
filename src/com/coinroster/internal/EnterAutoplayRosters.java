package com.coinroster.internal;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;

public class EnterAutoplayRosters extends Utils {
	
	public EnterAutoplayRosters(Connection sql_connection, int contest_template_id, int contest_id) throws Exception {
	
		DB db = new DB(sql_connection);
		ResultSet rs = null;
		SimpleDateFormat output_pattern = new SimpleDateFormat("yyyy-MM-dd");
		Date now = new Date();
		
		PreparedStatement get_autoplays = sql_connection.prepareStatement("select * from autoplay where active = 1 and contest_template_id = ? and start_date <= ? and end_date >= ?");
		get_autoplays.setInt(1, contest_template_id);
		get_autoplays.setString(2, output_pattern.format(now));
		get_autoplays.setString(3, output_pattern.format(now));
		rs = get_autoplays.executeQuery();
		
		int salary_cap, roster_size;
		JSONArray option_table; 
		ResultSet contest_data = db.get_data_for_autoplay(contest_id);
		if(contest_data.next()){
			option_table = new JSONArray(contest_data.getString(1));
			option_table = this.sortOptionTable(option_table);
			salary_cap = contest_data.getInt(2);
			roster_size = contest_data.getInt(3);
		}
		else{
			return;
		}
		
		// loop through active autoplays
		while(rs.next()){
			
			JSONObject input = new JSONObject();
			String user_id = rs.getString("user_id");
			String algorithm = rs.getString("algorithm");
			int number_of_entries = rs.getInt("num_rosters");
			boolean use_rc = true;
			
			double remaining_salary = salary_cap;
			double players_left = roster_size;
			JSONArray roster = new JSONArray();
			ArrayList<String> player_ids = new ArrayList<String>();
			
			switch(algorithm){
				
				case "BARBELL":
				
					double cheapest = option_table.getJSONObject(option_table.length() - 1).getDouble("price");
					for(int i = 0; i < option_table.length(); i++){
						
						String player_id = option_table.getJSONObject(i).getString("id");
						double price = option_table.getJSONObject(i).getDouble("price");
						
						if(price <= (remaining_salary - ((players_left - 1) * cheapest)) && players_left > 0){
							
							JSONObject drafted_player = new JSONObject();
							drafted_player.put("id", player_id);
							drafted_player.put("price", price);
							roster.put(drafted_player);
							player_ids.add(player_id);
							log("drafting " +  option_table.getJSONObject(i).getString("name") + " (id: " + player_id + ")");
							remaining_salary -= price;
							players_left--;
							
						}
						
					}
					log("amount spent: " + (salary_cap - remaining_salary));
					log("number of players drafted: " + (roster_size - players_left));
					break;
					
				
				case "SAFE":
					
					double price_to_start = remaining_salary / (double) roster_size;
					for(int i = 0; i < option_table.length(); i++){
					
						String player_id = option_table.getJSONObject(i).getString("id");
						double price = option_table.getJSONObject(i).getDouble("price");
						
						if(price <= price_to_start && players_left > 0){
							JSONObject drafted_player = new JSONObject();
							drafted_player.put("id", player_id);
							drafted_player.put("price", price);
							roster.put(drafted_player);
							player_ids.add(player_id);
							log("drafting " +  option_table.getJSONObject(i).getString("name") + " (id: " + player_id + ")");
							remaining_salary -= price;
							players_left--;
						}
					}
					log("amount spent: " + (salary_cap - remaining_salary));
					log("number of players drafted: " + (roster_size - players_left));
					break;
				
				
				case "RANDOM":
					
					double amount_per_player = salary_cap / roster_size;
					while(players_left > 0){
						JSONArray subset = new JSONArray();
						for(int i = 0; i < option_table.length(); i++){
							if(option_table.getJSONObject(i).getDouble("price") <= amount_per_player && !player_ids.contains(option_table.getJSONObject(i).getString("id")))
								subset.put(option_table.getJSONObject(i));
						}
						log(subset.toString());
						log("subset length = " + subset.length());
						int random_number = new Random().nextInt(subset.length());
						log("random num: " + random_number);
						JSONObject player_to_add = subset.getJSONObject(random_number);
						String player_id = player_to_add.getString("id");
						double price = player_to_add.getDouble("price");
						
						JSONObject drafted_player = new JSONObject();
						drafted_player.put("id", player_id);
						drafted_player.put("price", price);
						roster.put(drafted_player);
						player_ids.add(player_id);
						log("drafting " +  player_to_add.getString("name") + " (id: " + player_id + ")");
						remaining_salary -= price;
						players_left--;
					}
					log("amount spent: " + (salary_cap - remaining_salary));
					log("number of players drafted: " + (roster_size - players_left));
					break;
					
			}
			
			input.put("user_id", user_id);
			input.put("number_of_entries", number_of_entries);
			input.put("use_rc", use_rc);
			input.put("contest_id", contest_id);
			input.put("roster", roster);
			
			MethodInstance method = new MethodInstance();
			JSONObject output = new JSONObject("{\"status\":\"0\"}");
			method.input = input;
			method.output = output;
			method.session = null;
			method.internal_caller = true;
			method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateEntryRoster").getConstructor(MethodInstance.class);
				c.newInstance(method);
				log(method.output.toString());
			}
			catch(Exception e){
				Server.exception(e);
			}		
		}
	}
	
	public JSONArray sortOptionTable(JSONArray option_table) throws JSONException{
		
		JSONArray sorted = new JSONArray();
		List<JSONObject> jsonValues = new ArrayList<JSONObject>();
	    for (int i = 0; i < option_table.length(); i++) {
	        jsonValues.add(option_table.getJSONObject(i));
	    }
	    Collections.sort( jsonValues, new Comparator<JSONObject>() {

	    	private static final String KEY_NAME = "price";

	        @Override
	        public int compare(JSONObject a, JSONObject b) {
	            Integer valA = null, valB = null;
	            try {
	                valA = (Integer) a.get(KEY_NAME);
	                valB = (Integer) b.get(KEY_NAME);
	            } 
	            catch (JSONException e) {
	            	Server.exception(e);
	            }
	            return -valA.compareTo(valB);
	        }
	    });

	    for (int i = 0; i < option_table.length(); i++) {
	        sorted.put(jsonValues.get(i));
	    }
	    return sorted;
	}
}
