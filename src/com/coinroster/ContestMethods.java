package com.coinroster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.CrowdSettleBot;
import com.coinroster.bots.GolfBot;

public class ContestMethods extends Utils{

//------------------------------------------------------------------------------------

	// create basketball contests reading from csv
	public static void createBasketballContests() {
		
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			BasketballBot ball_bot = new BasketballBot(sql_connection);
			ball_bot.scrapeGameIDs();
			if(ball_bot.getGameIDs() == null)
				return;
			ball_bot.setup();
			ball_bot.savePlayers();
			
			Long deadline = ball_bot.getEarliestGame();
            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();

            //create Pari-Mutuel contest for most points
            JSONObject pari_mutuel_data = ball_bot.createPariMutuel(deadline, date.toString());
            MethodInstance pari_method = new MethodInstance();
			JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
			pari_method.input = pari_mutuel_data;
			pari_method.output = pari_output;
			pari_method.session = null;
			pari_method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(pari_method);
			}
			catch(Exception e){
				log(pari_method.output.toString());
				e.printStackTrace();
			}
			
			// read text file to create roster contests
			String fileName = Server.java_path + "BasketballContests.txt";
			String line = "";
			String sep = ";";
			
			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
				//skip the header
				br.readLine();
				while ((line = br.readLine()) != null) {
					JSONObject fields = new JSONObject();
					
					String[] contest = line.split(sep);
					
					// parameters for contest
					String category = "FANTASYSPORTS";		
					String contest_type = "ROSTER";
		            String settlement_type = contest[0];					
					String progressive_code = "";
					String title = contest[1] + " | " + date.toString(); 
					String desc = contest[2];
		            double rake = Double.parseDouble(contest[3]);
		            double cost_per_entry = Double.parseDouble(contest[4]);
		            int salary_cap = Integer.parseInt(contest[5]);
		            int min_users = Integer.parseInt(contest[6]);
		            int max_users = Integer.parseInt(contest[7]);
		            int entries_per_user = Integer.parseInt(contest[8]);
		            int roster_size = Integer.parseInt(contest[9]);
		            String score_header = contest[10];
		            String odds_source = "n/a";
		            if(!settlement_type.equals("JACKPOT")){
		            	JSONArray empty = new JSONArray();
		            	fields.put("pay_table", empty);
		            }
		            else{
			            String[] payouts_str = contest[11].split(",");
			            double[] payouts = new double[payouts_str.length];
			            for (int i = 0; i < payouts_str.length; i++) {
			                payouts[i] = Double.parseDouble(payouts_str[i]);
			            }
			            JSONArray pay_table = new JSONArray();
						for(int i=0; i < payouts.length; i++){
							JSONObject entry = new JSONObject();
							entry.put("payout", payouts[i]);
							entry.put("rank", i+1);
							pay_table.put(entry);
						}
						
						fields.put("pay_table", pay_table);
		            }
		       
		            fields.put("category", category);
					fields.put("sub_category", "BASKETBALL");
					fields.put("contest_type", contest_type);
					fields.put("progressive", progressive_code);
		            fields.put("settlement_type", settlement_type);
		            fields.put("title", title);
		            fields.put("description", desc);
		            fields.put("rake", rake);
		            fields.put("cost_per_entry", cost_per_entry);
		            fields.put("registration_deadline", deadline);
		            fields.put("odds_source", odds_source);
		            
		            ResultSet playerIDs = db.getAllPlayerIDs(ball_bot.sport);
		            JSONArray option_table = new JSONArray();
					while(playerIDs.next()){
						PreparedStatement get_player = sql_connection.prepareStatement("select name, team_abr, salary from player where id = ?");
						get_player.setInt(1, playerIDs.getInt(1));
						ResultSet player_data = get_player.executeQuery();
						if(player_data.next()){
							JSONObject player = new JSONObject();
							player.put("name", player_data.getString(1) + " " + player_data.getString(2));
							player.put("price", player_data.getDouble(3));
							player.put("count", 0);
							player.put("id", playerIDs.getInt(1));
							option_table.put(player);
						}
					}
					
					fields.put("option_table", option_table);
					fields.put("salary_cap", salary_cap);
					fields.put("min_users", min_users);		            
					fields.put("max_users", max_users);		            
					fields.put("entries_per_user", entries_per_user);
					fields.put("roster_size", roster_size);	
					fields.put("score_header", score_header);		            
					
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
				
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	

//------------------------------------------------------------------------------------

	// check to see if contests are in play, settle if necessary
	public static void checkBasketballContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			ArrayList<Integer> roster_contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "BASKETBALL", "ROSTER");
			ArrayList<Integer> pari_contest_ids = db_connection.get_pari_mutuel_id("BASKETBALL", "PARI-MUTUEL");

