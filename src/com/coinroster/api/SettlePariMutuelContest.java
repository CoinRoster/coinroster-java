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
import com.coinroster.internal.UserMail;

public class SettlePariMutuelContest extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public SettlePariMutuelContest(MethodInstance method) throws Exception 
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
			
			int 
			
			contest_id = input.getInt("contest_id"),
			winning_outcome = input.getInt("winning_outcome");
			
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
					
					if (!contest.getString("contest_type").equals("PARI-MUTUEL"))
						{
						output.put("error", "Contest " + contest_id + " is not a pari-mutuel contest");
						break lock;
						}
					
					if (contest.getInt("status") != 2)
						{
						output.put("error", "Contest " + contest_id + " is not in play");
						break lock;
						}
					
					JSONArray option_table = new JSONArray(contest.getString("option_table"));
					
					boolean valid_option = false;
					
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
						}
					
					if (!valid_option)
						{
						output.put("error", "Invalid winning option id: " + winning_outcome);
						break lock;
						}
					
					// if we get here, user has provided a valid option for a pari_mutuel contest that is in play
		
					// we'll need to access the liability and contest_asset accounts in multiple places:
					
					JSONObject liability_account = db.select_user("username", "internal_liability");
					String liability_account_id = liability_account.getString("user_id");
					
					JSONObject contest_account = db.select_user("username", "internal_contest_asset");
					String contest_account_id = contest_account.getString("user_id");
					
					// first we calculate basic settlement amounts
					
					PreparedStatement get_amounts = sql_connection.prepareStatement("select sum(amount), sum(if(entry_data = ?, amount, 0)) from entry where contest_id = ?");
					get_amounts.setInt(1, winning_outcome);
					get_amounts.setInt(2, contest_id);
					ResultSet amount_rs = get_amounts.executeQuery();
					
					amount_rs.next();
					
					double
					
					wagers_total = amount_rs.getDouble(1),
					winning_outcome_total = amount_rs.getDouble(2),
					
					rake = contest.getDouble("rake"),
					rake_amount = multiply(rake, wagers_total, 0),
					user_winnings_total = subtract(wagers_total, rake_amount, 0),
		
					actual_rake_amount = wagers_total; // gets decremented every time something is paid out
					
					double payout_ratio = divide(user_winnings_total, winning_outcome_total, 0);
					
					log("Payout ratio: " + payout_ratio);
					
					// get totals wagered in both BTC and RC from transactions:
					
					PreparedStatement get_totals = sql_connection.prepareStatement("select sum(case when trans_type = 'BTC-CONTEST-ENTRY' then amount end), sum(case when trans_type = 'RC-CONTEST-ENTRY' then amount end) from transaction where contest_id = ?");
					get_totals.setInt(1, contest_id);
					ResultSet totals_rs = get_totals.executeQuery();
					totals_rs.next();
					
					double 
					
					btc_wagers_total = totals_rs.getDouble(1),
					rc_wagers_total = totals_rs.getDouble(2),
					
					total_from_transactions = add(btc_wagers_total, rc_wagers_total, 0);
		
					log("Winning selection: " + winning_outcome);
					log("Total wagers based on transaction records: " + total_from_transactions);
					log("Total wagers based on entry records: " + wagers_total);
					log("Rake: " + rake);
					log("Rake amount: " + rake_amount);
					log("Total winnings: " + user_winnings_total);
					log("Total wagered on winning outcome: " + winning_outcome_total);
					
					if (wagers_total != total_from_transactions)
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
					
					// prepare rc swap
					
					double 
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					rc_liability_balance = liability_account.getDouble("rc_balance");
					
					btc_liability_balance = subtract(btc_liability_balance, rc_wagers_total, 0); // subtract increases liability
					rc_liability_balance = add(rc_liability_balance, rc_wagers_total, 0); // add decreases liability
					
					// some accumulators for referrer payouts
			
					TreeMap<String, Double> referrer_map = new TreeMap<String, Double>();
					double total_referrer_payout = 0;
					
					// process entries, credit winners, build up referrer_map
					
					PreparedStatement select_entries = sql_connection.prepareStatement("select user_id, sum(amount), entry_data from entry where contest_id = ? group by entry_data, user_id");
					select_entries.setInt(1, contest_id);
					ResultSet entry_rs = select_entries.executeQuery();
					
					while (entry_rs.next())
						{
						String user_id = entry_rs.getString(1);
						double user_wager = entry_rs.getDouble(2);
						int selection = entry_rs.getInt(3);
						double user_raked_amount = multiply(user_wager, rake, 0);
						
						JSONObject user = db.select_user("id", user_id);
						
						log("");
						log("User: " + user_id);
						log("Wager: " + user_wager);
						log("Selection: " + selection);
						log("Amount raked: " + user_raked_amount);
											
						if (selection == winning_outcome)
							{
							double user_winnings = multiply(user_wager, payout_ratio, 0);
							
							log("Winnings: " + user_winnings);
							
							double user_btc_balance = user.getDouble("btc_balance");
							user_btc_balance = add(user_btc_balance, user_winnings, 0);
							
							actual_rake_amount = subtract(actual_rake_amount, user_winnings, 0);
							
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
							
							subject = "You won " + user_winnings + " BTC in contest #" + contest_id, 
							message_body = "";
							
							message_body += "You won <b>" + user_winnings + " BTC</b> in contest #" + contest_id + " - <b>" + contest_title + "</b>";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "You may view your transactions <a href='" + Server.host + "/account/'>here</a>.";
							message_body += "<br/>";
							message_body += "<br/>";
							message_body += "Please do not reply to this email.";
							
							new UserMail(user, subject, message_body);
							}
						//else {in future, send email if user opts to receive emails when they lose}
			
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
						
						message_body += "You earned <b>" + referrer_payout + " RC</b> in referral revenue from contest #" + contest_id + " - <b>" + contest_title + "</b>";
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
					update_contest.setString(2, option_table.toString());
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
						
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}