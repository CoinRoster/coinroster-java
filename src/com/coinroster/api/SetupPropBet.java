package com.coinroster.api;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.ContestMethods;
import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.GolfBot;
import com.coinroster.bots.HockeyBot;

public class SetupPropBet extends Utils{
	public static String method_level = "standard";

	public SetupPropBet(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				
				boolean is_fixed_odds = false;
				
				BaseballBot baseball_bot = null;
				BasketballBot basketball_bot = null;
				GolfBot golf_bot = null;
				HockeyBot hockey_bot = null;
				
				JSONObject data = input.getJSONObject("data");
				
				String category = "FANTASYSPORTS";
				String contest_type = "PARI-MUTUEL";
				String settlement_type = "PARI-MUTUEL";
				String sub_category = data.getString("sub_category");
				String sport = sub_category.replace("PROPS", "");
				boolean priv = data.getBoolean("private");
				double cost_per_entry = 0.000001;
				double rake = 5.0;
				String progressive = "";
				JSONObject scoring_rules, prop_data;
				String gameIDs = "";
				String sport_title = "", contest_title = "", date_name_title = "";
				String desc = "";
				try{
					scoring_rules = data.getJSONObject("scoring_rules");
				}catch(JSONException e){
					scoring_rules = new JSONObject();
				}
				
				try{
					prop_data = data.getJSONObject("prop_data");
				}catch(JSONException e){
					prop_data = new JSONObject();
				}
				
				
				// checks for fixed-odds and if so convert risk to String
				if (prop_data.has("risk")) {
					is_fixed_odds = true;
					String risk = Utils.format_btc(prop_data.getDouble("risk"));
					prop_data.remove("risk");
					prop_data.put("risk", risk);
				}
				
				Long deadline = null;
				switch(sport){
					
					case "BASEBALL":
						baseball_bot = new BaseballBot(sql_connection);
						baseball_bot.scrapeGameIDs();
						if(baseball_bot.getGameIDs() != null){
							date_name_title = Instant.ofEpochMilli(baseball_bot.getEarliestGame()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
						}else{
							output.put("error", "Baseball contests are not currently available");
							break method;
						}
						break;
					
					case "GOLF":
						golf_bot = new GolfBot(sql_connection);
						int today = ContestMethods.getToday();
						golf_bot.scrapeTourneyID(today);
						if(golf_bot.getTourneyID() != null){
							
							gameIDs  = golf_bot.getTourneyID();
							
							try{
								 scoring_rules = data.getJSONObject("scoring_rules");
								 sport_title = " | Multi-Stat";
								
							}catch(Exception e){
								sport_title = " | Score to Par";
							}
							
							try{
								// figure out deadline
								switch(prop_data.getString("when")){
									case "tournament":
										deadline = golf_bot.getDeadline();
										date_name_title = golf_bot.getTourneyName() + " | Full Tournament";
										break;
									case "1":
										deadline = golf_bot.getDeadline();
										date_name_title =golf_bot.getTourneyName() + " | Round 1";
										break;
									case "2":
										deadline = golf_bot.getDeadline() + 86400000;
										date_name_title = golf_bot.getTourneyName() + " | Round 2";
										break;
									case "3":
										deadline = golf_bot.getDeadline() + (2 * 86400000);
										date_name_title = golf_bot.getTourneyName() + " | Round 3";
										break;
									case "4":
										deadline = golf_bot.getDeadline() + (3 * 86400000);
										date_name_title = golf_bot.getTourneyName() + " | Round 4";
										break;
								}
							}catch(JSONException e){
								deadline = golf_bot.getDeadline();
								date_name_title = golf_bot.getTourneyName() + " | Full Tournament";
							}
							
						}else{
							output.put("error", "Golf contests are not currently available");
							break method;
						}
						break;
						
					case "BASKETBALL":
						basketball_bot = new BasketballBot(sql_connection);
						basketball_bot.scrapeGameIDs();
						if(basketball_bot.getGameIDs() != null){
							date_name_title = Instant.ofEpochMilli(basketball_bot.getEarliestGame()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
						}else{
							output.put("error", "Basketball contests are not currently available");
							break method;
						}
						break;
						
					case "HOCKEY":
						hockey_bot = new HockeyBot(sql_connection);
						hockey_bot.scrapeGameIDs();
						if(hockey_bot.getGameIDs() != null){
							date_name_title = Instant.ofEpochMilli(hockey_bot.getEarliestGame()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
						}else{
							output.put("error", "Hockey contests are not currently available");
							break method;
						}
						break;
					case "BITCOINS":
						Long registration_date = prop_data.getLong("registration_deadline"); //time of price index.
						Long settlement_date = prop_data.getLong("settlement_deadline");
						
						if (registration_date < System.currentTimeMillis() + 60 * 60 * 1000) {
							output.put("error", "Registration deadline too early.");
							break method;
						} else if (settlement_date < registration_date + 60 * 60 * 1000){
							output.put("error", "Settlement deadline too close to registration deadline.");
							break method;
						}
						date_name_title = new Date(registration_date).toString();
						deadline = registration_date;
						break;
					default:
						break;
				}
				
				String prop_type = prop_data.getString("prop_type");
				int auto_settle = 1;
				JSONArray option_table = new JSONArray();
				
				//check prop_type and set option_table, title, description accordingly
				switch(prop_type){
					case "MATCH_PLAY":
						
						contest_title = "Match Play";
						ArrayList<String> games = new ArrayList<String>();
						
						JSONObject tie = new JSONObject();
						tie.put("id", 1);
						tie.put("description", "Tie");
						if(is_fixed_odds) tie.put("odds", prop_data.getDouble("tie_odds"));
						option_table.put(tie);
						
						int index = 2;
						JSONArray players = prop_data.getJSONArray("players");
						for(int i=0; i < players.length(); i++){
							JSONObject player = players.getJSONObject(i);
							ResultSet info = db.get_player_info(sport, player.getString("id"));
							if(info.next()){
								String name = info.getString(1) + " " + info.getString(2);
								String gameID = info.getString(3);
								if(!games.contains(gameID))
									games.add(gameID);
								JSONObject p = new JSONObject();
								p.put("description", name);
								p.put("id", index);
								p.put("player_id", player.getString("id"));
								if (is_fixed_odds) 
									p.put("odds", player.getDouble("odds"));
								option_table.put(p);
								index += 1;
							}
						}
						
						gameIDs = games.toString();
						
						if(sport.equals("BASEBALL")){
							deadline = findEarliestGame(games, baseball_bot.getGames());
						}
						else if(sport.equals("BASKETBALL")){
							deadline = findEarliestGame(games, basketball_bot.getGames());
						}
						else if(sport.equals("HOCKEY")){
							deadline = findEarliestGame(games, hockey_bot.getGames());
						}
							
						if(sport.equals("GOLF") && prop_data.getString("multi_stp").equals("score_to_par")){
							desc += "Place your bets on which of the following golfers will finish the contest with the top score to par";
						}
						else{
							//one stat game
							if(scoring_rules.length() == 1){
								String key = (String) scoring_rules.keys().next();
								desc += "Place your bets on which player will get more " + key + "?<br>";
								desc += appendDescription(scoring_rules);
								
							}
							//multi-stat game
							else{
								desc += appendDescription(scoring_rules);
							}
						}
						break;
						
					case "OVER_UNDER":
						
						ResultSet info = db.get_player_info(sport, prop_data.getString("player_id"));
						String name = "";
						String game = "";
						
						double 
						over_odds = 0,
						under_odds = 0;
						
						if(info.next()){
							name = info.getString(1) + " " + info.getString(2);
							game = info.getString(3);
						}
												
						String o_u = String.valueOf(prop_data.getDouble("over_under_value"));
						contest_title = name + " Over/Under " + o_u;
						
						JSONObject over = new JSONObject();
						over.put("description", "Over " + o_u);
						over.put("id", 1);
						
						if(is_fixed_odds) {
							over_odds = prop_data.getDouble("over_odds");
							over.put("odds", over_odds);
						}
						
						option_table.put(over);
						
						JSONObject under = new JSONObject();
						under.put("description", "Under " + o_u);
						under.put("id", 2);
										
						if(is_fixed_odds) {
							under_odds = prop_data.getDouble("under_odds");
							under.put("odds", under_odds);
						}
						
						option_table.put(under);
						
						if(sport.equals("BASEBALL")){
							for(int i = 0; i < baseball_bot.getGames().length(); i++){ 
								if(baseball_bot.getGames().getJSONObject(i).getString("gameID").equals(game)){
									deadline = baseball_bot.getGames().getJSONObject(i).getLong("date_milli");
									break;
								}
							}
							ArrayList<String> gameIds = new ArrayList<String>();
							gameIds.add(game);
							gameIDs = gameIds.toString();
						}
						else if(sport.equals("BASKETBALL")){
							for(int i = 0; i < basketball_bot.getGames().length(); i++){ 
								if(basketball_bot.getGames().getJSONObject(i).getString("gameID").equals(game)){
									deadline = basketball_bot.getGames().getJSONObject(i).getLong("date_milli");
									break;
								}
							}
							ArrayList<String> gameIds = new ArrayList<String>();
							gameIds.add(game);
							gameIDs = gameIds.toString();
							
						}
						else if(sport.equals("HOCKEY")){
							for(int i = 0; i < hockey_bot.getGames().length(); i++){ 
								if(hockey_bot.getGames().getJSONObject(i).getString("gameID").equals(game)){
									deadline = hockey_bot.getGames().getJSONObject(i).getLong("date_milli");
									break;
								}
							}
							ArrayList<String> gameIds = new ArrayList<String>();
							gameIds.add(game);
							gameIDs = gameIds.toString();
							
						}
							
						if(sport.equals("GOLF") && prop_data.getString("multi_stp").equals("score_to_par")){
							desc += "Place your bets on if " + name + " will finish the contest over or under " + o_u + ".<br>";
							desc += "Note:<br>" + Math.floor(prop_data.getDouble("over_under_value")) + " is UNDER<br>";
							desc += Math.ceil(prop_data.getDouble("over_under_value")) + " is OVER";
						}
						else{
							//single stat
							if(scoring_rules.length() == 1){
								String key = (String) scoring_rules.keys().next();
								desc += "Place your bets on if " + name + " will get over/under "+ String.valueOf(prop_data.getDouble("over_under_value")) + " " + key + "?";
								desc += appendDescription(scoring_rules);
							}
							//multi-stat game
							else{
								desc += appendDescription(scoring_rules);
							}
						}
						break;
						
					case "NUMBER_SHOTS":
						
						info = db.get_player_info(sport, prop_data.getString("player_id"));
						name = "";
						if(info.next()){
							name = info.getString(1) + " " + info.getString(2);
						}
						String when = prop_data.getString("when");
						
						// make option table
						JSONObject any_other = new JSONObject();
						any_other.put("description", "Any other number");
						any_other.put("id", 1);
						option_table.put(any_other);
						
						// tournament prop
						if(when.equals("tournament")){
							for(int i = 0; i <= 60; i++){
								JSONObject item = new JSONObject();
								item.put("id", i+2);
								item.put("description", String.valueOf(i));
								option_table.put(item);
							}
							desc = "Place your bets on the number of " + prop_data.getString("shot").toUpperCase() + " " + name + " will accumulate in the entire tournament";
						}
						
						// just one round
						else{
							for(int i = 0; i <= 18; i++){
								JSONObject item = new JSONObject();
								item.put("id", i+2);
								item.put("description", String.valueOf(i));
								option_table.put(item);
							}
							desc = "Place your bets on the number of " + prop_data.getString("shot").toUpperCase() + " " + name + " will accumulate in Round " + when;
						}
						sport_title = "";
						contest_title = name + " Number of " + prop_data.getString("shot").toUpperCase();
						break;
						
					case "MAKE_CUT":
						info = db.get_player_info(sport, prop_data.getString("player_id"));
						name = "";
						if(info.next())
							name = info.getString(1) + " " + info.getString(2);
						
						JSONObject yes = new JSONObject();
						yes.put("description", "Yes");
						yes.put("id", 1);
						if(is_fixed_odds) yes.put("odds", prop_data.getDouble("yes_odds"));
						option_table.put(yes);
						
						JSONObject no = new JSONObject();
						no.put("description", "No");
						no.put("id", 2);
						if(is_fixed_odds) no.put("odds", prop_data.getDouble("no_odds"));
						option_table.put(no);
						
						log(option_table.toString());
						
						sport_title = "";
						contest_title = "Will " + name + " Make the Cut?";
						desc = "Place your bets on whether or not " + name + " will make the cut!";
						
						// pass in when: tournament
						prop_data.put("when", "tournament");
						break;
					
					case "OVER_UNDER_BTC":
						
						category = "FINANCIAL";
						sub_category = "BITCOINS";
						Date settlement_date = new Date(prop_data.getLong("settlement_deadline"));
						
						String over_under = String.valueOf(prop_data.getDouble("over_under_value"));
						desc = "Place bets on whether the bitcoin price index will be over or under " + over_under;
						desc += " by " + settlement_date.toString();
						
						
						
						JSONObject lower = new JSONObject();
						lower.put("description", "Under " + over_under + "BTC");
						lower.put("id", 1);
						option_table.put(lower);
						
						//Not sure about these table values, but should work for now.
						JSONObject higher = new JSONObject();
						higher.put("description",  "Over or equal to " + over_under + "BTC");
						higher.put("id", 2);
						option_table.put(higher);
						
						prop_data.put("BTC_index", over_under);
						break;
					
					default:
						log("Could not find prop bet with type = " + prop_type);
						output.put("error", "Could not find prop bet with type = " + prop_type);
						break;
				
				}
				
				String title = date_name_title + " | " + contest_title + sport_title;
				JSONObject prop = new JSONObject();
				prop.put("category", category);
				prop.put("sub_category", sub_category);
				prop.put("progressive", progressive);
				prop.put("contest_type", contest_type);
				prop.put("title", title);
				prop.put("description", desc);
				prop.put("registration_deadline", deadline);
				prop.put("rake", rake);
				prop.put("cost_per_entry", cost_per_entry);
				prop.put("settlement_type", settlement_type);
				prop.put("option_table", option_table);
				prop.put("auto_settle", auto_settle);
				prop.put("scoring_rules", scoring_rules.toString());
				prop.put("prop_data", prop_data.toString());
				prop.put("private", priv);
				prop.put("gameIDs", gameIDs);
				
				MethodInstance prop_method = new MethodInstance();
				JSONObject prop_output = new JSONObject("{\"status\":\"0\"}");
				prop_method.input = prop;
				prop_method.output = prop_output;
				prop_method.internal_caller = true;
				prop_method.session =  session;
				prop_method.sql_connection = sql_connection;
				
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(prop_method);
					output = prop_method.output;
					output.put("status", "1");
				}
				catch(Exception e){
					output = prop_method.output;
					Server.exception(e);
				}
				
				
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.toString());
				output.put("status", "0");
			}
//------------------------------------------------------------------------------------
		}
		method.response.send(output);
	}
	
	public String appendDescription(JSONObject scoring_rules){
		String desc = "Scoring Rules<br>";
		Iterator<?> keys = scoring_rules.keys();
		while(keys.hasNext()){
			String key = (String) keys.next();
			try {
				String value_str = "";
				Double value = scoring_rules.getDouble(key);
				if(value < 0)
					value_str = String.valueOf(value);
				else
					value_str = "+" + String.valueOf(value);
				
				desc += key.toUpperCase() + ": " + value_str + "<br>";
			}catch (Exception e) {
			}	
		}
		return desc;
	}
	
	public Long findEarliestGame(ArrayList<String> games, JSONArray gameData) throws JSONException{
		for(int i = 0; i < gameData.length(); i++){
			for(String game : games){
				if(gameData.getJSONObject(i).getString("gameID").equals(game)){
					return gameData.getJSONObject(i).getLong("date_milli");
				}
			}
		}
		return null;
	}
	
}