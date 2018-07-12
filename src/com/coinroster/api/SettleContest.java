	package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import com.coinroster.internal.Backup;
import com.coinroster.internal.BuildLobby;
import com.coinroster.internal.UserMail;

public class SettleContest extends Utils
	{
	public static String method_level = "score_bot";
	@SuppressWarnings("unused")
	public SettleContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			boolean do_update = true;
			
			int contest_id = input.getInt("contest_id");
			String contest_admin;
			if(session==null)
				contest_admin = "ContestBot";
			else
				contest_admin = session.user_id();
			
			new Backup();

			// lock it all
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, contest write, entry write, transaction write, progressive write, voting right");

			try {
				lock : {
				
					// initial contest validation

					JSONObject contest = db.select_contest(contest_id);
					
					String 
					
					contest_title = contest.getString("title"),
					contest_type = contest.getString("contest_type");
					
					boolean voting_contest = db.is_voting_contest(contest_id);

					log("Validating contest #" + contest_id);
					log("Type: " + contest_type);
					log("Voting contest: " + voting_contest);
					
					if (contest == null)
						{
						String error = "Invalid contest id: " + contest_id;
						
						log(error);
						output.put("error", error);
						
						break lock;
						}
					
					if (contest.getInt("status") != 2 && !voting_contest)
						{
						String error = "Contest " + contest_id + " is not in play";
						
						log(error);
						output.put("error", error);
						
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
							boolean valid_option = false;

							winning_outcome = input.getInt("winning_outcome");
							
							for (int i=0; i<option_table.length(); i++)
								{
								JSONObject option = option_table.getJSONObject(i);
								
								int option_id = option.getInt("id");
								
								if (winning_outcome == option_id) 
									{
									option.put("outcome", 1);
									valid_option = true;
									}
								else option.put("outcome", 0);
								
								option_table.put(i, option);
								}
							
							if (!valid_option)
								{
								String error = "Invalid winning option id: " + winning_outcome;
								
								log(error);
								output.put("error", error);
								
								break lock;
								}
								
							} break;
							
						case "ROSTER" :
							{
							// populate map of player scores 
								
							player_scores = input.getJSONArray("player_scores");
							
							for (int i=0, limit=player_scores.length(); i<limit; i++)
								{
								JSONObject player = player_scores.getJSONObject(i);
								
								int player_id = player.getInt("id");
								double score_normalized = player.getDouble("score_normalized");
								String score_raw = player.getString("score_raw");
								
								score_map.put(player_id, score_normalized);
								raw_score_map.put(player_id, score_raw);
								}
								
							// loop through system players to make sure all players have been assigned a score
							// add player scores to JSONObject to be written back into contest
								
							for (int i=0, limit=option_table.length(); i<limit; i++)
								{
								JSONObject player = option_table.getJSONObject(i);
								
								int player_id = player.getInt("id");
								
								if (!score_map.containsKey(player_id))
								{
									String error = "No score provided for " + player.getString("name");
									log(error);
									
									// pga tour withdrawal
									player.put("score", 0);
									player.put("score_raw", "WD");
									
									option_table.put(i, player);
									
								}
								else{
						
									player.put("score", score_map.get(player_id));
									player.put("score_raw", raw_score_map.get(player_id));
									
									option_table.put(i, player);
									}
								}
							}
							break;
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
						output.put("error", error);
						
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

					double 
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					rc_liability_balance = liability_account.getDouble("rc_balance"),
					opening_rc_liability_balance = rc_liability_balance; // used later to determine net swap
					
					if(!voting_contest) {
							
						log("Swapping all RC to BTC");
						
						btc_liability_balance = subtract(btc_liability_balance, rc_wagers_total, 0); // subtract increases liability
						rc_liability_balance = add(rc_liability_balance, rc_wagers_total, 0); // add decreases liability
	
						log("");
					} else {
						// if voting round, all payout is done using RC
						log("VOTING ROUND: Swapping all BTC to RC");
						
						btc_liability_balance = add(btc_liability_balance, btc_wagers_total, 0); // add decreases liability
						rc_liability_balance = subtract(rc_liability_balance, btc_wagers_total, 0); // subtract increases liability
	
						log("");
					}
					
					//--------------------------------------------------------------------------------------------------------------

					// calculate some general figures for the pool

					double
					
					rake = contest.getDouble("rake"), // e.g. 0.01 if rake is 1%
					rake_amount = multiply(rake, total_from_transactions, 0),
					actual_rake_amount = total_from_transactions, // gets decremented every time something is paid out; remainder -> internal_asset
					progressive_paid = 0,							
					user_winnings_total = subtract(total_from_transactions, rake_amount, 0),
					contest_creator_commission = 0;
					
					JSONObject internal_asset = db.select_user("username", "internal_asset");
					
					if(voting_contest && do_update) {
						
						// voting contest creator gets a 1% commission from the original contest's betting volume
						
						Integer original_contest = db.get_original_contest(contest_id);
						
						PreparedStatement get_original_total = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ?");
						get_entry_total.setInt(1, original_contest);
						ResultSet original_total_rs = get_entry_total.executeQuery();
						entry_total_rs.next();
						contest_creator_commission = multiply(db.get_voting_contest_commission(), entry_total_rs.getDouble(1), 0);
						log("Contest creator commission: " + contest_creator_commission);
						
						// account id that created the original crowd-settled contest
						String to_account =  db.select_contest(original_contest).getString("created_by");
						String from_account = internal_asset.getString("id");
						
						PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						create_transaction.setLong(1, System.currentTimeMillis());
						create_transaction.setString(2, contest_admin);
						create_transaction.setString(3, "RC-VOTING-CONTEST-COMMISSION");
						create_transaction.setString(4, from_account);
						create_transaction.setString(5, to_account);
						create_transaction.setDouble(6, contest_creator_commission);
						create_transaction.setString(7, "RC");
						create_transaction.setString(8, "RC");
						create_transaction.setString(9, "Voting round creator commission");
						create_transaction.setInt(10, contest_id);
						create_transaction.executeUpdate();
						
						// update balances by swapping asset BTC balance to user's RC balance
						Double internal_asset_btc_balance = internal_asset.getDouble("btc_balance");
						Double user_rc_balance = db.select_user("id", to_account).getDouble("rc_balance");
						
						internal_asset_btc_balance = subtract(internal_asset_btc_balance, contest_creator_commission, 0);
						user_rc_balance = add(user_rc_balance, contest_creator_commission, 0);
						
						db.update_btc_balance(from_account, internal_asset_btc_balance);
						db.update_rc_balance(to_account, user_rc_balance);
					}
					
					log("Pool: " + total_from_transactions);
					log("Rake: " + rake);
					log("Rake amount: " + rake_amount);
					log("User winnings: " + user_winnings_total);
					log("");

					//--------------------------------------------------------------------------------------------------------------

					JSONArray entries = db.select_contest_entries(contest_id);
					
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

					
					// calculate referral rewards / process rake credits 
					
					/* IF NOT VOTING */

					if (!voting_contest)
						{
						log("Calculating referral rewards...");

						TreeMap<String, Double> referrer_map = new TreeMap<String, Double>();
						
						double total_referrer_payout = 0;
						
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
						}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					
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
									create_transaction.setString(2, contest_admin);
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
									create_transaction.setString(2, contest_admin);
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
									double user_wager = entry_rs.getDouble(2);
									
									if (!voting_contest) {
										// payout in btc if not a voting round

										double 
										
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
									} else {
										
										double 
										
										user_rc_balance = user.getDouble("rc_balance"),
										user_winnings = multiply(user_wager, payout_ratio, 0);
										
										log("");
										log("User: " + user_id);
										log("Wager: " + user_wager);
										log("Winnings: " + user_winnings);
										
										// implicit conversion from BTC to RC
										user_rc_balance = add(user_rc_balance, user_winnings, 0);
										
										actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0);
										
										if (do_update)
											{
											db.update_rc_balance(user_id, user_rc_balance);
						
											String 
											
											transaction_type = "RC-VOTING-CONTEST-WINNINGS",
											from_account = contest_account_id,
											to_account = user_id,
											from_currency = "BTC",
											to_currency = "RC",
											memo = "Winnings (RC) from: " + contest_title;
											
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
											
											subject = "You picked the correct outcome in: " + contest_title, 
											message_body = "";
											
											message_body += "You picked the correct outcome in: <b>" + contest_title + "</b>";
											message_body += "<br/>";
											message_body += "<br/>";
											message_body += "Payout: <b>" + format_btc(user_winnings) + " Roster Coins</b>";
											message_body += "<br/>";
											message_body += "<br/>";
											message_body += "<a href='" + Server.host + "/contests/entries.html?contest_id=" + contest_id + "'>Click here</a> for detailed results.";
											
											new UserMail(user, subject, message_body);
											}
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
									double player_score = 0;
									try{
										player_score = score_map.get(player_id);
									}
									catch(NullPointerException e){
										log("player " + player_id + " not found; setting score = 0");
										player_score = 0;
									}
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
								update_contest.setString(1, contest_admin);
								update_contest.setString(2, option_table.toString());
								update_contest.setLong(3, System.currentTimeMillis());
								update_contest.setDouble(4, progressive_paid);
								update_contest.setInt(5, contest_id);
								break;
								
							case "ROSTER" :
								
								String normalization_scheme = input.getString("normalization_scheme");
								
								update_contest = sql_connection.prepareStatement("update contest set status = 3, settled_by = ?, option_table = ?, settled = ?, progressive_paid = ?, scores_updated = ?, scoring_scheme = ? where id = ?");
								update_contest.setString(1, contest_admin);
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
		
					if (do_update) output.put("status", "1"); 
					else output.put("error", "SettleContest is in test mode - updates have been turned off");
					}
				}
			catch (Exception e)
				{
				output.put("error", e.getMessage());
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