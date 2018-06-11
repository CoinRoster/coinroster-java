package com.coinroster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

public class SSI extends Utils
	{
	protected SSI(HttpRequest request, HttpResponse response) throws Exception
		{
		Connection sql_connection = null;
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);
			
			String 
			
			response_data = null,
			root = request.header("root"), 				// e.g. /usr/share/nginx/html/coinroster.com
			ssi_directory = root + "/ssi-java/",  		// e.g. /usr/share/nginx/html/coinroster.com/ssi-java/
			target_url = request.target_url(), 			// e.g. /ssi-java/nav
			target_object = request.target_object(); 	// e.g. nav
	
			String session_token = request.cookie("session_token");
			Session session = new Session(session_token);
	
			boolean session_active = session.active();
	
			if (request.header("promo") != null)
				{
				String promo_code = target_object.trim();
				
				JSONObject promo = db.select_promo(promo_code);
				
				if (promo == null) 
					{
					log("!!!!! Failed attempt to load promo landing page: " + promo_code);

					response_data = Utils.read_to_string(root + "/signup.html");
					}
				else
					{
					JSONObject promo_properties = new JSONObject();
					
					promo_properties.put("promo_code", promo_code);
					promo_properties.put("free_play_amount", promo.get("free_play_amount"));
					promo_properties.put("description", promo.get("description"));
					promo_properties.put("expires", promo.get("expires"));
					promo_properties.put("cancelled", promo.get("cancelled"));
					
					response_data = Utils.read_to_string(ssi_directory + "promo_landing.html");
					response_data = response_data.replace("<!--factory:promo_details-->", "<script>window.promo = " + promo_properties.toString() + ";</script>");
					}
				}
			
			switch (target_object)
				{
				case "session" :
					{
					if (session_active)
						{
						String user_id = session.user_id();
						
						JSONObject user = db.select_user("id", user_id);
						
						String currency = user.getString("currency");
						
						int deposit_bonus_claimed = user.getInt("deposit_bonus_claimed");
						
						double
						
						btc_balance = user.getDouble("btc_balance"),
						rc_balance = user.getDouble("rc_balance"),
						available_balance = add(btc_balance, rc_balance, 0),
						miner_fee = db.get_miner_fee(),
						withdrawal_fee = db.get_withdrawal_fee(),
						first_deposit = user.getDouble("first_deposit"),
						deposit_bonus_cap = user.getDouble("deposit_bonus_cap"),
						deposit_bonus_available = Math.min(first_deposit, deposit_bonus_cap);

						if (btc_balance < deposit_bonus_available) deposit_bonus_available = btc_balance;
						
						JSONObject 
						
						session_properties = new JSONObject(),
						internal_promotions = db.select_user("username", "internal_promotions");
	
						PreparedStatement check_for_promo  = sql_connection.prepareStatement("select count(*) from promo where referrer = ? and cancelled = 0");
						check_for_promo.setString(1, user_id);
						ResultSet result_set = check_for_promo.executeQuery();
	
						if (result_set.next()) 
							{
							int referral_code_count = result_set.getInt(1);
							session_properties.put("referral_code_count", referral_code_count);
							}
						else session_properties.put("referral_code_count", 0);
						
						session_properties.put("username", session.username());
						session_properties.put("user_level", session.user_level());
						session_properties.put("referral_offer", user.getDouble("referral_offer"));
						session_properties.put("btc_balance", btc_balance);
						session_properties.put("rc_balance", rc_balance);
						session_properties.put("available_balance", available_balance);
						session_properties.put("withdrawal_locked", user.getInt("withdrawal_locked"));
						session_properties.put("first_deposit", first_deposit);
						session_properties.put("deposit_bonus_claimed", deposit_bonus_claimed);
						session_properties.put("deposit_bonus_cap", deposit_bonus_cap);
						session_properties.put("deposit_bonus_rollover_multiple", user.getInt("deposit_bonus_rollover_multiple"));
						session_properties.put("deposit_bonus_available", deposit_bonus_available);
						session_properties.put("rollover_quota", user.getDouble("rollover_quota"));
						session_properties.put("rollover_progress", user.getDouble("rollover_progress"));
						session_properties.put("contest_status", user.getInt("contest_status"));
						session_properties.put("currency", currency);
						session_properties.put("btcusd_last_price", db.get_last_price("BTCUSD"));
						session_properties.put("currency_last_price", db.get_last_price(currency));
						session_properties.put("currency_description", db.get_currency_description(currency));
						session_properties.put("odds_format", user.getString("odds_format"));
						session_properties.put("cgs_address", user.getString("cgs_address"));
						session_properties.put("miner_fee", miner_fee);
						session_properties.put("withdrawal_fee_sat", btc_to_satoshi(withdrawal_fee));
						session_properties.put("withdrawal_fee", withdrawal_fee);
						session_properties.put("referral_link", Server.host + "/a/" + user.getString("referrer_key"));
						session_properties.put("internal_promo_balance", internal_promotions.getDouble("btc_balance"));
						
						response_data = "<script>window.session = " + session_properties.toString() + ";</script>";
						}
					else 
						{
						String currency = "USD";
						
						double
						
						btcusd_last_price = db.get_last_price("BTCUSD"),
						currency_last_price = db.get_last_price(currency);
						String currency_description = db.get_currency_description(currency);
						
						JSONObject inactive_properties = new JSONObject();
						
						inactive_properties.put("currency", currency);
						inactive_properties.put("btcusd_last_price", btcusd_last_price);
						inactive_properties.put("currency_last_price", currency_last_price);
						inactive_properties.put("currency_description", currency_description);
						
						response_data = "<script>window.inactive_session = " + inactive_properties.toString() + ";</script>";
						}
					} break;
				case "lobby_nav" :
				case "nav" :
					{
					if (!session_active) 
						{
						if (target_object.equals("lobby_nav")) response_data = "<!-- PLACEHOLDER -->";
						else response_data = Utils.read_to_string(ssi_directory + "nav_inactive.html");
						}
					else 
						{
						response_data = Utils.read_to_string(ssi_directory + "nav_active.html");
						response_data = response_data.replace("<!--ssi:username-->", session.username());
						if (session.user_level().equals("1")) response_data = response_data.replaceAll("ssi_header_admin_wrapper", "");
						}
					} break;
				case "nav_admin" :
					{
					if (session_active)
						{
						response_data = Utils.read_to_string(ssi_directory + "nav_admin.html");
						response_data = response_data.replace("<!--ssi:username-->", session.username());
						response_data = response_data.replaceAll("ssi_header_admin_wrapper", "");
						}
					} break;
				case "my_contests_count" :
					{
					if (session_active)
						{
						PreparedStatement count_contests = sql_connection.prepareStatement("select status, count(distinct(contest.id)) from contest inner join entry on entry.contest_id = contest.id where entry.user_id = ? group by contest.status order by contest.status asc");
						count_contests.setString(1, session.user_id());
	
						ResultSet result_set = count_contests.executeQuery();
						
						JSONObject contest_counts = new JSONObject();
	
						// initialize all counts to 0 since result_set will only contain statuses with positive counts
						
						contest_counts.put("open", 0);
						contest_counts.put("in_play", 0);
						contest_counts.put("settled", 0);
						
						while (result_set.next())
							{
							int 
							
							contest_status = result_set.getInt(1),
							contest_count = result_set.getInt(2);
							
							switch (contest_status)
								{
								case 1 :
									contest_counts.put("open", contest_count);
									break;
								case 2 :
									contest_counts.put("in_play", contest_count);
									break;
								case 3 :
									contest_counts.put("settled", contest_count);
									break;
								}
							}
						
						response_data = "<script>window.contest_counts = " + contest_counts.toString() + ";</script>";
						}
					else response_data = " ";
					} break;
				case "lobby_header" :
					{
					if (!session_active) response_data = Utils.read_to_string(ssi_directory + "lobby_inactive.html");
					else response_data = "<!-- PLACEHOLDER -->";
					} break;
				}
			
//------------------------------------------------------------------------------------

			if (response_data == null)
				{
				log("!!!!! Unauthorized SSI request: " + target_url);
				response_data = " ";
				}
			
			response.send(response_data);
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}
	}