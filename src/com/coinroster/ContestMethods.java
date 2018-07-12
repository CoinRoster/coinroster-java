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
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.bots.BaseballBot;

import com.coinroster.bots.CrowdSettleBot;
import com.coinroster.internal.UpdateContestStatus;


public class ContestMethods extends Utils{

//------------------------------------------------------------------------------------

	// create basketball contests reading from csv
//	public static void createBasketballContests() {
//		
//		Connection sql_connection = null;
//		
//		try {
//			sql_connection = Server.sql_connection();
//			DB db = new DB(sql_connection);
//			BasketballBot ball_bot = new BasketballBot(sql_connection);
//			String gameID_array = ball_bot.scrapeGameIDs();
//			if(ball_bot.getGameIDs() == null)
//				return;
//			ball_bot.setup();
//			ball_bot.savePlayers();
//			
//			Long deadline = ball_bot.getEarliestGame();
//            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
//
//            //create Pari-Mutuel contest for most points
//            JSONObject pari_mutuel_data = ball_bot.createPariMutuel(deadline, date.toString());
//            MethodInstance pari_method = new MethodInstance();
//			JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
//			pari_method.input = pari_mutuel_data;
//			pari_method.output = pari_output;
//			pari_method.session = null;
//			pari_method.sql_connection = sql_connection;
//			try{
//				Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
//				c.newInstance(pari_method);
//			}
//			catch(Exception e){
//				log(pari_method.output.toString());
//				e.printStackTrace();
//			}
//			
//			// read text file to create roster contests
//			String fileName = Server.java_path + "BasketballContests.txt";
//			String line = "";
//			String sep = ";";
//			
//			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//				//skip the header
//				br.readLine();
//				while ((line = br.readLine()) != null) {
//					JSONObject fields = new JSONObject();
//					
//					String[] contest = line.split(sep);
//					
//					// parameters for contest
//					String category = "FANTASYSPORTS";		
//					String contest_type = "ROSTER";
//		            String settlement_type = contest[0];					
//					String progressive_code = "";
//					String title = contest[1] + " | " + date.toString(); 
//					String desc = contest[2];
//		            double rake = Double.parseDouble(contest[3]);
//		            double cost_per_entry = Double.parseDouble(contest[4]);
//		            int salary_cap = Integer.parseInt(contest[5]);
//		            int min_users = Integer.parseInt(contest[6]);
//		            int max_users = Integer.parseInt(contest[7]);
//		            int entries_per_user = Integer.parseInt(contest[8]);
//		            int roster_size = Integer.parseInt(contest[9]);
//		            String score_header = contest[10];
//		            String odds_source = "n/a";
//		            if(!settlement_type.equals("JACKPOT")){
//		            	JSONArray empty = new JSONArray();
//		            	fields.put("pay_table", empty);
//		            }
//		            else{
//			            String[] payouts_str = contest[11].split(",");
//			            double[] payouts = new double[payouts_str.length];
//			            for (int i = 0; i < payouts_str.length; i++) {
//			                payouts[i] = Double.parseDouble(payouts_str[i]);
//			            }
//			            JSONArray pay_table = new JSONArray();
//						for(int i=0; i < payouts.length; i++){
//							JSONObject entry = new JSONObject();
//							entry.put("payout", payouts[i]);
//							entry.put("rank", i+1);
//							pay_table.put(entry);
//						}
//						
//						fields.put("pay_table", pay_table);
//		            }
//		       
//		            fields.put("category", category);
//					fields.put("sub_category", "BASKETBALL");
//					fields.put("contest_type", contest_type);
//					fields.put("progressive", progressive_code);
//		            fields.put("settlement_type", settlement_type);
//		            fields.put("title", title);
//		            fields.put("description", desc);
//		            fields.put("rake", rake);
//		            fields.put("cost_per_entry", cost_per_entry);
//		            fields.put("registration_deadline", deadline);
//		            fields.put("odds_source", odds_source);
//		            fields.put("gameIDs", gameID_array);
//
//		            ResultSet playerIDs = db.getAllPlayerIDs(ball_bot.sport);
//		            JSONArray option_table = new JSONArray();
//					while(playerIDs.next()){
//						PreparedStatement get_player = sql_connection.prepareStatement("select name, team_abr, salary from player where id = ?");
//						get_player.setInt(1, playerIDs.getInt(1));
//						ResultSet player_data = get_player.executeQuery();
//						if(player_data.next()){
//							JSONObject player = new JSONObject();
//							player.put("name", player_data.getString(1) + " " + player_data.getString(2));
//							player.put("price", player_data.getDouble(3));
//							player.put("count", 0);
//							player.put("id", playerIDs.getInt(1));
//							option_table.put(player);
//						}
//					}
//					
//					fields.put("option_table", option_table);
//					fields.put("salary_cap", salary_cap);
//					fields.put("min_users", min_users);		            
//					fields.put("max_users", max_users);		            
//					fields.put("entries_per_user", entries_per_user);
//					fields.put("roster_size", roster_size);	
//					fields.put("score_header", score_header);		            
//					
//					MethodInstance method = new MethodInstance();
//					JSONObject output = new JSONObject("{\"status\":\"0\"}");
//					method.input = fields;
//					method.output = output;
//					method.session = null;
//					method.sql_connection = sql_connection;
//					try{
//						Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
//						c.newInstance(method);
//					}
//					catch(Exception e){
//						e.printStackTrace();
//					}
//				}
//
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        }
//				
//		} catch (Exception e) {
//			Server.exception(e);
//		} finally {
//			if (sql_connection != null) {
//				try {
//					sql_connection.close();
//					} 
//				catch (SQLException ignore) {
//					// ignore
//				}
//			}
//		}
//	}	

