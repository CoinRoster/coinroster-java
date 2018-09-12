package com.coinroster.api;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

public class SetupRoster extends Utils{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public SetupRoster(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
				String today_str = LocalDate.now().format(formatter);
				
				JSONObject data = input.getJSONObject("data");
				log(data.toString());
							
				data.put("category", "FANTASYSPORTS");
				data.put("progressive", "");
				data.put("rake", 5);
				data.put("entries_per_user", data.getInt("max_rosters"));
				String desc = "Be careful not to include injured or inactive players<br>";
				
				data.put("odds_source", "");
				JSONArray empty = new JSONArray();
				
				switch(data.getString("settlement_type")){
					
					case "JACKPOT":
						JSONArray payouts = data.getJSONArray("jackpot_payouts");
						JSONArray pay_table = new JSONArray();
						for(int i=0; i < payouts.length(); i++){
							JSONObject entry = new JSONObject();
							entry.put("payout", payouts.get(i));
							entry.put("rank", i+1);
							pay_table.put(entry);
						}
						log("pay table: " + pay_table.toString());
						data.put("pay_table", pay_table.toString());
						data.put("min_users", 2);
						break;
					
					case "DOUBLE-UP":
						data.put("pay_table", empty.toString());
						data.put("min_users", 2);
						break;
					
					case "HEADS-UP":
						data.put("pay_table", empty.toString());
						data.put("min_users", 2);
						data.put("max_users", 2);
						break;
					
				}
				
				ResultSet options = null;
				String title = "";
				JSONArray option_table = null;
				double weight = 0;
				switch(data.getString("sub_category")){
					
					case "BASKETBALL":
						BasketballBot basketball_bot = new BasketballBot(sql_connection);
						basketball_bot.scrapeGameIDs();
						if(basketball_bot.getGameIDs() != null){
							JSONArray total_games = basketball_bot.getGames();
							ArrayList<String> games = new ArrayList<String>();
							JSONArray games_json = data.getJSONArray("gameIDs");
							boolean deadline_saved = false;
							Long deadline = null;
							for(int x = 0; x < total_games.length(); x++){
								String game = total_games.getJSONObject(x).getString("gameID");
								for(int i = 0; i < games_json.length(); i++){
									String gameID = games_json.getString(i);
									if(!games.contains(gameID)) games.add(gameID);
									if(game.equals(gameID) && !deadline_saved){
										deadline = total_games.getJSONObject(x).getLong("date_milli");
										deadline_saved = true;
									}
									
								}
							}
							data.put("salary_cap", 2000);
							// get weight for top player = 30% of salary cap
							weight = basketball_bot.getNormalizationWeight(0.3, games, data.getInt("salary_cap"));
							data.put("gameIDs", games.toString());
							options = db.getOptionTable(basketball_bot.sport, false, 0, games);
							data.put("registration_deadline", deadline);
							data.put("roster_size", 0);
							data.put("score_header", "Fantasy Points");
							
							title = today_str + " | " + data.getString("settlement_type");
							data.put("title", title);	
							String desc_append = appendDescription(data.getJSONObject("scoring_rules"));
							desc += desc_append;
							
						}else{
							output.put("error", "Basketball contests are not currently available");
							break method;
						}
						break;
						
					case "HOCKEY":
						HockeyBot hockey_bot = new HockeyBot(sql_connection);
						hockey_bot.scrapeGameIDs();
						if(hockey_bot.getGameIDs() != null){
							JSONArray total_games = hockey_bot.getGames();
							ArrayList<String> games = new ArrayList<String>();
							JSONArray games_json = data.getJSONArray("gameIDs");
							boolean deadline_saved = false;
							Long deadline = null;
							for(int x = 0; x < total_games.length(); x++){
								String game = total_games.getJSONObject(x).getString("gameID");
								for(int i = 0; i < games_json.length(); i++){
									String gameID = games_json.getString(i);
									if(!games.contains(gameID)) games.add(gameID);
									if(game.equals(gameID) && !deadline_saved){
										deadline = total_games.getJSONObject(x).getLong("date_milli");
										deadline_saved = true;
									}
									
								}
							}
							data.put("salary_cap", 2000);
							// get weight for top player = 30% of salary cap
							weight = hockey_bot.getNormalizationWeight(0.3, games, data.getInt("salary_cap"));
							data.put("gameIDs", games.toString());
							options = db.getOptionTable(hockey_bot.sport, false, 0, games);
							data.put("registration_deadline", deadline);
							data.put("roster_size", 0);
							data.put("score_header", "Fantasy Points");
							
							title = today_str + " | " + data.getString("settlement_type");
							data.put("title", title);	
							String desc_append = appendDescription(data.getJSONObject("scoring_rules"));
							desc += desc_append;
							
						}else{
							output.put("error", "Hockey contests are not currently available");
							break method;
						}
						break;
						
						
					case "BASEBALL":
						BaseballBot baseball_bot = new BaseballBot(sql_connection);
						baseball_bot.scrapeGameIDs();
						if(baseball_bot.getGameIDs() != null){
							JSONArray total_games = baseball_bot.getGames();
							ArrayList<String> games = new ArrayList<String>();
							JSONArray games_json = data.getJSONArray("gameIDs");
							boolean deadline_saved = false;
							Long deadline = null;
							for(int x = 0; x < total_games.length(); x++){
								String game = total_games.getJSONObject(x).getString("gameID");
								for(int i = 0; i < games_json.length(); i++){
									String gameID = games_json.getString(i);
									if(!games.contains(gameID)) games.add(gameID);
									if(game.equals(gameID) && !deadline_saved){
										deadline = total_games.getJSONObject(x).getLong("date_milli");
										deadline_saved = true;
									}
									
								}
							}
							data.put("gameIDs", games.toString());
							data.put("salary_cap", 2000);
							options = db.getOptionTable(baseball_bot.sport, false, 0, games);
							data.put("registration_deadline", deadline);
							data.put("roster_size", 0);
							weight = baseball_bot.getNormalizationWeight(0.3, games, data.getInt("salary_cap"));
							data.put("score_header", "Fantasy Points");
							
							title = today_str + " | " + data.getString("settlement_type");
							data.put("title", title);	
							String desc_append = appendDescription(data.getJSONObject("scoring_rules"));
							desc += desc_append;
							
						}else{
							output.put("error", "Baseball contests are not currently available");
							break method;
						}
						break;
						
					
					case "GOLF":
						GolfBot golf_bot = new GolfBot(sql_connection);
						int today = ContestMethods.getToday();
						golf_bot.scrapeTourneyID(today);
						if(golf_bot.getTourneyID() != null){
							title = golf_bot.getTourneyName() + " | ";
						
							data.put("gameIDs", golf_bot.getTourneyID());
							
							JSONObject scoring_rules;
							String score_header = "";
							String title_type = "";
							
							try{
								 scoring_rules = data.getJSONObject("scoring_rules");
								 score_header = "Fantasy Points";
								 title_type = "Multi-Stat";
								 String desc_append = appendDescription(data.getJSONObject("scoring_rules"));
								 desc += desc_append;
							}catch(Exception e){
								data.put("scoring_rules", "");
								score_header = "Score to Par";
								title_type = "Score to Par";
							}
							
							data.put("score_header", score_header);
							data.put("salary_cap", 10000);
							data.put("roster_size", 6);
							weight = golf_bot.getNormalizationWeight(0.3, data.getInt("salary_cap"));
							Long deadline = null;
							
							// figure out deadline
							JSONObject prop_data = data.getJSONObject("prop_data");
							switch(prop_data.getString("when")){
								case "tournament":
									deadline = golf_bot.getDeadline();
									title += "Full Tournament | " + title_type;
									break;
								case "1":
									deadline = golf_bot.getDeadline();
									title += "Round 1 | " + title_type;
									break;
								case "2":
									deadline = golf_bot.getDeadline() + 86400000;
									title += "Round 2 | " + title_type;
									break;
								case "3":
									deadline = golf_bot.getDeadline() + (2 * 86400000);
									title += "Round 3 | " + title_type;
									break;
								case "4":
									deadline = golf_bot.getDeadline() + (3 * 86400000);
									title += "Round 4 | " + title_type;
									break;
							}
							
							title += " | " + data.getString("settlement_type");
							
							data.put("registration_deadline", deadline);
							data.put("title", title);
							option_table = db.getGolfRosterOptionTable(golf_bot.getTourneyID(), weight);
						}
						else{
							output.put("error", "Golf contests are not currently available");
							break method;
						}
						break;		
				}
					
				if(!data.getString("sub_category").equals("GOLF")){	
					option_table = new JSONArray();
					while(options.next()){
						JSONObject player = new JSONObject();
						player.put("name", options.getString(2) + " " + options.getString(3));
						player.put("price", (int) Math.round(options.getDouble(4) * weight));
						player.put("id", options.getString(1));
						option_table.put(player);
					}
				}
				
				data.put("option_table", option_table);
				data.put("description", desc);
				
				try{
					data.put("scoring_rules", data.getJSONObject("scoring_rules").toString());
				}catch(JSONException e){
					data.put("scoring_rules", "");
				}
				try{
					data.put("prop_data", data.getJSONObject("prop_data").toString());
				}catch(JSONException e){
					data.put("prop_data", "");
				}
				
				MethodInstance prop_method = new MethodInstance();
				JSONObject prop_output = new JSONObject("{\"status\":\"0\"}");
				prop_method.input = data;
				prop_method.output = prop_output;
				prop_method.session = method.session;
				prop_method.internal_caller = true;
				prop_method.sql_connection = sql_connection;
				
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(prop_method);
					output = prop_method.output;
					output.put("status", "1");
				}
				catch(Exception e){
					Server.exception(e);
					output = prop_method.output;
				}
				
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.toString());
				output.put("status", "0");	
			}
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
				int value = scoring_rules.getInt(key);
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
	
}
