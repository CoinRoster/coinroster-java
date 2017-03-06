package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;
import com.coinroster.internal.UserMail;

public class SettleRosterContest extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public SettleRosterContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String contest_admin = session.user_id();
			
			int contest_id = input.getInt("contest_id");
			
			JSONArray player_scores = input.getJSONArray("player_scores");
			
			// lock it all
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, contest write, entry write, transaction write");

			try {
				lock : {

					JSONObject contest = db.select_contest(contest_id);
					String contest_title = contest.getString("title");
					
					if (contest == null)
						{
						output.put("error", "Invalid contest id: " + contest_id);
						break lock;
						}
					
					if (!contest.getString("contest_type").equals("ROSTER"))
						{
						output.put("error", "Contest " + contest_id + " is not a roster contest");
						break lock;
						}
					
					if (contest.getInt("status") != 2)
						{
						output.put("error", "Contest " + contest_id + " is not in play");
						break lock;
						}
					
					// make a map of player scores 
					
					Map<Integer, Integer> score_map = new TreeMap<Integer, Integer>();
					Map<Integer, String> raw_score_map = new TreeMap<Integer, String>();
					
					for (int i=0, limit=player_scores.length(); i<limit; i++)
						{
						JSONObject player = player_scores.getJSONObject(i);
						
						int player_id = player.getInt("id");
						
						score_map.put(player_id, player.getInt("score"));
						raw_score_map.put(player_id, player.getString("score_raw"));
						}
					
					// loop through system players to make sure all players have been assigned a score
					// add player scores to JSONObject to be written back into contest
					
					JSONArray system_players = new JSONArray(contest.getString("option_table"));

					for (int i=0, limit=system_players.length(); i<limit; i++)
						{
						JSONObject player = system_players.getJSONObject(i);
						
						int player_id = player.getInt("id");
						
						if (!score_map.containsKey(player_id))
							{
							output.put("error", "No score provided for " + player.getString("name"));
							break lock;
							}
						
						player.put("score", score_map.get(player_id));
						player.put("score_raw", raw_score_map.get(player_id));
						
						system_players.put(player_id, player);
						}
					
					// if we get here, user has provided complete player scores for a roster contest that is in play

					// we'll need to access the liability and contest_asset accounts in multiple places:
					
					JSONObject liability_account = db.select_user("username", "internal_liability");
					String liability_account_id = liability_account.getString("user_id");
					
					JSONObject contest_account = db.select_user("username", "internal_contest_asset");
					String contest_account_id = contest_account.getString("user_id");
					
					// checksum of entries against transactions
					
					PreparedStatement get_entry_total = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ?");
					get_entry_total.setInt(1, contest_id);
					ResultSet entry_total_rs = get_entry_total.executeQuery();
					entry_total_rs.next();
					
					PreparedStatement get_transaction_totals = sql_connection.prepareStatement("select sum(case when trans_type = 'BTC-CONTEST-ENTRY' then amount end), sum(case when trans_type = 'RC-CONTEST-ENTRY' then amount end) from transaction where contest_id = ?");
					get_transaction_totals.setInt(1, contest_id);
					ResultSet totals_rs = get_transaction_totals.executeQuery();
					totals_rs.next();
					
					double 
					
					btc_wagers_total = totals_rs.getDouble(1),
					rc_wagers_total = totals_rs.getDouble(2),

					total_from_entries = entry_total_rs.getDouble(1),
					total_from_transactions = add(btc_wagers_total, rc_wagers_total, 0),
					total_referrer_payout = 0;
					
					// check roster amounts against transactions

					if (total_from_entries != total_from_transactions)
						{
						output.put("error", "Total based on entries does not match total based on transactions - this is serious!");
						break lock;
						}

					// pull all funds from the contest
					
					double 
					
					btc_contest_balance = contest_account.getDouble("btc_balance"),
					rc_contest_balance = contest_account.getDouble("rc_balance");
					
					btc_contest_balance = subtract(btc_contest_balance, btc_wagers_total, 0);
					rc_contest_balance = subtract(rc_contest_balance, rc_wagers_total, 0);
		
					PreparedStatement update_contest_account = sql_connection.prepareStatement("update user set btc_balance = ?, rc_balance = ? where id = ?");
					update_contest_account.setDouble(1, btc_contest_balance);
					update_contest_account.setDouble(2, rc_contest_balance);
					update_contest_account.setString(3, contest_account_id);
					update_contest_account.executeUpdate();
				
					// prepare rc swap - at this stage, convert all RC pulled from the contest into BTC
					
					double 
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					rc_liability_balance = liability_account.getDouble("rc_balance");
					
					btc_liability_balance = subtract(btc_liability_balance, rc_wagers_total, 0); // subtract increases liability
					rc_liability_balance = add(rc_liability_balance, rc_wagers_total, 0); // add decreases liability
					
					// get all rosters, score them, sort them into score buckets high to low, process referral revenue
					
					double
					
					rake = contest.getDouble("rake"),
					rake_amount = multiply(rake, total_from_entries, 0),
					user_winnings_total = subtract(total_from_entries, rake_amount, 0),
					actual_rake_amount = total_from_entries,
					
					cost_per_entry = contest.getDouble("cost_per_entry");

					log("Pool: " + total_from_entries);
					log("Rake: " + rake);
					log("Rake amount: " + rake_amount);
					log("User winnings: " + user_winnings_total);
					
					String settlement_type = contest.getString("settlement_type");
					
					TreeMap<Double, List<Integer>> roster_rankings = new TreeMap<Double, List<Integer>>(Collections.reverseOrder());
					Map<Integer, JSONObject> roster_map = new TreeMap<Integer, JSONObject>();
					TreeMap<String, Double> referrer_map = new TreeMap<String, Double>();
					
					JSONArray rosters = db.select_contest_entries(contest_id);
					
					for (int i=0, limit=rosters.length(); i<limit; i++)
						{
						// get roster details ----------------------------------------------
						
						JSONObject roster = rosters.getJSONObject(i);
						
						int roster_id = roster.getInt("entry_id");
						roster_map.put(roster_id, roster);
						
						String user_id = roster.getString("user_id");
						double amount = roster.getDouble("amount");
						JSONArray entry_data = new JSONArray(roster.getString("entry_data"));
						int number_of_rosters = (int) divide(amount, cost_per_entry, 0);

						log("");
						log("Roster ID: " + roster_id);
						log("User ID: " + user_id);
						log("Amount: " + amount);
						log("Qty: " + number_of_rosters);
						
						// score roster ----------------------------------------------------
						
						double roster_score = 0;
						
						for (int j=0; j<entry_data.length(); j++)
							{
							JSONObject player = entry_data.getJSONObject(j);
							int player_score = score_map.get(player.getInt("id"));
							roster_score = add(roster_score, player_score, 0);
							}
						
						List<Integer> rosters_at_score = null;
						
						if (roster_rankings.containsKey(roster_score)) rosters_at_score = roster_rankings.get(roster_score);
						else rosters_at_score = new ArrayList<Integer>();
						
						rosters_at_score.add(roster_id);
						roster_rankings.put(roster_score, rosters_at_score);
						
						log("Score: " + (int) roster_score);
						
						// handle the rake -------------------------------------------------
						
						JSONObject user = db.select_user("id", user_id);

						double user_raked_amount = multiply(amount, rake, 0);
						double user_btc_balance = user.getDouble("btc_balance");

						log("Raked amount: " + user_raked_amount);
						
						int free_play = user.getInt("free_play");
						
						if (free_play == 1)
							{
							log("Free play credit: " + user_raked_amount);
							
							user_btc_balance = add(user_btc_balance, user_raked_amount, 0);
							
							actual_rake_amount = subtract(actual_rake_amount, user_raked_amount, 0);
							
							db.update_btc_balance(user_id, user_btc_balance);
							
							String 
							
							transaction_type = "BTC-RAKE-CREDIT",
							from_account = contest_account_id,
							to_account = user_id,
							from_currency = "BTC",
							to_currency = "BTC",
							memo = "Rake credit (BTC) from contest #" + contest_id;
							
							PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
							create_transaction.setLong(1, System.currentTimeMillis());
							create_transaction.setString(2, contest_admin);
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
						else // this has to be an else, otherwise we could end up crediting 1.5 x the rake
							{
							String referrer = user.getString("referrer");
							if (!referrer.equals("")) // process referral payout if applicable
								{
								double referrer_payout = 0;
								
								switch (user.getInt("referral_program"))
									{
									case 1 : // perpetual 50% of rake
										{
										double affiliate_multiple = 0.5;
										referrer_payout = multiply(user_raked_amount, affiliate_multiple, 0);
										break;
										}
									}
								
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
							}
						}
					
					
					/* code snippet to iterate over rosters in descending order of score

					for (Map.Entry<Double, List<Integer>> entry : roster_rankings.entrySet()) 
						{
						log(entry.getKey());
						List<Integer> rosters_at_score = entry.getValue();
						
						for (int i=0, limit = rosters_at_score.size(); i<limit; i++)
							{
							int roster_id = rosters_at_score.get(i);
							JSONObject roster = roster_map.get(roster_id);
							}
						}
						
					*/
					
					switch (settlement_type)
						{
						case "HEADS-UP" :
							{
							// heads up:
								
							log("");
							log("Settling heads up");
								
							int roster_count_at_score = 0;

							Map<String, Integer> winner_map = new TreeMap<String, Integer>();
							
							List<Integer> winning_rosters = roster_rankings.get(roster_rankings.firstKey());
							
							for (int i=0; i<winning_rosters.size(); i++)
								{
								int roster_id = winning_rosters.get(i);
								JSONObject roster = roster_map.get(roster_id);
								String user_id = roster.getString("user_id");
								
								int win_count = 0;
								
								if (winner_map.containsKey(user_id)) win_count = winner_map.get(user_id);
								
								win_count++;
								roster_count_at_score++;
								
								winner_map.put(user_id, win_count);
								}
							
							for (Map.Entry<String, Integer> entry : winner_map.entrySet()) 
								{
								String user_id = entry.getKey();							
								int win_count = entry.getValue();
								
								JSONObject user = db.select_user("id", user_id);
								
								double 
								
								user_btc_balance = user.getDouble("btc_balance"),
								
								win_multiple = divide(win_count, roster_count_at_score, 0),
								user_winnings = multiply(win_multiple, user_winnings_total, 0);
								
								user_btc_balance = add(user_btc_balance, user_winnings, 0);
								actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0); 
								
								boolean outright_win = win_multiple == 1.0 ? true : false; // false = some kind of tie

								log("");
								log("User: " + user_id);
								log("Win multiple: " + win_multiple);
								log("Winnings: " + user_winnings);
								
								db.update_btc_balance(user_id, user_btc_balance);
								
								String 
								
								transaction_type = "BTC-CONTEST-WINNINGS",
								from_account = contest_account_id,
								to_account = user_id,
								from_currency = "BTC",
								to_currency = "BTC",
								memo = "Winnings (BTC) from contest #" + contest_id;
								
								PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
								create_transaction.setLong(1, System.currentTimeMillis());
								create_transaction.setString(2, contest_admin);
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
								
								subject = "You " + (outright_win ? "had" : "tied") + " the highest-scoring roster in contest #" + contest_id, 
								message_body = "";
								
								message_body += "You " + (outright_win ? "had" : "tied") + " the highest-scoring roster in contest #" + contest_id + " - <b>" + contest_title + "</b>";
								message_body += "<br/>";
								message_body += "<br/>";
								message_body += "Payout: <b>" + format_btc(user_winnings) + " BTC</b>";
								message_body += "<br/>";
								message_body += "<br/>";
								message_body += "You may view your transactions <a href='" + Server.host + "/account/'>here</a>.";
								message_body += "<br/>";
								message_body += "<br/>";
								message_body += "Please do not reply to this email.";
								
								new UserMail(user, subject, message_body);
								}
							
							} break;
						case "DOUBLE-UP" :
							{
							
							} break;
						case "JACKPOT" :
							{
							
							} break;
						}

					log("");
					log("Actual rake amount: " + actual_rake_amount);

					// now we swap BTC to RC for referral payouts
					
					btc_liability_balance = add(btc_liability_balance, total_referrer_payout, 0); // add decreases liability
					rc_liability_balance = subtract(rc_liability_balance, total_referrer_payout, 0); // subtract increases liability
	
					PreparedStatement update_liability = sql_connection.prepareStatement("update user set btc_balance = ?, rc_balance = ? where id = ?");
					update_liability.setDouble(1, btc_liability_balance);
					update_liability.setDouble(2, rc_liability_balance);
					update_liability.setString(3, liability_account_id);
					update_liability.executeUpdate();

					// compare rc_liability_balance against original to determine net swap
					
					double starting_rc_liability_balance = liability_account.getDouble("rc_balance");
					
					if (rc_liability_balance != starting_rc_liability_balance) // net swap exists
						{
						String
						
						swap_trans_type = null,
						swap_from_currency = null,
						swap_to_currency = null,
						swap_memo = "Net swap for contest #" + contest_id;
						
						double swap_amount = 0;
						
						if (rc_liability_balance < starting_rc_liability_balance) // rc liability has increased
							{
							swap_trans_type = "BTC-SWAP-TO-RC";
							swap_from_currency = "BTC";
							swap_to_currency = "RC";
							
							swap_amount = subtract(starting_rc_liability_balance, rc_liability_balance, 0);
							}
						else if (rc_liability_balance > starting_rc_liability_balance) // rc liability has decreased
							{
							swap_trans_type = "RC-SWAP-TO-BTC";
							swap_from_currency = "RC";
							swap_to_currency = "BTC";
							
							swap_amount = subtract(rc_liability_balance, starting_rc_liability_balance, 0);
							}
	
						PreparedStatement swap = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						swap.setLong(1, System.currentTimeMillis());
						swap.setString(2, contest_admin);
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
		
					// we now have our liability account ajusted to fund referrer payouts
				
					for (Map.Entry<String, Double> entry : referrer_map.entrySet()) 
						{
						String user_id = entry.getKey();
						double referrer_payout = entry.getValue();
						
						JSONObject user = db.select_user("id", user_id);
		
						double user_rc_balance = user.getDouble("rc_balance");
						
						user_rc_balance = add(user_rc_balance, referrer_payout, 0);
		
						db.update_rc_balance(user_id, user_rc_balance);
		
						String 
						
						transaction_type = "RC-REFERRAL-REVENUE",
						from_account = liability_account_id,
						to_account = user_id,
						from_currency = "RC",
						to_currency = "RC",
						memo = "Referral revenue (RC) from contest #" + contest_id;

						PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						create_transaction.setLong(1, System.currentTimeMillis());
						create_transaction.setString(2, contest_admin);
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
						
						subject = "Referral revenue for contest #" + contest_id, 
						message_body = "";
						
						message_body += "You earned <b>" + format_btc(referrer_payout) + " RC</b> in referral revenue from contest #" + contest_id + " - <b>" + contest_title + "</b>";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "You may view your transactions <a href='" + Server.host + "/account/'>here</a>.";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "Please do not reply to this email.";
						
						new UserMail(user, subject, message_body);
						}
				
					// move everything that hasn't been paid out as winnings or referral revenue to internal_asset:
					
					JSONObject internal_asset = db.select_user("username", "internal_asset");
					String internal_asset_id = internal_asset.getString("user_id");
					double internal_asset_btc_balance = internal_asset.getDouble("btc_balance");
					internal_asset_btc_balance = add(internal_asset_btc_balance, actual_rake_amount, 0);
					db.update_btc_balance(internal_asset_id, internal_asset_btc_balance);
					
					String 
					
					transaction_type = "BTC-RAKE",
					from_account = contest_account_id,
					to_account = internal_asset_id,
					from_currency = "BTC",
					to_currency = "BTC",
					memo = "Rake (BTC) from contest #" + contest_id;

					PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
					create_transaction.setLong(1, System.currentTimeMillis());
					create_transaction.setString(2, contest_admin);
					create_transaction.setString(3, transaction_type);
					create_transaction.setString(4, from_account);
					create_transaction.setString(5, to_account);
					create_transaction.setDouble(6, actual_rake_amount);
					create_transaction.setString(7, from_currency);
					create_transaction.setString(8, to_currency);
					create_transaction.setString(9, memo);
					create_transaction.setInt(10, contest_id);
					create_transaction.executeUpdate();

					// update contest to settled, add updated option table (has an outcome flag on each option):

					PreparedStatement update_contest = sql_connection.prepareStatement("update contest set status = 3, settled_by = ?, option_table = ? where id = ?");
					update_contest.setString(1, contest_admin);
					update_contest.setString(2, system_players.toString());
					update_contest.setInt(3, contest_id);
					update_contest.executeUpdate();
		
					output.put("status", "1");
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

			new BuildLobby(sql_connection);
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}