//------------------------------------------------------------------------------------

	// check to see if contests are in play, settle if necessary
//	public static void checkBasketballContests() {
//		Connection sql_connection = null;
//		try {
//			sql_connection = Server.sql_connection();
//			DB db_connection = new DB(sql_connection);
//			ArrayList<Integer> roster_contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "BASKETBALL", "ROSTER");
//			ArrayList<Integer> pari_contest_ids = db_connection.get_pari_mutuel_id("BASKETBALL", "PARI-MUTUEL");
//
//			if(!roster_contest_ids.isEmpty() || !pari_contest_ids.isEmpty()){
//				BasketballBot ball_bot = new BasketballBot(sql_connection);
//				log("Basketball contest is in play and minute is multiple of 20");
//				ArrayList<String> gameIDs = db_connection.getAllGameIDsDB(ball_bot.sport);
//				boolean games_ended;
//				games_ended = ball_bot.scrape(gameIDs);
//				JSONArray player_scores = ball_bot.updateScores();
//
//				for(Integer contest_id : roster_contest_ids ){
//					
//					JSONObject fields = new JSONObject();
//					fields.put("contest_id", contest_id);
//					fields.put("normalization_scheme", "INTEGER");
//					fields.put("player_scores", player_scores);
//					
//					MethodInstance method = new MethodInstance();
//					JSONObject output = new JSONObject("{\"status\":\"0\"}");
//					method.input = fields;
//					method.output = output;
//					method.session = null;
//					method.sql_connection = sql_connection;
//					try{
//						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
//						c.newInstance(method);
//					}
//					catch(Exception e){
//						e.printStackTrace();
//					}
//					
//				}
//				if(games_ended){
//					log("Basketball games have ended");
//					for(Integer contest_id : roster_contest_ids){
//						
//						JSONObject fields = new JSONObject();
//						fields.put("contest_id", contest_id);
//						fields.put("normalization_scheme", "INTEGER");
//						fields.put("player_scores", player_scores);
//						
//						MethodInstance method = new MethodInstance();
//						JSONObject output = new JSONObject("{\"status\":\"0\"}");
//						method.input = fields;
//						method.output = output;
//						method.session = null;
//						method.sql_connection = sql_connection;
//						try{
//							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
//							c.newInstance(method);
//						}
//						catch(Exception e){
//							e.printStackTrace();
//						}
//					}
//					
//					//SETTLE PARIMUTUELS FROM NIGHT'S GAMES
//					for(Integer id : pari_contest_ids){
//						JSONObject pari_fields = ball_bot.settlePariMutuel(id);
//						MethodInstance pari_method = new MethodInstance();
//						JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
//						pari_method.input = pari_fields;
//						pari_method.output = pari_output;
//						pari_method.session = null;
//						pari_method.sql_connection = sql_connection;
//						try{
//							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
//							c.newInstance(pari_method);
//						}
//						catch(Exception e){
//							e.printStackTrace();
//						}		
//					}
//				}
//			}
//		} catch (Exception e) {
//			Server.exception(e);
//		} finally {
//			if (sql_connection != null) {
//				try {
//					sql_connection.close();
//					} 
//				catch (SQLException ignore) {
//					// ignore
//				}
//			}
//		}
//	}	