			if(!roster_contest_ids.isEmpty() || !pari_contest_ids.isEmpty()){
				BasketballBot ball_bot = new BasketballBot(sql_connection);
				log("Basketball contest is in play and minute is multiple of 20");
				ArrayList<String> gameIDs = db_connection.getAllGameIDsDB(ball_bot.sport);
				boolean games_ended;
				games_ended = ball_bot.scrape(gameIDs);
				JSONArray player_scores = ball_bot.updateScores();

				for(Integer contest_id : roster_contest_ids ){
					
					JSONObject fields = new JSONObject();
					fields.put("contest_id", contest_id);
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
					
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
					
				}
				if(games_ended){
					log("Basketball games have ended");
					for(Integer contest_id : roster_contest_ids){
						
						JSONObject fields = new JSONObject();
						fields.put("contest_id", contest_id);
						fields.put("normalization_scheme", "INTEGER");
						fields.put("player_scores", player_scores);
						
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					
					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
					for(Integer id : pari_contest_ids){
						JSONObject pari_fields = ball_bot.settlePariMutuel(id);
						MethodInstance pari_method = new MethodInstance();
						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
						pari_method.input = pari_fields;
						pari_method.output = pari_output;
						pari_method.session = null;
						pari_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(pari_method);
						}
						catch(Exception e){
							e.printStackTrace();
						}		
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	

//------------------------------------------------------------------------------------

	public static void createGolfContests() {
		
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			GolfBot golfBot = new GolfBot(sql_connection);
			DB db = new DB(sql_connection);
			golfBot.scrapeTourneyID();
			if(golfBot.getTourneyID() == null)
				return;
			golfBot.setup();
			golfBot.savePlayers();
			
			Long deadline = golfBot.getDeadline();

			//create Pari-Mutuel contest for most points
			Calendar cal = Calendar.getInstance();
			for(int round = 1; round <=4; round++){
				cal.setTimeInMillis(deadline);
				cal.add(Calendar.DATE, round-1);
				long round_deadline = cal.getTimeInMillis();
				JSONObject pari_mutuel_data = golfBot.createPariMutuel(round_deadline, String.valueOf(round));
	            MethodInstance pari_method = new MethodInstance();
				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
				pari_method.input = pari_mutuel_data;
				pari_method.output = pari_output;
				pari_method.session = null;
				pari_method.sql_connection = sql_connection;
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(pari_method);
				}
				catch(Exception e){
					log(pari_method.output.toString());
					e.printStackTrace();
				}	
			}

			// read text file to create roster contests
			String fileName = Server.java_path + "GolfContests.txt";
			String line = "";
			String sep = ";";
			
			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
				//skip the header
				br.readLine();
				while ((line = br.readLine()) != null) {
					JSONObject fields = new JSONObject();
					
					String[] contest = line.split(sep);
					
					// parameters for contest
					String category = "FANTASYSPORTS";		
					String contest_type = "ROSTER";
		            String settlement_type = contest[0];					
					String progressive_code = "";
					String title = golfBot.getTourneyName() + " " + contest[1];
					String desc = contest[2];
		            double rake = Double.parseDouble(contest[3]);
		            double cost_per_entry = Double.parseDouble(contest[4]);
		            int salary_cap = Integer.parseInt(contest[5]);
		            int min_users = Integer.parseInt(contest[6]);
		            int max_users = Integer.parseInt(contest[7]);
		            int entries_per_user = Integer.parseInt(contest[8]);
		            int roster_size = Integer.parseInt(contest[9]);
		            String score_header = contest[10];
		            String odds_source = "n/a";
		            if(!settlement_type.equals("JACKPOT")){
		            	JSONArray empty = new JSONArray();
		            	fields.put("pay_table", empty);
		            }
		            else{
			            String[] payouts_str = contest[11].split(",");
			            double[] payouts = new double[payouts_str.length];
			            for (int i = 0; i < payouts_str.length; i++) {
			                payouts[i] = Double.parseDouble(payouts_str[i]);
			            }
			            JSONArray pay_table = new JSONArray();
						for(int i=0; i < payouts.length; i++){
							JSONObject entry = new JSONObject();
							entry.put("payout", payouts[i]);
							entry.put("rank", i+1);
							pay_table.put(entry);
						}
						
						fields.put("pay_table", pay_table);
		            }
		       
		            fields.put("category", category);
					fields.put("sub_category", "GOLF");
					fields.put("contest_type", contest_type);
					fields.put("progressive", progressive_code);
		            fields.put("settlement_type", settlement_type);
		            fields.put("title", title);
		            fields.put("description", desc);
		            fields.put("rake", rake);
		            fields.put("cost_per_entry", cost_per_entry);
		            fields.put("registration_deadline", deadline);
		            fields.put("odds_source", odds_source);
		            fields.put("tourneyID", golfBot.getTourneyID());
		            
		            ResultSet playerIDs = db.getAllPlayerIDs(golfBot.sport);
		            JSONArray option_table = new JSONArray();
					while(playerIDs.next()){
						PreparedStatement get_player = sql_connection.prepareStatement("select name, salary from player where id = ?");
						get_player.setInt(1, playerIDs.getInt(1));
						ResultSet player_data = get_player.executeQuery();
						if(player_data.next()){
							JSONObject player = new JSONObject();
							String name = player_data.getString(1);
							String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
							String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
							player.put("name", nameNormalized);
							player.put("price", player_data.getDouble(2));
							player.put("count", 0);
							player.put("id", playerIDs.getInt(1));
							option_table.put(player);
						}
					}
					
					fields.put("option_table", option_table);
					fields.put("salary_cap", salary_cap);
					fields.put("min_users", min_users);		            
					fields.put("max_users", max_users);		            
					fields.put("entries_per_user", entries_per_user);
					fields.put("roster_size", roster_size);	
					fields.put("score_header", score_header);		            
					
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
				
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
	
//------------------------------------------------------------------------------------

	public static void updateGolfContestField() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			GolfBot golfBot = new GolfBot(sql_connection);
			boolean new_players = golfBot.appendLateAdditions();
			if(!new_players)
				log("no new golfers added to field");
		}
		catch (Exception e) {
			Server.exception(e);
		} 
		finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					log("closing sql_connection");
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}
			
	
//------------------------------------------------------------------------------------
	
	public static void checkGolfContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			ArrayList<Integer> roster_contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "GOLF", "ROSTER");
			ArrayList<Integer> pari_contest_ids = db_connection.get_pari_mutuel_id("GOLF", "PARI-MUTUEL");

			if(!roster_contest_ids.isEmpty() || !pari_contest_ids.isEmpty()){
				GolfBot golfBot = new GolfBot(sql_connection);
				log("Golf Contest is in play and minute is multiple of 20");
				String tourneyID = golfBot.getLiveTourneyID();
				boolean finished = golfBot.scrapeScores(tourneyID);
				
				//check to see if Pari-Mutuels are ready to be settled
				golfBot.checkPariMutuelStatus(pari_contest_ids);
				JSONArray player_map = golfBot.updateScoresDB();

				for(Integer contest_id : roster_contest_ids ){

					JSONObject fields = new JSONObject();
					fields.put("contest_id", contest_id);
					fields.put("normalization_scheme", "INTEGER-INVERT");
					fields.put("player_scores", player_map);

					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
					
				}
				if(finished){
					log("Golf tournament has ended");
					for(Integer contest_id : roster_contest_ids){
						
						JSONObject fields = new JSONObject();
						fields.put("contest_id", contest_id);
						fields.put("normalization_scheme", "INTEGER-INVERT");
						fields.put("player_scores", player_map);
						
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	

	//------------------------------------------------------------------------------------
	
		public static void checkCrowdContests() {

			Connection sql_connection = null;
			try {
				sql_connection = Server.sql_connection();
				DB db = new DB(sql_connection);
				
				ArrayList<Integer> voting_contest_ids = db.check_if_in_play("USERGENERATED", "VOTING", "PARI-MUTUEL");

				if(!voting_contest_ids.isEmpty()){
					CrowdSettleBot crowd_bot = new CrowdSettleBot(sql_connection);
					
					for(Integer contest_id : voting_contest_ids){
						
						JSONObject contest = db.select_contest(contest_id);
						if(new Date().getTime() > contest.getLong("settlement_deadline")) {
							
							// Settle Voting Round
							log("Voting round for contest: " + contest_id + " has ended, settling");
							JSONObject input = crowd_bot.settlePariMutuel(contest_id);
							input.put("contest_id", contest_id);

							MethodInstance method = new MethodInstance();
							JSONObject output = new JSONObject("{\"status\":\"0\"}");
							method.input = input;
							method.output = output;
							method.session = null;
							method.sql_connection = sql_connection;
							try{
								Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
								c.newInstance(method);
							}
							catch(Exception e){
								e.printStackTrace();
							}
							input.remove("contest_id");
							
							// get contest ID of contest that created voting round
							// (maybe find a better way of doing this)
							int original_contest_id = Integer.parseInt(contest.getString("created_by").replaceAll("[\\D]", ""));
							
							// settle original contest
//							JSONObject original_contest = db.select_contest(original_contest_id);
							input.put("contest_id", original_contest_id);							
							
							MethodInstance original_method = new MethodInstance();
							original_method.input = input;
							original_method.output = output;
							original_method.session = null;
							original_method.sql_connection = sql_connection;
							try{
								Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
								c.newInstance(method);
							}
							catch(Exception e){
								e.printStackTrace();
							}
						}
					}
				}
			} catch (Exception e) {
				Server.exception(e);
			} finally {
				if (sql_connection != null) {
					try {
						sql_connection.close();
						} 
					catch (SQLException ignore) {
						// ignore
					}
				}
			}
		}	
			
	//------------------------------------------------------------------------------------

	// create basketball contests reading from csv
	public static void createBaseballContests() {
		
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			BaseballBot baseball_bot = new BaseballBot(sql_connection);
			baseball_bot.scrapeGameIDs();
			if(baseball_bot.getGameIDs() == null)
				return;
			baseball_bot.setup();
			baseball_bot.savePlayers();
			
			Long deadline = baseball_bot.getEarliestGame();
            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();

            //create Pari-Mutuel contest for most points
            JSONObject pari_mutuel_data = baseball_bot.createPariMutuel(deadline, date.toString());
            MethodInstance pari_method = new MethodInstance();
			JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
			pari_method.input = pari_mutuel_data;
			pari_method.output = pari_output;
			pari_method.session = null;
			pari_method.sql_connection = sql_connection;
			try{
				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
				c.newInstance(pari_method);
			}
			catch(Exception e){
				log(pari_method.output.toString());
				e.printStackTrace();
			}
			
			// read text file to create roster contests
			String fileName = Server.java_path + "BaseballContests.txt";
			String line = "";
			String sep = ";";
			
			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
				//skip the header
				br.readLine();
				while ((line = br.readLine()) != null) {
					JSONObject fields = new JSONObject();
					
					String[] contest = line.split(sep);
					
					// parameters for contest
					String category = "FANTASYSPORTS";		
					String contest_type = "ROSTER";
		            String settlement_type = contest[0];
		            log("Settlement Type:" + settlement_type);
					String progressive_code = "";
					String title = contest[1] + " | " + date.toString(); 
					String desc = contest[2];
		            double rake = Double.parseDouble(contest[3]);
		            double cost_per_entry = Double.parseDouble(contest[4]);
		            int salary_cap = Integer.parseInt(contest[5]);
		            int min_users = Integer.parseInt(contest[6]);
		            int max_users = Integer.parseInt(contest[7]);
		            int entries_per_user = Integer.parseInt(contest[8]);
		            int roster_size = Integer.parseInt(contest[9]);
		            String score_header = contest[10];
		            String odds_source = "n/a";
		            if(!settlement_type.equals("JACKPOT")){
		            	JSONArray empty = new JSONArray();
		            	fields.put("pay_table", empty);
		            }
		            else{
			            String[] payouts_str = contest[11].split(",");
			            double[] payouts = new double[payouts_str.length];
			            for (int i = 0; i < payouts_str.length; i++) {
			                payouts[i] = Double.parseDouble(payouts_str[i]);
			            }
			            JSONArray pay_table = new JSONArray();
						for(int i=0; i < payouts.length; i++){
							JSONObject entry = new JSONObject();
							entry.put("payout", payouts[i]);
							entry.put("rank", i+1);
							pay_table.put(entry);
						}
						log(pay_table.toString());
						fields.put("pay_table", pay_table);
		            }
		       
		            fields.put("category", category);
					fields.put("sub_category", "BASEBALL");
					fields.put("contest_type", contest_type);
					fields.put("progressive", progressive_code);
		            fields.put("settlement_type", settlement_type);
		            fields.put("title", title);
		            fields.put("description", desc);
		            fields.put("rake", rake);
		            fields.put("cost_per_entry", cost_per_entry);
		            fields.put("registration_deadline", deadline);
		            fields.put("odds_source", odds_source);
		            
		            ResultSet playerIDs = db.getAllPlayerIDs(baseball_bot.sport);
		            JSONArray option_table = new JSONArray();
					while(playerIDs.next()){
						PreparedStatement get_player = sql_connection.prepareStatement("select name, team_abr, salary from player where id = ?");
						get_player.setInt(1, playerIDs.getInt(1));
						ResultSet player_data = get_player.executeQuery();
						if(player_data.next()){
							JSONObject player = new JSONObject();
							player.put("name", player_data.getString(1) + " " + player_data.getString(2));
							player.put("price", player_data.getDouble(3));
							player.put("count", 0);
							player.put("id", playerIDs.getInt(1));
							option_table.put(player);
						}
					}
					
					fields.put("option_table", option_table);
					fields.put("salary_cap", salary_cap);
					fields.put("min_users", min_users);		            
					fields.put("max_users", max_users);		            
					fields.put("entries_per_user", entries_per_user);
					fields.put("roster_size", roster_size);	
					fields.put("score_header", score_header);		            
					
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
				
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	

	// check to see if contests are in play, settle if necessary
	public static void checkBaseballContests() {
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db_connection = new DB(sql_connection);
			ArrayList<Integer> roster_contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "BASEBALL", "ROSTER");
			ArrayList<Integer> pari_contest_ids = db_connection.get_pari_mutuel_id("BASEBALL", "PARI-MUTUEL");

			if(!roster_contest_ids.isEmpty() || !pari_contest_ids.isEmpty()){
				BaseballBot baseball_bot = new BaseballBot(sql_connection);
				log("Baseball contest is in play and minute is multiple of 20");
				ArrayList<String> gameIDs = db_connection.getAllGameIDsDB(baseball_bot.sport);
				boolean games_ended;
				games_ended = baseball_bot.scrape(gameIDs);
				JSONArray player_scores = baseball_bot.updateScores();

				for(Integer contest_id : roster_contest_ids ){
					
					JSONObject fields = new JSONObject();
					fields.put("contest_id", contest_id);
					fields.put("normalization_scheme", "INTEGER");
					fields.put("player_scores", player_scores);
					
					MethodInstance method = new MethodInstance();
					JSONObject output = new JSONObject("{\"status\":\"0\"}");
					method.input = fields;
					method.output = output;
					method.session = null;
					method.sql_connection = sql_connection;
					try{
						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
						c.newInstance(method);
					}
					catch(Exception e){
						e.printStackTrace();
					}
					
				}
				if(games_ended){
					log("Baseball games have ended");
					for(Integer contest_id : roster_contest_ids){
						
						JSONObject fields = new JSONObject();
						fields.put("contest_id", contest_id);
						fields.put("normalization_scheme", "INTEGER");
						fields.put("player_scores", player_scores);
						
						MethodInstance method = new MethodInstance();
						JSONObject output = new JSONObject("{\"status\":\"0\"}");
						method.input = fields;
						method.output = output;
						method.session = null;
						method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(method);
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					
					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
					for(Integer id : pari_contest_ids){
						JSONObject pari_fields = baseball_bot.settlePariMutuel(id);
						MethodInstance pari_method = new MethodInstance();
						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
						pari_method.input = pari_fields;
						pari_method.output = pari_output;
						pari_method.session = null;
						pari_method.sql_connection = sql_connection;
						try{
							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
							c.newInstance(pari_method);
						}
						catch(Exception e){
							e.printStackTrace();
						}		
					}
				}
			}
		} catch (Exception e) {
			Server.exception(e);
		} finally {
			if (sql_connection != null) {
				try {
					sql_connection.close();
					} 
				catch (SQLException ignore) {
					// ignore
				}
			}
		}
	}	
	
}
