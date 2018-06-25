package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

public class UserSettleContest extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public UserSettleContest(MethodInstance method) throws Exception 
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
			statement.execute("lock tables user write, contest write, entry write, transaction write, progressive write");

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
						output.put("error", error);
						
						break lock;
						}
					
					if (contest.getInt("status") != 2)
						{
						String error = "Contest " + contest_id + " is not in play";
						
						log(error);
						output.put("error", error);
						
						break lock;
						}

					log("");
					
					//--------------------------------------------------------------------------------------------------------------
					
					// Users can make only PARI-MUTUEL contests
					
					int winning_outcome = 0;
					
					//--------------------------------------------------------------------------------------------------------------
				
					// validate settlement data provided by contest admin
					
					log("Validating settlement data");

					JSONArray option_table = new JSONArray(contest.getString("option_table"));

					boolean valid_option = false;

					winning_outcome = input.getInt("winning_outcome");
					
					for (int i=0; i < option_table.length(); i++)
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
					
					//--------------------------------------------------------------------------------------------------------------
					
					// below is the main winnings processing section

					//--------------------------------------------------------------------------------------------------------------
					

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
							}
						}
							
						//--------------------------------------------------------------------------------------------------------------

					
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

						update_contest = sql_connection.prepareStatement("update contest set status = 3, settled_by = ?, option_table = ?, settled = ?, progressive_paid = ? where id = ?");
						update_contest.setString(1, contest_admin);
						update_contest.setString(2, option_table.toString());
						update_contest.setLong(3, System.currentTimeMillis());
						update_contest.setDouble(4, progressive_paid);
						update_contest.setInt(5, contest_id);

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