//------------------------------------------------------------------------------------

//	public static void createGolfContests() {
//		
//		Connection sql_connection = null;
//		
//		try {
//			sql_connection = Server.sql_connection();
//			GolfBot golfBot = new GolfBot(sql_connection);
//			DB db = new DB(sql_connection);
//			golfBot.scrapeTourneyID();
//			if(golfBot.getTourneyID() == null)
//				return;
//			golfBot.setup();
//			golfBot.savePlayers();
//			
//			Long deadline = golfBot.getDeadline();
//
//			//create Pari-Mutuel contest for most points
//			Calendar cal = Calendar.getInstance();
//			for(int round = 1; round <=4; round++){
//				cal.setTimeInMillis(deadline);
//				cal.add(Calendar.DATE, round-1);
//				long round_deadline = cal.getTimeInMillis();
//				JSONObject pari_mutuel_data = golfBot.createPariMutuel(round_deadline, String.valueOf(round));
//	            MethodInstance pari_method = new MethodInstance();
//				JSONObject pari_output = new JSONObject("{\"status\":\"0\"}");
//				pari_method.input = pari_mutuel_data;
//				pari_method.output = pari_output;
//				pari_method.session = null;
//				pari_method.sql_connection = sql_connection;
//				try{
//					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
//					c.newInstance(pari_method);
//				}
//				catch(Exception e){
//					log(pari_method.output.toString());
//					e.printStackTrace();
//				}	
//			}
//
//			// read text file to create roster contests
//			String fileName = Server.java_path + "GolfContests.txt";
//			String line = "";
//			String sep = ";";
//			
//			try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
//				//skip the header
//				br.readLine();
//				while ((line = br.readLine()) != null) {
//					JSONObject fields = new JSONObject();
//					
//					String[] contest = line.split(sep);
//					
//					// parameters for contest
//					String category = "FANTASYSPORTS";		
//					String contest_type = "ROSTER";
//		            String settlement_type = contest[0];					
//					String progressive_code = "";
//					String title = golfBot.getTourneyName() + " " + contest[1];
//					String desc = contest[2];
//		            double rake = Double.parseDouble(contest[3]);
//		            double cost_per_entry = Double.parseDouble(contest[4]);
//		            int salary_cap = Integer.parseInt(contest[5]);
//		            int min_users = Integer.parseInt(contest[6]);
//		            int max_users = Integer.parseInt(contest[7]);
//		            int entries_per_user = Integer.parseInt(contest[8]);
//		            int roster_size = Integer.parseInt(contest[9]);
//		            String score_header = contest[10];
//		            String odds_source = "n/a";
//		            if(!settlement_type.equals("JACKPOT")){
//		            	JSONArray empty = new JSONArray();
//		            	fields.put("pay_table", empty);
//		            }
//		            else{
//			            String[] payouts_str = contest[11].split(",");
//			            double[] payouts = new double[payouts_str.length];
//			            for (int i = 0; i < payouts_str.length; i++) {
//			                payouts[i] = Double.parseDouble(payouts_str[i]);
//			            }
//			            JSONArray pay_table = new JSONArray();
//						for(int i=0; i < payouts.length; i++){
//							JSONObject entry = new JSONObject();
//							entry.put("payout", payouts[i]);
//							entry.put("rank", i+1);
//							pay_table.put(entry);
//						}
//						
//						fields.put("pay_table", pay_table);
//		            }
//		       
//		            fields.put("category", category);
//					fields.put("sub_category", "GOLF");
//					fields.put("contest_type", contest_type);
//					fields.put("progressive", progressive_code);
//		            fields.put("settlement_type", settlement_type);
//		            fields.put("title", title);
//		            fields.put("description", desc);
//		            fields.put("rake", rake);
//		            fields.put("cost_per_entry", cost_per_entry);
//		            fields.put("registration_deadline", deadline);
//		            fields.put("odds_source", odds_source);
//		            fields.put("tourneyID", golfBot.getTourneyID());
//		            
//		            ResultSet playerIDs = db.getAllPlayerIDs(golfBot.sport);
//		            JSONArray option_table = new JSONArray();
//					while(playerIDs.next()){
//						PreparedStatement get_player = sql_connection.prepareStatement("select name, salary from player where id = ?");
//						get_player.setInt(1, playerIDs.getInt(1));
//						ResultSet player_data = get_player.executeQuery();
//						if(player_data.next()){
//							JSONObject player = new JSONObject();
//							String name = player_data.getString(1);
//							String name2 = Normalizer.normalize(name, Normalizer.Form.NFD);
//							String nameNormalized = name2.replaceAll("[^\\p{ASCII}]", "");
//							player.put("name", nameNormalized);
//							player.put("price", player_data.getDouble(2));
//							player.put("count", 0);
//							player.put("id", playerIDs.getInt(1));
//							option_table.put(player);
//						}
//					}
//					
//					fields.put("option_table", option_table);
//					fields.put("salary_cap", salary_cap);
//					fields.put("min_users", min_users);		            
//					fields.put("max_users", max_users);		            
//					fields.put("entries_per_user", entries_per_user);
//					fields.put("roster_size", roster_size);	
//					fields.put("score_header", score_header);		            
//					
//					MethodInstance method = new MethodInstance();
//					JSONObject output = new JSONObject("{\"status\":\"0\"}");
//					method.input = fields;
//					method.output = output;
//					method.session = null;
//					method.sql_connection = sql_connection;
//					try{
//						Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
//						c.newInstance(method);
//					}
//					catch(Exception e){
//						e.printStackTrace();
//					}
//				}
//
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        }
//				
//		} catch (Exception e) {
//			Server.exception(e);
//		} finally {
//			if (sql_connection != null) {
//				try {
//					sql_connection.close();
//					} 
//				catch (SQLException ignore) {
//					// ignore
//				}
//			}
//		}
//	}
//	
////------------------------------------------------------------------------------------
//
//	public static void updateGolfContestField() {
//		Connection sql_connection = null;
//		try {
//			sql_connection = Server.sql_connection();
//			GolfBot golfBot = new GolfBot(sql_connection);
//			boolean new_players = golfBot.appendLateAdditions();
//			if(!new_players)
//				log("no new golfers added to field");
//		}
//		catch (Exception e) {
//			Server.exception(e);
//		} 
//		finally {
//			if (sql_connection != null) {
//				try {
//					sql_connection.close();
//					log("closing sql_connection");
//					} 
//				catch (SQLException ignore) {
//					// ignore
//				}
//			}
//		}
//	}
			
	
//------------------------------------------------------------------------------------

	public static void checkCrowdContests() {
	
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			
			// get voting round contests that have status 1
			ArrayList<Integer> voting_contest_ids = db.check_if_in_play();
			
			if(!voting_contest_ids.isEmpty()){
				CrowdSettleBot crowd_bot = new CrowdSettleBot(sql_connection);
				
				// iterate through list of ids and check for contests whose registration deadline expired
				for(Integer contest_id : voting_contest_ids){
					
					JSONObject contest = db.select_contest(contest_id);
					if(new Date().getTime() > contest.getLong("registration_deadline")) {
						
						// Settle Voting Round
						log("Voting round for contest: " + contest_id + " has ended, settling");
						JSONObject input = crowd_bot.settlePariMutuel(contest_id);
						input.put("contest_id", contest_id);
						log(input.toString());
						
						// multiple bets placed, notify admin
						if(input.has("multiple_bets")) {
							JSONObject cash_register = db.select_user("username", "internal_cash_register");
							
							String
							
							cash_register_email_address = cash_register.getString("email_address"),
							cash_register_admin = "Cash Register Admin",
							
							subject_admin = "Crowd Settled Contest Has Multiple Entries",
							message_body_admin = "";
							
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "A crowd settled contest has entries that users have placed on multiple options. Please settle the contest below:";
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "Contest ID: <b>" + contest_id + "</b>";
							message_body_admin += "<br/>";
							message_body_admin += "<br/>";
							message_body_admin += "Please settle the contest from the admin panel.";
							message_body_admin += "<br/>";
			
							Server.send_mail(cash_register_email_address, cash_register_admin, subject_admin, message_body_admin);
							new UpdateContestStatus(sql_connection, contest_id, 5);
							return;
						}
	
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
						
						int original_contest_id = db.get_original_contest(contest_id);
						
						// settle original contest
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
//	public static void checkGolfContests() {
//		Connection sql_connection = null;
//		try {
//			sql_connection = Server.sql_connection();
//			DB db_connection = new DB(sql_connection);
//			ArrayList<Integer> roster_contest_ids = db_connection.check_if_in_play("FANTASYSPORTS", "GOLF", "ROSTER");
//			ArrayList<Integer> pari_contest_ids = db_connection.get_pari_mutuel_id("GOLF", "PARI-MUTUEL");
//
//			if(!roster_contest_ids.isEmpty() || !pari_contest_ids.isEmpty()){
//				GolfBot golfBot = new GolfBot(sql_connection);
//				log("Golf Contest is in play and minute is multiple of 20");
//				String tourneyID = golfBot.getLiveTourneyID();
//				boolean finished = golfBot.scrapeScores(tourneyID);
//				
//				//check to see if Pari-Mutuels are ready to be settled
//				golfBot.checkPariMutuelStatus(pari_contest_ids);
//				JSONArray player_map = golfBot.updateScoresDB();
//
//				for(Integer contest_id : roster_contest_ids ){
//
//					JSONObject fields = new JSONObject();
//					fields.put("contest_id", contest_id);
//					fields.put("normalization_scheme", "INTEGER-INVERT");
//					fields.put("player_scores", player_map);
//
//					MethodInstance method = new MethodInstance();
//					JSONObject output = new JSONObject("{\"status\":\"0\"}");
//					method.input = fields;
//					method.output = output;
//					method.session = null;
//					method.sql_connection = sql_connection;
//					try{
//						Constructor<?> c = Class.forName("com.coinroster.api." + "UpdateScores").getConstructor(MethodInstance.class);
//						c.newInstance(method);
//					}
//					catch(Exception e){
//						e.printStackTrace();
//					}
//					
//				}
//				if(finished){
//					log("Golf tournament has ended");
//					for(Integer contest_id : roster_contest_ids){
//						
//						JSONObject fields = new JSONObject();
//						fields.put("contest_id", contest_id);
//						fields.put("normalization_scheme", "INTEGER-INVERT");
//						fields.put("player_scores", player_map);
//						
//						MethodInstance method = new MethodInstance();
//						JSONObject output = new JSONObject("{\"status\":\"0\"}");
//						method.input = fields;
//						method.output = output;
//						method.session = null;
//						method.sql_connection = sql_connection;
//						try{
//							Constructor<?> c = Class.forName("com.coinroster.api." + "SettleContest").getConstructor(MethodInstance.class);
//							c.newInstance(method);
//						}
//						catch(Exception e){
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//		} catch (Exception e) {
//			Server.exception(e);
//		} finally {
//			if (sql_connection != null) {
//				try {
//					sql_connection.close();
//					} 
//				catch (SQLException ignore) {
//					// ignore
//				}
//			}
//		}
//	}	
		
	//------------------------------------------------------------------------------------

	// create basketball contests reading from csv
	public static void createBaseballContests() {
		
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			DB db = new DB(sql_connection);
			BaseballBot baseball_bot = new BaseballBot(sql_connection);
			String gameID_array = baseball_bot.scrapeGameIDs();
			if(baseball_bot.getGameIDs() == null)
				return;
			baseball_bot.setup();
			baseball_bot.savePlayers();
			
			Long deadline = baseball_bot.getEarliestGame();
            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();

            JSONArray prop_contests = db.getRosterTemplates("BASEBALLPROPS");
            for(int i = 0; i < prop_contests.length(); i++){
				JSONObject contest = prop_contests.getJSONObject(i);
				String title = contest.getString("title")  + " | " + date.toString(); 
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
				
	            JSONObject pari_mutuel_data = baseball_bot.createPariMutuel(deadline, date.toString(), contest);
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
			
			//read templates from `CONTEST_TEMPLATES` table
			JSONArray roster_contests = db.getRosterTemplates("BASEBALL");
			for(int index = 0; index < roster_contests.length(); index++){
				JSONObject contest = roster_contests.getJSONObject(index);
				String title = contest.getString("title")  + " | " + date.toString(); 
				contest.put("title", title);
				contest.put("odds_source", "n/a");
				contest.put("gameIDs", gameID_array);
				contest.put("registration_deadline", deadline);
				ResultSet options;
				if(contest.getInt("filter") == 0){
					options = db.getOptionTable(baseball_bot.sport, false, 0);
				}
				else{
					options = db.getOptionTable(baseball_bot.sport, true, contest.getInt("filter"));
				}
					
	            JSONArray option_table = new JSONArray();
				while(options.next()){
					JSONObject player = new JSONObject();
					player.put("name", options.getString(2) + " " + options.getString(3));
					player.put("price", options.getDouble(4));
					player.put("count", 0);
					player.put("id", options.getInt(1));
					option_table.put(player);
				}
				contest.put("option_table", option_table);
				MethodInstance method = new MethodInstance();
				JSONObject output = new JSONObject("{\"status\":\"0\"}");
				method.input = contest;
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
			JSONObject roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASEBALL", "ROSTER");
			JSONObject pari_contests = db_connection.get_active_pari_mutuels("BASEBALLPROPS", "PARI-MUTUEL");

			if(!(roster_contests.length() == 0) || !(pari_contests.length() == 0)){
				BaseballBot baseball_bot = new BaseballBot(sql_connection);
				log("Baseball games are in play and minute is multiple of 20");
				ArrayList<String> gameIDs = db_connection.getAllGameIDsDB(baseball_bot.sport);
				boolean games_ended;
				games_ended = baseball_bot.scrape(gameIDs);

				Iterator<?> roster_contest_ids = roster_contests.keys();
				while(roster_contest_ids.hasNext()){
					String c_id = (String) roster_contest_ids.next();
					String scoring_rules_string = roster_contests.getString(c_id);
					JSONObject scoring_rules = new JSONObject(scoring_rules_string);
					
					JSONArray player_scores = baseball_bot.updateScores(scoring_rules);
					JSONObject fields = new JSONObject();
					fields.put("contest_id", Integer.parseInt(c_id));
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
					log("baseball games have ended. Settling now...");
					roster_contests = db_connection.check_if_in_play("FANTASYSPORTS", "BASEBALL", "ROSTER");
					roster_contest_ids = roster_contests.keys();
					while(roster_contest_ids.hasNext()){
						String c_id = (String) roster_contest_ids.next();
						String scoring_rules_string = roster_contests.getString(c_id);
						JSONObject scoring_rules = new JSONObject(scoring_rules_string);
						JSONArray player_scores = baseball_bot.updateScores(scoring_rules);

						JSONObject fields = new JSONObject();
						fields.put("contest_id", Integer.parseInt(c_id));
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
					pari_contests = db_connection.get_active_pari_mutuels("BASEBALLPROPS", "PARI-MUTUEL");
					Iterator<?> pari_contest_ids = pari_contests.keys();	
					while(pari_contest_ids.hasNext()){
						String c_id = (String) pari_contest_ids.next();
						
						JSONObject scoring_rules = new JSONObject(pari_contests.getJSONObject(c_id).getString("scoring_rules"));
						JSONObject prop_data = new JSONObject(pari_contests.getJSONObject(c_id).getString("prop_data"));
						JSONArray option_table = new JSONArray(pari_contests.getJSONObject(c_id).getString("option_table"));

						JSONObject pari_fields = baseball_bot.settlePariMutuel(Integer.parseInt(c_id), scoring_rules, prop_data, option_table);
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
