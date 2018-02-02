package com.coinroster.bots;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;
import com.coinroster.api.JsonReader;
import com.coinroster.internal.Backup;
import com.coinroster.internal.BuildLobby;
import com.coinroster.internal.UserMail;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BasketballBot extends Utils {
	
	public static String method_level = "admin";

	// instance variables
	protected ArrayList<String> game_IDs;
	private Map<Integer, Player> players_list;
	private long earliest_game;
	private String sport = "BASKETBALL";

	private static Connection sql_connection = null;
	
	// constructor

	public BasketballBot(Connection sql_connection) throws IOException, JSONException{
		BasketballBot.sql_connection = sql_connection;
	}
	// methods
	public ArrayList<String> getGameIDs(){
		return game_IDs;
	}
	
	public long getEarliestGame(){
		return earliest_game;
	}
	public void scrapeGameIDs() throws IOException, JSONException{
		ArrayList<String> gameIDs = new ArrayList<String>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYYMMdd");
		String today = LocalDate.now().format(formatter);
		JSONObject json = JsonReader.readJsonFromUrl("http://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?lang=en&region=us&calendartype=blacklist&limit=100&dates=" + today + "&tz=America%2FNew_York");
		JSONArray events = json.getJSONArray("events");
		if(events.length() == 0){
			System.out.println("No games");
			this.game_IDs = null;
		}
		else{
			String earliest_date = events.getJSONObject(0).getString("date");
	        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
	        try {
	            Date date = formatter1.parse(earliest_date.replaceAll("Z$", "+0000"));
	            System.out.println(date);
	            long milli = date.getTime();
	            this.earliest_game = milli;
	        } 
	        catch (ParseException e) {
	            e.printStackTrace();
	        }

			for(int i=0; i < events.length(); i++){
				
				JSONArray links = events.getJSONObject(i).getJSONArray("links");
				String href = links.getJSONObject(0).getString("href");
				String gameID = href.split("=")[1].replace("&sourceLang", "");
				gameIDs.add(gameID.toString());
				this.game_IDs = gameIDs;
			}
		}
	}
	
	public Map<Integer, Player> getPlayerHashMap(){
		return players_list;
	}
	
	public static ResultSet getAllPlayerIDs(){
		ResultSet result_set = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id from player where sport_type=?");
			get_players.setString(1, "BASKETBALL");
			result_set = get_players.executeQuery();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return result_set;
	}
	
	public ArrayList<String> getAllGameIDsDB() throws SQLException{
		ResultSet result_set = null;
		ArrayList<String> gameIDs = new ArrayList<String>();
		try {
			PreparedStatement get_games = sql_connection.prepareStatement("select distinct gameID from player where sport_type=?");
			get_games.setString(1, "BASKETBALL");
			result_set = get_games.executeQuery();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		while(result_set.next()){
			gameIDs.add(result_set.getString(1));	
		}
		return gameIDs;
	}
	
	public void savePlayers(){
		try {
			PreparedStatement delete_old_rows = sql_connection.prepareStatement("delete from player where sport_type=?");
			delete_old_rows.setString(1, this.sport);
			delete_old_rows.executeUpdate();
			System.out.println("deleted " + this.sport + " players from old contests");

			for(Player player : this.getPlayerHashMap().values()){
				
				PreparedStatement save_player = sql_connection.prepareStatement("insert into player(id, name, sport_type, gameID, team_abr, salary, points, bioJSON) values(?, ?, ?, ?, ?, ?, ?, ?)");				
				save_player.setInt(1, player.getESPN_ID());
				save_player.setString(2, player.getName());
				save_player.setString(3, sport);
				save_player.setString(4, player.getGameID());
				save_player.setString(5, player.getTeam());
				save_player.setDouble(6, player.getSalary());
				save_player.setDouble(7, player.getPoints());
				save_player.setString(8, player.getBio().toString());
				save_player.executeUpdate();	
			}
			
			System.out.println("added " + sport + " players to DB");
		}
		catch (Exception e) {
			Server.exception(e);
		}
	}
	
	public static int createContest(String category, String contest_type, String progressive_code, 
			String title, String desc, double rake, double cost_per_entry, String settlement_type, 
			int salary_cap, int min_users, int max_users, int entries_per_user, int roster_size, 
			String odds_source, String score_header, double[] payouts, ResultSet playerIDs, Long deadline) throws Exception{

		DB db = new DB(sql_connection);

		if (title.length() > 255)
		{
			log("Title is too long");
			return 1;
		}

		if (deadline - System.currentTimeMillis() < 1 * 60 * 60 * 1000)
		{
			log( "Registration deadline must be at least 1 hour from now");
			return 1;
		}

		if (rake < 0 || rake >= 100)
		{
			log("Rake cannot be < 0 or > 100");
			return 1;
		}

		rake = divide(rake, 100, 0); // convert to %

		if (cost_per_entry == 0)
		{
			log("Cost per entry cannot be 0");
			return 1;
		}

		if (progressive_code.equals("")) progressive_code = null; // default value
		else
		{
			JSONObject progressive = db.select_progressive(progressive_code);

			if (progressive == null)
			{
				log("Invalid Progressive");
				return 1;
			}

			if (!progressive.getString("category").equals(category) || !progressive.getString("sub_category").equals("BASKETBALL"))
			{
				log("Progressive belongs to a different category");
				return 1;
			}
		}

		log("Contest parameters:");

		log("category: " + category);
		log("sub_category: BASKETBALL");
		log("progressive: " + progressive_code);
		log("contest_type: " + contest_type);
		log("title: " + title);
		log("description: " + desc);
		log("registration_deadline: " + deadline);
		log("rake: " + rake);
		log("cost_per_entry: " + cost_per_entry);

		if(contest_type.equals("ROSTER")){
			if (min_users < 2)
			{
				log( "Invalid value for [min users]");
				return 1;
			}
			if (max_users < min_users && max_users != 0) // 0 = unlimited
			{
				log( "Invalid value for [max users]");
				return 1;
			}

			if (roster_size < 0)
			{
				log("Roster size cannobe be negative");
				return 1;
			}

			if (entries_per_user < 0)
			{
				log("Invalid value for [entries per user]");
				return 1;
			}

			if (score_header.equals(""))
			{
				log("Please choose a score column header");
				return 1;
			}

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
							
			switch (settlement_type)
			{
			case "HEADS-UP":

				if (min_users != 2 || max_users != 2)
				{
					log("Invalid value(s) for number of users");
					return 1;
				}
				break;

			case "DOUBLE-UP": break;

			case "JACKPOT": break;

			default:

				log("Invalid value for [settlement type]");
				return 1;

			}
			JSONArray pay_table = new JSONArray();
			for(int i=0; i < payouts.length; i++){
				JSONObject line = new JSONObject();
				line.put("payout", payouts[i]);
				line.put("rank", i+1);
				pay_table.put(line);
			}
			
			try{
				
				PreparedStatement create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, description, registration_deadline, rake, cost_per_entry, settlement_type, min_users, max_users, entries_per_user, pay_table, salary_cap, option_table, created, created_by, roster_size, odds_source, score_header) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				create_contest.setString(1, category);
				create_contest.setString(2, "BASKETBALL");
				create_contest.setString(3, progressive_code);
				create_contest.setString(4, contest_type);
				create_contest.setString(5, title);
				create_contest.setString(6, desc);
				create_contest.setLong(7, deadline);
				create_contest.setDouble(8, rake);
				create_contest.setDouble(9, cost_per_entry);
				create_contest.setString(10, settlement_type);
				create_contest.setInt(11, min_users);
				create_contest.setInt(12, max_users);
				create_contest.setInt(13, entries_per_user);
				create_contest.setString(14, pay_table.toString());
				create_contest.setDouble(15, salary_cap);
				create_contest.setString(16, option_table.toString());	
				create_contest.setLong(17, System.currentTimeMillis());
				create_contest.setString(18, "ColeFisher");
				create_contest.setInt(19, roster_size);
				create_contest.setString(20, odds_source);
				log(odds_source);
				create_contest.setString(21, score_header);
				create_contest.executeUpdate();
				log("added contest to db");
				new BuildLobby(sql_connection);
			}
			catch(Exception e){
				e.printStackTrace();
			}
        }
	return 0;
	}
	
	public void updateScores(int contest_id, Connection sql_connection) throws SQLException{
		
		Statement statement = sql_connection.createStatement();
		statement.execute("lock tables contest write, player read");

		try {
			lock : {
		
				// initial contest validation
				DB db = new DB(sql_connection);
				JSONObject contest = db.select_contest(contest_id);
	
				if (contest == null)
					{
					String error = "Invalid contest id: " + contest_id;
					
					log(error);					
					break lock;
					}
				
				if (contest.getInt("status") != 2)
					{
					String error = "Contest " + contest_id + " is not in play";
					log(error);					
					break lock;
					}

				if (!contest.getString("contest_type").equals("ROSTER"))
					{
					String error = "Contest " + contest_id + " is not a roster contest";			
					log(error);					
					break lock;
					}
				
				// ROSTER	
				JSONArray option_table = new JSONArray(contest.getString("option_table"));		
				for (int i=0, limit=option_table.length(); i<limit; i++)
					{
					JSONObject player = option_table.getJSONObject(i);
					
					int player_id = player.getInt("id");
					double fantasy_points = db.get_fantasy_points(player_id, sport);
			
					player.put("score", fantasy_points);
					player.put("score_raw", fantasy_points);
					
					option_table.put(i, player);
					}
				
				PreparedStatement update_contest = sql_connection.prepareStatement("update contest set option_table = ?, scores_updated = ?, scoring_scheme = ? where id = ?");
				update_contest.setString(1, option_table.toString());
				update_contest.setLong(2, System.currentTimeMillis());
				update_contest.setString(3, "INTEGER");
				update_contest.setInt(4, contest_id);
				update_contest.executeUpdate();
				
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		finally
			{
			statement.execute("unlock tables");
			}
	}
	
	public void settleContest(int contest_id, Connection sql_connection) throws SQLException{
		log("made it into settleContest() method");
		boolean do_update = true;		
		DB db = new DB(sql_connection);
		new Backup();

		// lock it all
		Statement statement = sql_connection.createStatement();
		statement.execute("lock tables user write, contest write, entry write, transaction write, progressive write, player read");

		try {
			lock : {
			
				// initial contest validation

				JSONObject contest = db.select_contest(contest_id);
				
				String 
				
				contest_title = contest.getString("title"),
				contest_type = contest.getString("contest_type");

				log("Validating contest #" + contest_id);
				log("Type: " + contest_type);
				
				if (contest == null)
					{
					String error = "Invalid contest id: " + contest_id;
					log(error);
					break lock;
					}
				
				if (contest.getInt("status") != 2)
					{
					String error = "Contest " + contest_id + " is not in play";
					log(error);
					break lock;
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------
			
				// variables & storage required for various settlement schemes
				
				// PARI-MUTUEL
				
				int winning_outcome = 0;
				
				// ROSTER
				
				JSONArray player_scores;
				
				Map<Integer, Double> score_map = new TreeMap<Integer, Double>();
				Map<Integer, String> raw_score_map = new TreeMap<Integer, String>();
				
				//--------------------------------------------------------------------------------------------------------------
			
				// validate settlement data provided by contest admin
				
				log("Validating settlement data");

				JSONArray option_table = new JSONArray(contest.getString("option_table"));
				
				switch (contest_type)
					{
					case "PARI-MUTUEL" :
						{
//						boolean valid_option = false;
//
//						winning_outcome = input.getInt("winning_outcome");
//						
//						for (int i=0; i<option_table.length(); i++)
//							{
//							JSONObject option = option_table.getJSONObject(i);
//							
//							int option_id = option.getInt("id");
//							
//							if (winning_outcome == option_id) 
//								{
//								option.put("outcome", 1);
//								valid_option = true;
//								}
//							else option.put("outcome", 0);
//							
//							option_table.put(i, option);
//							}
//						
//						if (!valid_option)
//							{
//							String error = "Invalid winning option id: " + winning_outcome;
//							
//							log(error);
//							output.put("error", error);
//							
//							break lock;
//							}
							
						} break;
						
					case "ROSTER" :
						{
						// populate map of player scores 
							
//						player_scores = input.getJSONArray("player_scores");
//						
//						for (int i=0, limit=player_scores.length(); i<limit; i++)
//							{
//							JSONObject player = player_scores.getJSONObject(i);
//							
//							int player_id = player.getInt("id");
//							double score_normalized = player.getDouble("score_normalized");
//							String score_raw = player.getString("score_raw");
//							
//							score_map.put(player_id, score_normalized);
//							raw_score_map.put(player_id, score_raw);
//							}
							
						// loop through system players to make sure all players have been assigned a score
						// add player scores to JSONObject to be written back into contest
							
						for (int i=0, limit=option_table.length(); i<limit; i++)
							{
							JSONObject player = option_table.getJSONObject(i);
							
							int player_id = player.getInt("id");
							
//							if (!score_map.containsKey(player_id))
//								{
//								String error = "No score provided for " + player.getString("name");
//								
//								log(error);
//								output.put("error", error);
//								
//								break lock;
//								}
							double score = db.get_fantasy_points(player_id, sport);
							player.put("score", score);
							player.put("score_raw", score);
							
							option_table.put(i, player);
							}
						} break;
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------
			
				// if we get here, user has provided valid settlement data for a contest that is in play
				
				//--------------------------------------------------------------------------------------------------------------
			
				// checksum entries against transactions

				log("Validating prize pool ...");
				
				PreparedStatement get_transaction_totals = sql_connection.prepareStatement("select sum(case when trans_type = 'BTC-CONTEST-ENTRY' then amount end), sum(case when trans_type = 'RC-CONTEST-ENTRY' then amount end) from transaction where contest_id = ?");
				get_transaction_totals.setInt(1, contest_id);
				ResultSet transaction_totals_rs = get_transaction_totals.executeQuery();
				transaction_totals_rs.next();
				
				PreparedStatement get_entry_total = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ?");
				get_entry_total.setInt(1, contest_id);
				ResultSet entry_total_rs = get_entry_total.executeQuery();
				entry_total_rs.next();
				
				double 
				
				btc_wagers_total = transaction_totals_rs.getDouble(1),
				rc_wagers_total = transaction_totals_rs.getDouble(2),

				total_from_transactions = add(btc_wagers_total, rc_wagers_total, 0),
				total_from_entries = entry_total_rs.getDouble(1);

				log("Total from transactions: " + total_from_transactions);
				log("Total from entries: " + total_from_entries);

				if (total_from_transactions != total_from_entries)
					{
					String error = "Total based on transactions does not match total based on entries - this is serious!";
					
					log(error);
					break lock;
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------

				// pull all funds from the contest
				
				log("Pulling funds from contest");

				JSONObject liability_account = db.select_user("username", "internal_liability");
				String liability_account_id = liability_account.getString("user_id");
				
				JSONObject contest_account = db.select_user("username", "internal_contest_asset");
				String contest_account_id = contest_account.getString("user_id");

				double 
				
				btc_contest_balance = contest_account.getDouble("btc_balance"),
				rc_contest_balance = contest_account.getDouble("rc_balance");
				
				btc_contest_balance = subtract(btc_contest_balance, btc_wagers_total, 0);
				rc_contest_balance = subtract(rc_contest_balance, rc_wagers_total, 0);
				
				if (do_update)
					{
					db.update_btc_balance(contest_account_id, btc_contest_balance);
					db.update_rc_balance(contest_account_id, rc_contest_balance);
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------

				// prepare rc swap
				
				// our gross liability does not change at all during contest settlement, but the RC-to-BTC distribution changes
				// at this stage, we convert all RC pulled from the contest into BTC
				// later, we will convert the sum of all referrer payouts from BTC to RC

				log("Swapping all RC to BTC");

				double 
				
				btc_liability_balance = liability_account.getDouble("btc_balance"),
				rc_liability_balance = liability_account.getDouble("rc_balance"),
				opening_rc_liability_balance = rc_liability_balance; // used later to determine net swap
				
				btc_liability_balance = subtract(btc_liability_balance, rc_wagers_total, 0); // subtract increases liability
				rc_liability_balance = add(rc_liability_balance, rc_wagers_total, 0); // add decreases liability

				log("");
				
				//--------------------------------------------------------------------------------------------------------------

				// calculate some general figures for the pool

				double
				
				rake = contest.getDouble("rake"), // e.g. 0.01 if rake is 1%
				rake_amount = multiply(rake, total_from_transactions, 0),
				actual_rake_amount = total_from_transactions, // gets decremented every time something is paid out; remainder -> internal_asset
				progressive_paid = 0,							
				user_winnings_total = subtract(total_from_transactions, rake_amount, 0);
				
				log("Pool: " + total_from_transactions);
				log("Rake: " + rake);
				log("Rake amount: " + rake_amount);
				log("User winnings: " + user_winnings_total);
				log("");

				//--------------------------------------------------------------------------------------------------------------

				// calculate referral rewards / process rake credits

				log("Calculating referral rewards...");

				TreeMap<String, Double> referrer_map = new TreeMap<String, Double>();
				
				double total_referrer_payout = 0;
				
				JSONArray entries = db.select_contest_entries(contest_id);
				
				for (int i=0, limit=entries.length(); i<limit; i++)
					{
					JSONObject entry = entries.getJSONObject(i);
					
					int entry_id = entry.getInt("entry_id");
					String user_id = entry.getString("user_id");

					JSONObject user = db.select_user("id", user_id);
					
					int 
					
					free_play = user.getInt("free_play"),
					withdrawal_locked = user.getInt("withdrawal_locked");

					double 
					
					user_btc_balance = user.getDouble("btc_balance"),
					entry_amount = entry.getDouble("amount"),
					user_raked_amount = multiply(entry_amount, rake, 0);

					log("");
					log("Entry ID: " + entry_id);
					log("User ID: " + user_id);
					log("Amount: " + entry_amount);
					log("Raked amount: " + user_raked_amount);
					
					if (free_play == 1)
						{
						log("Free play credit: " + user_raked_amount);
						
						user_btc_balance = add(user_btc_balance, user_raked_amount, 0);
						
						actual_rake_amount = subtract(actual_rake_amount, user_raked_amount, 0);
						
						if (do_update) 
							{
							db.update_btc_balance(user_id, user_btc_balance);
							
							String 
							
							transaction_type = "BTC-RAKE-CREDIT",
							from_account = contest_account_id,
							to_account = user_id,
							from_currency = "BTC",
							to_currency = "BTC",
							memo = "Rake credit (BTC) from: " + contest_title;
							
							PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
							create_transaction.setLong(1, System.currentTimeMillis());
							create_transaction.setString(2, "COLEFISHER");
							create_transaction.setString(3, transaction_type);
							create_transaction.setString(4, from_account);
							create_transaction.setString(5, to_account);
							create_transaction.setDouble(6, user_raked_amount);
							create_transaction.setString(7, from_currency);
							create_transaction.setString(8, to_currency);
							create_transaction.setString(9, memo);
							create_transaction.setInt(10, contest_id);
							create_transaction.executeUpdate();
							}
						}
					else if (withdrawal_locked == 0) // affiliates only earn on accounts that have met playing requirement
						{
						String referrer = user.getString("referrer");
						
						if (referrer.equals("")) continue;
						
						// process referral payout if applicable
						
						double 
						
						referral_program = user.getDouble("referral_program"),
						referrer_payout = multiply(user_raked_amount, referral_program, 0);

						actual_rake_amount = subtract(actual_rake_amount, referrer_payout, 0);
						total_referrer_payout = add(total_referrer_payout, referrer_payout, 0);
						
						log("Referral payout for " + referrer + " : " + referrer_payout);
						
						if (referrer_map.containsKey(referrer))
							{
							referrer_payout = add(referrer_payout, referrer_map.get(referrer), 0);
							referrer_map.put(referrer, referrer_payout);
							}
						else referrer_map.put(referrer, referrer_payout);
						}
					else if (withdrawal_locked == 1) db.credit_user_rollover_progress(user, entry_amount);
					}
				
				log("");

				//--------------------------------------------------------------------------------------------------------------

				// we previously converted all RC to BTC; now we create RC to fund affiliate payouts
				
				log("Processing net swap ...");
				
				btc_liability_balance = add(btc_liability_balance, total_referrer_payout, 0); // add decreases liability
				rc_liability_balance = subtract(rc_liability_balance, total_referrer_payout, 0); // subtract increases liability
				
				// at this point, we write out the updated liability balances
				
				if (do_update)
					{
					db.update_btc_balance(liability_account_id, btc_liability_balance);
					db.update_rc_balance(liability_account_id, rc_liability_balance);
					}

				// compare final rc_liability_balance against opening balance to determine net swap
				
				log("Opening RC liability balance: " + opening_rc_liability_balance);
				log("New RC liability balance: " + rc_liability_balance);
				
				if (rc_liability_balance == opening_rc_liability_balance) log("No net swap");
				else // net swap exists
					{
					String
					
					swap_trans_type = null,
					swap_from_currency = null,
					swap_to_currency = null,
					swap_memo = "Net swap for contest #" + contest_id;
					
					double swap_amount = 0;
					
					if (rc_liability_balance < opening_rc_liability_balance) // rc liability has increased; net creation of RC
						{
						swap_trans_type = "BTC-SWAP-TO-RC";
						swap_from_currency = "BTC";
						swap_to_currency = "RC";
						
						swap_amount = subtract(opening_rc_liability_balance, rc_liability_balance, 0);
						}
					else if (rc_liability_balance > opening_rc_liability_balance) // rc liability has decreased; net destruction of RC
						{
						swap_trans_type = "RC-SWAP-TO-BTC";
						swap_from_currency = "RC";
						swap_to_currency = "BTC";
						
						swap_amount = subtract(rc_liability_balance, opening_rc_liability_balance, 0);
						}
					
					log("Swap: " + swap_trans_type);
					
					if (do_update)
						{			
						PreparedStatement swap = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						swap.setLong(1, System.currentTimeMillis());
						swap.setString(2, "COLEFISHER");
						swap.setString(3, swap_trans_type);
						swap.setString(4, liability_account_id);
						swap.setString(5, liability_account_id);
						swap.setDouble(6, swap_amount);
						swap.setString(7, swap_from_currency);
						swap.setString(8, swap_to_currency);
						swap.setString(9, swap_memo);
						swap.setInt(10, contest_id);
						swap.executeUpdate();
						}
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------

				// process referral rewards

				log("Processing referral rewards");
			
				for (Map.Entry<String, Double> entry : referrer_map.entrySet()) 
					{
					String user_id = entry.getKey();
					double referrer_payout = entry.getValue();
					
					JSONObject user = db.select_user("id", user_id);
	
					double user_rc_balance = user.getDouble("rc_balance");
					
					user_rc_balance = add(user_rc_balance, referrer_payout, 0);
	
					if (do_update)
						{
						db.update_rc_balance(user_id, user_rc_balance);
	
						String 
						
						transaction_type = "RC-REFERRAL-REVENUE",
						from_account = liability_account_id,
						to_account = user_id,
						from_currency = "RC",
						to_currency = "RC",
						memo = "Referral revenue (RC) from: " + contest_title;
						
						PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						create_transaction.setLong(1, System.currentTimeMillis());
						create_transaction.setString(2, "COLEFISHER");
						create_transaction.setString(3, transaction_type);
						create_transaction.setString(4, from_account);
						create_transaction.setString(5, to_account);
						create_transaction.setDouble(6, referrer_payout);
						create_transaction.setString(7, from_currency);
						create_transaction.setString(8, to_currency);
						create_transaction.setString(9, memo);
						create_transaction.setInt(10, contest_id);
						create_transaction.executeUpdate();
						
						String
						
						subject = "Referral revenue from: " + contest_title, 
						message_body = "";
						
						message_body += "You earned <b>" + format_btc(referrer_payout) + " RC</b> in referral revenue from: <b>" + contest_title + "</b>";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "<a href='" + Server.host + "/account/transactions.html'>Click here</a> to view your transactions.";
						
						new UserMail(user, subject, message_body);
						}
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------
				
				// below is the main winnings processing section

				//--------------------------------------------------------------------------------------------------------------
				
				switch (contest_type)
					{
				
				 	//--------------------------------------------------------------------------------------------------------------
				
					case "PARI-MUTUEL" :
						{
						log("Processing winnings ...");
							
						PreparedStatement get_amounts = sql_connection.prepareStatement("select sum(if(entry_data = ?, amount, 0)) from entry where contest_id = ?");
						get_amounts.setInt(1, winning_outcome);
						get_amounts.setInt(2, contest_id);
						ResultSet amount_rs = get_amounts.executeQuery();
						
						amount_rs.next();
						
						double winning_outcome_total = amount_rs.getDouble(1);
						
						if (winning_outcome_total == 0) // nobody picked the winning outcome
							{
							if (!contest.isNull("progressive"))
								{
								String progressive_code = contest.getString("progressive");
								
								JSONObject 

								progressive = db.select_progressive(progressive_code),
								internal_progressive = db.select_user("username", "internal_progressive");
								
								String internal_progressive_id = internal_progressive.getString("user_id");
								
								double 
								
								progressive_balance = progressive.getDouble("balance"),
								internal_progressive_balance = internal_progressive.getDouble("btc_balance");

								progressive_balance = add(progressive_balance, user_winnings_total, 0);
								internal_progressive_balance = add(internal_progressive_balance, user_winnings_total, 0);
								actual_rake_amount = subtract(actual_rake_amount, user_winnings_total, 0);
								
								db.update_btc_balance(internal_progressive_id, internal_progressive_balance);
								
								String 
								
								transaction_type = "BTC-PROGRESSIVE-HOLD",
								from_account = contest_account_id,
								to_account = internal_progressive_id,
								from_currency = "BTC",
								to_currency = "BTC",
								memo = "Progressive hold for contest #" + contest_id;
								
								PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
								create_transaction.setLong(1, System.currentTimeMillis());
								create_transaction.setString(2, "COLEFISHER");
								create_transaction.setString(3, transaction_type);
								create_transaction.setString(4, from_account);
								create_transaction.setString(5, to_account);
								create_transaction.setDouble(6, user_winnings_total);
								create_transaction.setString(7, from_currency);
								create_transaction.setString(8, to_currency);
								create_transaction.setString(9, memo);
								create_transaction.setInt(10, contest_id);
								create_transaction.executeUpdate();
								
								PreparedStatement update_progressive = sql_connection.prepareStatement("update progressive set balance = ? where code = ?");
								update_progressive.setDouble(1, progressive_balance);
								update_progressive.setString(2, progressive_code);
								update_progressive.executeUpdate();
								}
							}
						else // at least one wager was placed on the winning outcome
							{
							if (!contest.isNull("progressive"))
								{
								String progressive_code = contest.getString("progressive");
								
								JSONObject 

								progressive = db.select_progressive(progressive_code),
								internal_progressive = db.select_user("username", "internal_progressive");
								
								String internal_progressive_id = internal_progressive.getString("user_id");
								
								double 
								
								progressive_balance = progressive.getDouble("balance"),
								internal_progressive_balance = internal_progressive.getDouble("btc_balance");

								internal_progressive_balance = subtract(internal_progressive_balance, progressive_balance, 0);
								user_winnings_total = add(user_winnings_total, progressive_balance, 0);
								actual_rake_amount = add(actual_rake_amount, progressive_balance, 0); // will get decremented back down
								progressive_paid = progressive_balance;
								progressive_balance = 0;
								
								db.update_btc_balance(internal_progressive_id, internal_progressive_balance);
								
								String 
								
								transaction_type = "BTC-PROGRESSIVE-DISBURSEMENT",
								from_account = internal_progressive_id,
								to_account = contest_account_id,
								from_currency = "BTC",
								to_currency = "BTC",
								memo = "Progressive disbursement for contest #" + contest_id;
								
								PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
								create_transaction.setLong(1, System.currentTimeMillis());
								create_transaction.setString(2, "COLEFISHER");
								create_transaction.setString(3, transaction_type);
								create_transaction.setString(4, from_account);
								create_transaction.setString(5, to_account);
								create_transaction.setDouble(6, user_winnings_total);
								create_transaction.setString(7, from_currency);
								create_transaction.setString(8, to_currency);
								create_transaction.setString(9, memo);
								create_transaction.setInt(10, contest_id);
								create_transaction.executeUpdate();
								
								PreparedStatement update_progressive = sql_connection.prepareStatement("update progressive set balance = 0 where code = ?");
								update_progressive.setString(1, progressive_code);
								update_progressive.executeUpdate();
								}

							double payout_ratio = divide(user_winnings_total, winning_outcome_total, 0);
							
							log("Winning selection: " + winning_outcome);
							log("Total wagered on winning outcome: " + winning_outcome_total);
							log("Payout ratio: " + payout_ratio);

							// select all winning wagers summed by user_id
							
							PreparedStatement select_entries = sql_connection.prepareStatement("select user_id, sum(amount) from entry where contest_id = ? and entry_data = ? group by user_id");
							select_entries.setInt(1, contest_id);
							select_entries.setInt(2, winning_outcome);
							ResultSet entry_rs = select_entries.executeQuery();
							
							while (entry_rs.next())
								{
								String user_id = entry_rs.getString(1);
								
								JSONObject user = db.select_user("id", user_id);
								
								double 
								
								user_wager = entry_rs.getDouble(2),
								user_btc_balance = user.getDouble("btc_balance"),
								user_winnings = multiply(user_wager, payout_ratio, 0);
								
								log("");
								log("User: " + user_id);
								log("Wager: " + user_wager);
								log("Winnings: " + user_winnings);
								
								user_btc_balance = add(user_btc_balance, user_winnings, 0);
								
								actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0);
								
								if (do_update)
									{
									db.update_btc_balance(user_id, user_btc_balance);
				
									String 
									
									transaction_type = "BTC-CONTEST-WINNINGS",
									from_account = contest_account_id,
									to_account = user_id,
									from_currency = "BTC",
									to_currency = "BTC",
									memo = "Winnings (BTC) from: " + contest_title;
									
									PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
									create_transaction.setLong(1, System.currentTimeMillis());
									create_transaction.setString(2, "COLEFISHER");
									create_transaction.setString(3, transaction_type);
									create_transaction.setString(4, from_account);
									create_transaction.setString(5, to_account);
									create_transaction.setDouble(6, user_winnings);
									create_transaction.setString(7, from_currency);
									create_transaction.setString(8, to_currency);
									create_transaction.setString(9, memo);
									create_transaction.setInt(10, contest_id);
									create_transaction.executeUpdate();
									
									String
									
									subject = "You picked the correct outcome in: " + contest_title, 
									message_body = "";
									
									message_body += "You picked the correct outcome in: <b>" + contest_title + "</b>";
									message_body += "<br/>";
									message_body += "<br/>";
									message_body += "Payout: <b>" + format_btc(user_winnings) + " BTC</b>";
									message_body += "<br/>";
									message_body += "<br/>";
									message_body += "<a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>Click here</a> for detailed results.";
									
									new UserMail(user, subject, message_body);
									}
								}
							}
						} break;
						
					//--------------------------------------------------------------------------------------------------------------
						
					case "ROSTER" :
						{
						// for roster contests, first we build up "score buckets" in descending order of score (roster_rankings)
						// each bucket contains a list of pointers to all rosters at that score
							
						log("Scoring rosters ...");
							
						TreeMap<Double, List<Integer>> roster_rankings = new TreeMap<Double, List<Integer>>(Collections.reverseOrder());

						double cost_per_entry = contest.getDouble("cost_per_entry");
						
						int number_of_rosters = 0;
						
						HashSet<Integer> rosters_with_payout_updated = new HashSet<Integer>(); // keep track of multi-entry rosters
													
						for (int roster_pointer=0; roster_pointer<entries.length(); roster_pointer++)
							{								
							JSONObject roster = entries.getJSONObject(roster_pointer);
							
							log("");
							log("Entry[" + roster_pointer + "] with ID: " + roster.getInt("entry_id"));
							log("User: " + roster.getString("user_id"));
							
							double roster_score = 0;
							
							JSONArray entry_data = new JSONArray(roster.getString("entry_data"));
							
							for (int i=0; i<entry_data.length(); i++)
								{
								JSONObject player = entry_data.getJSONObject(i);
								int player_id = player.getInt("id");
								double player_score = db.get_fantasy_points(player_id, sport);
								roster_score = add(roster_score, player_score, 0);
								}
							
							log("Score: " + (int) roster_score);

							db.update_roster_score(roster.getInt("entry_id"), roster_score);
							
							// score keys rosters_at_score from roster_rankings

							List<Integer> rosters_at_score = null;
							
							if (roster_rankings.containsKey(roster_score)) rosters_at_score = roster_rankings.get(roster_score);
							else rosters_at_score = new ArrayList<Integer>();

							// we need to add the roster as many times as it was entered
							
							int number_of_entries = (int) divide(roster.getDouble("amount"), cost_per_entry, 0);
							number_of_rosters += number_of_entries;

							log("Qty: " + number_of_entries);

							for (int i=0; i<number_of_entries; i++) rosters_at_score.add(roster_pointer);
	
							roster_rankings.put(roster_score, rosters_at_score);
							}
						
						log("");
						
						// next, we pay out winnings based on settlement_type
						
						switch (contest.getString("settlement_type"))
							{
							case "HEADS-UP" :
								{
								// to settle heads up, we grab the highest-scoring roster bucket
								// if only one user is represented in the bucket, they take all the winnings
								// if both users have rosters in the bucket, the winnings are split proportionately
									
								log("Settling heads up...");
								
								// accumulator for total number of rosters in bucket (for proportional split)
									
								int roster_count_at_score = 0;
								
								// winner_map will either have one or two keys corresponding to the one or two user_id's
								// each user_id will key the number of rosters that the user has in the bucket

								Map<String, Double> payout_map = new TreeMap<String, Double>();
								
								// get all roster pointers in hightest-scoring bucket:
								
								List<Integer> winning_rosters = roster_rankings.get(roster_rankings.firstKey());
								
								int number_of_winning_rosters = winning_rosters.size();
								
								double payout_per_roster = divide(user_winnings_total, number_of_winning_rosters, 0);
								
								// loop through rosters, increment counts in winner_map
								
								log("Payout per roster: " + payout_per_roster);
								
								for (int i=0; i<winning_rosters.size(); i++)
									{
									int 
									
									roster_pointer = winning_rosters.get(i);
									JSONObject roster = entries.getJSONObject(roster_pointer);
									
									int roster_id = roster.getInt("entry_id");
									String user_id = roster.getString("user_id");
									
									if (!rosters_with_payout_updated.contains(roster_id))
										{
										double 
										
										roster_amount = roster.getDouble("amount"),
										roster_entries = divide(roster_amount, cost_per_entry, 0),
										roster_payout = multiply(roster_entries, payout_per_roster, 0);
										
										log("Payout for roster #" + roster_id + " : " +roster_payout);
										
										if (do_update) db.update_roster_payout(roster_id, roster_payout);
										
										rosters_with_payout_updated.add(roster_id);
										}
									
									int win_count = 0;
									
									double user_payout = payout_per_roster;
									
									if (payout_map.containsKey(user_id)) user_payout = add(payout_map.get(user_id), user_payout, 0);
									
									payout_map.put(user_id, user_payout);

									log("Paying out " + payout_per_roster + " to roster #" + roster.getInt("entry_id") + " : " + user_id);
									}
								
								log("");
								log("Processing user winnings ...");
								
								boolean outright_win = payout_map.size() == 1 ? true : false;
								
								for (Map.Entry<String, Double> entry : payout_map.entrySet()) 
									{
									String user_id = entry.getKey();
									double user_winnings = entry.getValue();
									
									JSONObject user = db.select_user("id", user_id);
									
									double user_btc_balance = user.getDouble("btc_balance");
									
									user_btc_balance = add(user_btc_balance, user_winnings, 0);
									actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0); 
									
									log("");
									log("User: " + user_id);
									log("Winnings: " + user_winnings);
									
									if (do_update)
										{
										db.update_btc_balance(user_id, user_btc_balance);
										
										String 
										
										transaction_type = "BTC-CONTEST-WINNINGS",
										from_account = contest_account_id,
										to_account = user_id,
										from_currency = "BTC",
										to_currency = "BTC",
										memo = "Winnings (BTC) from: " + contest_title;
										
										PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
										create_transaction.setLong(1, System.currentTimeMillis());
										create_transaction.setString(2, "COLEFISHER");
										create_transaction.setString(3, transaction_type);
										create_transaction.setString(4, from_account);
										create_transaction.setString(5, to_account);
										create_transaction.setDouble(6, user_winnings);
										create_transaction.setString(7, from_currency);
										create_transaction.setString(8, to_currency);
										create_transaction.setString(9, memo);
										create_transaction.setInt(10, contest_id);
										create_transaction.executeUpdate();
										
										String
										
										subject = "You " + (outright_win ? "had" : "tied") + " the highest-scoring roster in: " + contest_title, 
										message_body = "";
										
										message_body += "You " + (outright_win ? "had" : "tied") + " the highest-scoring roster in: <b>" + contest_title + "</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										message_body += "Payout: <b>" + format_btc(user_winnings) + " BTC</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										message_body += "<a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>Click here</a> for detailed results.";
										
										new UserMail(user, subject, message_body);
										}
									}
								} break;
								
							case "DOUBLE-UP" :
								{
								// to settle a double up, we loop through roster buckets in descending order of score
								// rosters are paid out exactly 2x the cost per entry unless we hit a bucket where some 
							    // rosters are below the payline; in that case, the remaining payout is split pro-rata
								// once the payline has been hit, we break
									
								log("Settling double up...");
								log("");
								
								double 
								
								inverse_of_rake = subtract(1, rake, 0), // e.g. 0.05 rake -> 0.95 inverse_of_rake
								temp_number_of_rosters = multiply(inverse_of_rake, number_of_rosters, 0),
								payout_per_roster = multiply(cost_per_entry, 2, 0);
								
								int 
								
								pay_line = (int) divide(temp_number_of_rosters, 2, 0),
								number_of_rosters_paid_out = 0;
								
								double total_payable_amount = multiply(pay_line, payout_per_roster, 0); // decremented for each roster payout

								log("Inverse of rake: " + inverse_of_rake);
								log("Total number of rosters: " + number_of_rosters);
								log("Temp number of rosters: " + temp_number_of_rosters);
								log("Pay line: " + pay_line);
								log("Payout per roster: " + payout_per_roster);
								log("Total payable amount: " + total_payable_amount);
								
								Map<String, Double> payout_map = new TreeMap<String, Double>();
								HashSet<String> users_in_straddle = new HashSet<String>(); // keep track of users who got a partial payout, for communications
								
								for (Map.Entry<Double, List<Integer>> entry : roster_rankings.entrySet()) 
									{
									List<Integer> rosters_at_score = entry.getValue();
									int number_of_rosters_at_score = rosters_at_score.size();
									
									log("");
									log("Remaining payout: " + total_payable_amount);
									log("Rosters with a score of " + entry.getKey() + ": " + number_of_rosters_at_score);
									
									boolean partial_payout = false;
									
									if (number_of_rosters_paid_out + number_of_rosters_at_score > pay_line) // bucket straddles payline
										{
										log("Payline is straddled");
										partial_payout = true;
										payout_per_roster = divide(total_payable_amount, number_of_rosters_at_score, 0);
										}

									for (int i=0; i<number_of_rosters_at_score; i++)
										{
										int 
										
										roster_pointer = rosters_at_score.get(i);
										JSONObject roster = entries.getJSONObject(roster_pointer);
										
										int roster_id = roster.getInt("entry_id");
										String user_id = roster.getString("user_id");
										
										if (!rosters_with_payout_updated.contains(roster_id))
											{
											double 
											
											roster_amount = roster.getDouble("amount"),
											roster_entries = divide(roster_amount, cost_per_entry, 0),
											roster_payout = multiply(roster_entries, payout_per_roster, 0);
											
											log("Payout for roster #" + roster_id + " : " +roster_payout);
											
											if (do_update) db.update_roster_payout(roster_id, roster_payout);
											
											rosters_with_payout_updated.add(roster_id);
											}
										
										total_payable_amount = subtract(total_payable_amount, payout_per_roster, 0);
										
										double user_payout = payout_per_roster;
										
										if (payout_map.containsKey(user_id)) user_payout = add(payout_map.get(user_id), user_payout, 0);
										
										payout_map.put(user_id, user_payout);
										
										if (partial_payout && !users_in_straddle.contains(user_id)) users_in_straddle.add(user_id);
										
										// update entry -> store score and payout
										}
									
									number_of_rosters_paid_out += number_of_rosters_at_score; // integer, no need for BigDecimal
									
									if (number_of_rosters_paid_out >= pay_line) break;
									}
								
								log("");
								log("Processing user winnings ...");
								
								for (Map.Entry<String, Double> entry : payout_map.entrySet()) 
									{
									String user_id = entry.getKey();
									double user_winnings = entry.getValue();
									boolean user_in_straddle = users_in_straddle.contains(user_id) ? true : false;
									
									JSONObject user = db.select_user("id", user_id);
									
									double user_btc_balance = user.getDouble("btc_balance");
									
									user_btc_balance = add(user_btc_balance, user_winnings, 0);
									actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0); 
									
									log("");
									log("User: " + user_id);
									log("Winnings: " + user_winnings);
									
									if (do_update)
										{
										db.update_btc_balance(user_id, user_btc_balance);
										
										String 
										
										transaction_type = "BTC-CONTEST-WINNINGS",
										from_account = contest_account_id,
										to_account = user_id,
										from_currency = "BTC",
										to_currency = "BTC",
										memo = "Winnings (BTC) from: " + contest_title;
										
										PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
										create_transaction.setLong(1, System.currentTimeMillis());
										create_transaction.setString(2, "COLEFISHER");
										create_transaction.setString(3, transaction_type);
										create_transaction.setString(4, from_account);
										create_transaction.setString(5, to_account);
										create_transaction.setDouble(6, user_winnings);
										create_transaction.setString(7, from_currency);
										create_transaction.setString(8, to_currency);
										create_transaction.setString(9, memo);
										create_transaction.setInt(10, contest_id);
										create_transaction.executeUpdate();
										
										String
										
										subject = "You had rosters above the payline in: " + contest_title, 
										message_body = "";
										
										message_body += "You had rosters above the payline in: <b>" + contest_title + "</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										
										if (user_in_straddle)
											{
											message_body += "Some of your rosters were in a tie that straddled the payline. All rosters at that score split the remaining pot evenly. You will therefore see some partial double-ups.";
											message_body += "<br/>";
											message_body += "<br/>";
											}
										
										message_body += "Total payout: <b>" + format_btc(user_winnings) + " BTC</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										message_body += "<a href='" + Server.host + "/rosters.html?contest_id=" + contest_id + "'>Click here</a> to view the final Leaderboard.";
										
										new UserMail(user, subject, message_body);
										}
									}
								
								} break;
								
							case "JACKPOT" :
								{
								// jackpot settlement is similar to heads up - the only difference being that we have multiple payout ranks to process
								// if three rosters tie the highest score, the sum of the first three ranks is paid out pro rata to those three users 
							    // any remaining ranks are then handed out using the same principle to the next-lowest-scoring buckets

								log("Settling jackpot ...");
									
								JSONArray pay_table = new JSONArray(contest.getString("pay_table"));

								Map<String, Double> payout_map = new TreeMap<String, Double>();
								
								int 
								
								number_of_ranks = pay_table.length(),
								rank_counter = 0;
								
								for (Map.Entry<Double, List<Integer>> entry : roster_rankings.entrySet()) 
									{
									List<Integer> rosters_at_score = entry.getValue();
									int number_of_rosters_at_score = rosters_at_score.size();
									
									log("");
									log("Rosters with a score of " + entry.getKey() + ": " + number_of_rosters_at_score);
									
									double payout_at_score = 0;
									
									boolean exit_loop = false;
									
									for (int i=0; i<number_of_rosters_at_score; i++)
										{
										JSONObject rank = pay_table.getJSONObject(rank_counter); 
										
										double 
										
										payout_multiple = rank.getDouble("payout"),
										payout_at_rank = multiply(user_winnings_total, payout_multiple, 0);
										payout_at_score = add(payout_at_rank, payout_at_score, 0);
										
										rank_counter++;
										
										log("Rank " + rank_counter + " (" + payout_multiple + ") : " + payout_at_rank);
										
										if (rank_counter == number_of_ranks) 
											{
											// rank_counter keeps track of where in the pay_table we are as we pay out.
											// once all ranks have been used in this inner loop, there is nothing to pay out.
											// We do still have to pay out the current bucket, so we set a boolean and break 
											// at the end of the score bucket loop
											
											exit_loop = true;
											break;
											}
										}
									
									double payout_per_roster = divide(payout_at_score, number_of_rosters_at_score, 0);

									log("Payout at score: " + payout_at_score);
									log("Payout per roster: " + payout_per_roster);
									
									for (int i=0; i<number_of_rosters_at_score; i++)
										{
										int 
										
										roster_pointer = rosters_at_score.get(i);
										JSONObject roster = entries.getJSONObject(roster_pointer);
										
										int roster_id = roster.getInt("entry_id");
										String user_id = roster.getString("user_id");
										
										if (!rosters_with_payout_updated.contains(roster_id))
											{
											double 
											
											roster_amount = roster.getDouble("amount"),
											roster_entries = divide(roster_amount, cost_per_entry, 0),
											roster_payout = multiply(roster_entries, payout_per_roster, 0);
											
											log("Payout for roster #" + roster_id + " : " +roster_payout);
											
											if (do_update) db.update_roster_payout(roster_id, roster_payout);
											
											rosters_with_payout_updated.add(roster_id);
											}
										
										double user_payout = payout_per_roster;
										
										if (payout_map.containsKey(user_id)) user_payout = add(payout_map.get(user_id), user_payout, 0);
										
										payout_map.put(user_id, user_payout);
										
										log("Paying out " + payout_per_roster + " to roster #" + roster.getInt("entry_id") + " : " + user_id);
										}
									
									if (exit_loop) break;
									}

								log("");
								log("Processing user winnings ...");
								
								for (Map.Entry<String, Double> entry : payout_map.entrySet()) 
									{
									String user_id = entry.getKey();
									double user_winnings = entry.getValue();
									
									JSONObject user = db.select_user("id", user_id);
									
									double user_btc_balance = user.getDouble("btc_balance");
									
									user_btc_balance = add(user_btc_balance, user_winnings, 0);
									actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0); 
									
									log("");
									log("User: " + user_id);
									log("Winnings: " + user_winnings);
									
									if (do_update)
										{
										db.update_btc_balance(user_id, user_btc_balance);
										
										String 
										
										transaction_type = "BTC-CONTEST-WINNINGS",
										from_account = contest_account_id,
										to_account = user_id,
										from_currency = "BTC",
										to_currency = "BTC",
										memo = "Winnings (BTC) from: " + contest_title;
										
										PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
										create_transaction.setLong(1, System.currentTimeMillis());
										create_transaction.setString(2, "COLEFISHER");
										create_transaction.setString(3, transaction_type);
										create_transaction.setString(4, from_account);
										create_transaction.setString(5, to_account);
										create_transaction.setDouble(6, user_winnings);
										create_transaction.setString(7, from_currency);
										create_transaction.setString(8, to_currency);
										create_transaction.setString(9, memo);
										create_transaction.setInt(10, contest_id);
										create_transaction.executeUpdate();
										
										String
										
										subject = "You had ranking rosters in: " + contest_title, 
										message_body = "";
										
										message_body += "You had ranking rosters in: <b>" + contest_title + "</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										message_body += "Total payout: <b>" + format_btc(user_winnings) + " BTC</b>";
										message_body += "<br/>";
										message_body += "<br/>";
										message_body += "<a href='" + Server.host + "/rosters.html?contest_id=" + contest_id + "'>Click here</a> to view the final Leaderboard.";
										
										new UserMail(user, subject, message_body);
										}
									}
								
								} break;
							}
						} break;
					}
				
				//--------------------------------------------------------------------------------------------------------------
				
				// end of winnings processing
				
				log("");

				//--------------------------------------------------------------------------------------------------------------
				
				// any funds that have not been paid out as winnings or referral revenue are credited to internal_asset

				log("Crediting asset account: " + actual_rake_amount);
			
				JSONObject internal_asset = db.select_user("username", "internal_asset");
				
				String internal_asset_id = internal_asset.getString("user_id");
				
				double internal_asset_btc_balance = internal_asset.getDouble("btc_balance");
				internal_asset_btc_balance = add(internal_asset_btc_balance, actual_rake_amount, 0);
				
				if (do_update) db.update_btc_balance(internal_asset_id, internal_asset_btc_balance);
				
				String 
				
				transaction_type = "BTC-RAKE",
				from_account = contest_account_id,
				to_account = internal_asset_id,
				from_currency = "BTC",
				to_currency = "BTC",
				memo = "Rake (BTC) from contest #" + contest_id;
				
				if (do_update)
					{
					PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
					create_transaction.setLong(1, System.currentTimeMillis());
					create_transaction.setString(2, "COLEFISHER");
					create_transaction.setString(3, transaction_type);
					create_transaction.setString(4, from_account);
					create_transaction.setString(5, to_account);
					create_transaction.setDouble(6, actual_rake_amount);
					create_transaction.setString(7, from_currency);
					create_transaction.setString(8, to_currency);
					create_transaction.setString(9, memo);
					create_transaction.setInt(10, contest_id);
					create_transaction.executeUpdate();
					}

				log("");
				
				//--------------------------------------------------------------------------------------------------------------
				
				// update contest with settlement info
				
				log("Updating contest");
				
				if (do_update)
					{
					PreparedStatement update_contest = null;

					switch (contest_type)
						{
						case "PARI-MUTUEL" :
							
							update_contest = sql_connection.prepareStatement("update contest set status = 3, settled_by = ?, option_table = ?, settled = ?, progressive_paid = ? where id = ?");
							update_contest.setString(1, "COLEFISHER");
							update_contest.setString(2, option_table.toString());
							update_contest.setLong(3, System.currentTimeMillis());
							update_contest.setDouble(4, progressive_paid);
							update_contest.setInt(5, contest_id);
							break;
							
						case "ROSTER" :
							
							String normalization_scheme = "INTEGER";
							
							update_contest = sql_connection.prepareStatement("update contest set status = 3, settled_by = ?, option_table = ?, settled = ?, progressive_paid = ?, scores_updated = ?, scoring_scheme = ? where id = ?");
							update_contest.setString(1, "COLEFISHER");
							update_contest.setString(2, option_table.toString());
							update_contest.setLong(3, System.currentTimeMillis());
							update_contest.setDouble(4, progressive_paid);
							update_contest.setLong(5, System.currentTimeMillis());
							update_contest.setString(6, normalization_scheme);
							update_contest.setInt(7, contest_id);
							break;
						}

					update_contest.executeUpdate();
					}

				log("");
	
				if (do_update) log("Do update"); 
				else log("SettleContest is in test mode - updates have been turned off");
				}
			}
		catch (Exception e)
			{
			log( e.getMessage());
			Server.exception(e);
			}
		finally
			{
			statement.execute("unlock tables");
			}
		
		new BuildLobby(sql_connection);
	}
		
		
	public static void editPoints(double pts, Connection sql_connection, int id) throws SQLException{
		PreparedStatement update_points = sql_connection.prepareStatement("update player set points = ? where id = ?");
		update_points.setDouble(1, pts);
		update_points.setInt(2, id);
		update_points.executeUpdate();
//		this.fantasy_points = pts;

	}
	// setup() method creates contest by creating a hashmap of <ESPN_ID, Player> entries
	public Map<Integer, Player> setup() throws IOException, JSONException, SQLException{
		Map<Integer, Player> players = new HashMap<Integer,Player>();
		for(int i=0; i < this.game_IDs.size(); i++){
			// for each gameID, get the two teams playing
			Document page = Jsoup.connect("http://www.espn.com/nba/game?gameId="+this.game_IDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
				      .referrer("http://www.google.com").timeout(6000).get();
			Elements team_divs = page.getElementsByClass("team-info-wrapper");
			// for each team, go to their stats page and scrape ppg
			for(Element team : team_divs){
				String team_link = team.select("a").attr("href");
				String team_abr = team_link.split("/")[5];
				Document team_stats_page = Jsoup.connect("http://www.espn.com/nba/team/roster/_/name/" + team_abr).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
					      .referrer("http://www.google.com").timeout(6000).get();
				Element stats_table = team_stats_page.getElementsByClass("mod-table").first().getElementsByClass("mod-content").first();
				Elements rows = stats_table.getElementsByTag("tr");
				for (Element row : rows){
					if(row.className().contains("oddrow") || row.className().contains("evenrow")){
						Elements cols = row.getElementsByTag("td");
						String name = cols.get(1).select("a").text();
						String team_name = team_abr.toUpperCase();
						int ESPN_id = Integer.parseInt(cols.get(1).select("a").attr("href").split("/")[7]);
						// create a player object, save it to the hashmap
						Player p = new Player(ESPN_id, name, team_name);
						System.out.println(p.getName() + " - " + p.getESPN_ID());
						p.scrape_info();
						p.setPPG();
						p.gameID = this.game_IDs.get(i);
						p.set_ppg_salary(p.getPPG());
						p.createBio();		
						players.put(ESPN_id, p);
					}
				}
			}
		}
		this.players_list = players;
		return players;
	}
	
	// scrape() method gets points from ESPN boxscores. 
	// meant to run every minute or so. Updates the Contest's player hashmap
	public boolean scrape(ArrayList<String> gameIDs) throws IOException, SQLException{
		
		int games_ended = 0;
		
		for(int i=0; i < gameIDs.size(); i++){
			Document page = Jsoup.connect("http://www.espn.com/nba/boxscore?gameId="+gameIDs.get(i)).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
				      .referrer("http://www.google.com").timeout(6000).get();
			Elements tables = page.getElementsByClass("mod-data");
			for (Element table : tables){
				Elements rows = table.getElementsByTag("tr");
				try{
					for (Element row : rows){
						Elements spans = row.getElementsByClass("name").select("a").select("span");
						String espn_ID_url = row.getElementsByClass("name").select("a").attr("href");
						if(!espn_ID_url.isEmpty()){
							int espn_ID = Integer.parseInt(espn_ID_url.split("/")[7]);
							if(spans.size() == 2){
								String points_string = row.getElementsByClass("pts").text();
								double pts;
								// if player played, get their points
								if(!points_string.isEmpty() && !points_string.contains("--")){
									pts = Double.parseDouble(points_string);		
								}
								// if player did not play, set pts=0
								else{
									pts = 0.0;
								}
								// look up player in HashMap with ESPN_ID, update his points in DB
								editPoints(pts, sql_connection, espn_ID);
							}			
						}			
					}
				}
				catch (NullPointerException nullPointer){		
				}		
			}
			
            //check to see if contest is finished - if all games are deemed final
			String time_left = page.getElementsByClass("status-detail").first().text();
			if(time_left.equals("Final")){
				games_ended += 1;
			}
			if(games_ended == gameIDs.size()){
				return true;
			}
		}
		return false;
	}

	class Player {
		public String method_level = "admin";
		private String name;
		private String team_abr;
		private int ESPN_ID;
		private double fantasy_points = 0;
		private double ppg;
		private double salary;
		private String birthString;
		private String height;
		private String weight;
		private String pos;
		private JSONArray last_five_games;
		private JSONObject career_stats;
		private JSONObject year_stats;
		private JSONObject bio;
		private String gameID;
		
		// constructor
		public Player(int id, String n, String team){
			this.ESPN_ID = id;
			this.name = n;
			this.team_abr = team;
		}
		
		// methods
	
		public double getPoints(){
			return fantasy_points;
		}
		public int getESPN_ID(){
			return ESPN_ID;
		}
		public String getGameID(){
			return gameID;
		}
		public void setPPG() throws JSONException{
			try{
				String ppg = this.getYearStats().getString("PPG");
				this.ppg = Double.parseDouble(ppg);
			}
			catch(java.lang.NullPointerException e){
				this.ppg = 0.0;
			}
			catch(JSONException e){
				this.ppg = 0.0;
			}
		}
		public double getSalary(){
			return salary;
		}
		public double getPPG(){
			return ppg;
		}
		public String getName(){
			return name;
		}
		public String getTeam(){
			return team_abr;
		}
		public JSONArray getGameLogs(){
			return last_five_games;
		}
		public JSONObject getCareerStats(){
			if(career_stats == null){
				return null;
			}
			return career_stats;
		}
		public JSONObject getYearStats(){
			if(year_stats == null){
				return null;
			}
			return year_stats;
		}
		public String getBirthString(){
			return birthString;
		}
		public String getPosition(){
			return pos;
		}
		public String getHeight(){
			return height;
		}
		public String getWeight(){
			return weight;
		}
		public void set_ppg_salary(double pts){
			if(pts < 5.0){
				this.salary = 50.0;
			}
			else{
				this.salary = Math.round(ppg * 10.0);
			}
		}
		public void createBio() throws JSONException{
			JSONObject bio = new JSONObject();
			bio.put("birthString", this.getBirthString());
			bio.put("height", this.getHeight());
			bio.put("Weight", this.getWeight());
			bio.put("pos", this.getPosition());
			bio.put("last_five_games", this.getGameLogs());
			bio.put("career_stats", this.getCareerStats());
			bio.put("year_stats", this.getYearStats());

			this.bio = bio;		
		}
		public JSONObject getBio(){
			return this.bio;
		}
		
		public void scrape_info() throws IOException, JSONException{
			
			Document page = Jsoup.connect("http://www.espn.com/nba/player/_/id/"+Integer.toString(this.getESPN_ID())).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
				      .referrer("http://www.google.com").timeout(6000).get();
			Elements bio_divs = page.getElementsByClass("player-bio");
			
			// parse bio-bio div to get pos, weight, height, birthString
			for(Element bio : bio_divs){
				Elements general_info = bio.getElementsByClass("general-info");
				String[] info = general_info.first().text().split(" ");
				String pos = info[1];
				this.pos = pos;
				String height = info[2] + " " + info[3].replace(",", "");
				String weight = info[4] + " " + info[5];
				this.height = height;
				this.weight = weight;
				Elements player_metadata = bio.getElementsByClass("player-metadata");
				Element item = player_metadata.first();
				Elements lis = item.children();
				String born_string = lis.first().text().replace("Born", "");
				this.birthString = born_string;
			}
			String[] stats = {"STAT_TYPE","GP","MPG","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","RPG","APG","BLKPG","STLPG","PFPG","TOPG","PPG"};
			Elements tables = page.getElementsByClass("tablehead");
			Element stats_table = tables.get(1);
			Elements stats_rows = stats_table.getElementsByTag("tr");
			try{
				for(Element row : stats_rows){
					if(row.className().contains("oddrow") || row.className().contains("evenrow")){
						JSONObject stat = new JSONObject();
						Elements cols = row.getElementsByTag("td");
						int index = 0;
						for(Element data : cols){
							stat.put(stats[index], data.text());
							index = index + 1;
						}
						
						if(stat.get("STAT_TYPE").equals("Career")){
							this.career_stats = stat;
						}
						else{
							this.year_stats = stat;
						}
					}
				}
			}
			catch (java.lang.ArrayIndexOutOfBoundsException e){
			}
		
			String[] game_log_stats = {"DATE","OPP","SCORE","MIN","FGM-FGA","FG%","3PM-3PA","3P%","FTM-FTA","FT%","REB","AST","BLK","STL","PF","TO","PTS"};
			Element game_log_table;
			try{
				game_log_table = tables.get(2);
			}
			catch(java.lang.IndexOutOfBoundsException e){
				//player does not have a year and career stats table, so his game logs is second table on page, not third
				game_log_table = tables.get(1);
			}
			Elements rows = game_log_table.getElementsByTag("tr");
			JSONArray game_logs = new JSONArray();
			for(Element row : rows){
				if(row.className().contains("oddrow") || row.className().contains("evenrow")){
					if(row.children().size() < 2){
						// skip the extra row in the game log - usually exists when player has been traded.
						continue;
					}
					JSONObject game = new JSONObject();
					Elements cols = row.getElementsByTag("td");
					int index = 0;
					for(Element data : cols){
						game.put(game_log_stats[index], data.text());
						index = index + 1;
					}
					game_logs.put(game);
				}	
			}
			this.last_five_games = game_logs;
		}
	}
	
}
