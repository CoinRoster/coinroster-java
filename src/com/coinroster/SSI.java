package com.coinroster;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONObject;

public class SSI extends Utils
	{
	protected SSI(HttpRequest request, OutputStream response) throws Exception
		{
		String 
		
		response_data = null,
		root = request.header("root"), 				// e.g. /usr/share/nginx/html/coinroster.com
		ssi_directory = root + "/ssi-java/",  		// e.g. /usr/share/nginx/html/coinroster.com/ssi-java/
		target_url = request.target_url(), 			// e.g. /ssi-java/nav
		target_object = request.target_object(); 	// e.g. nav

		String session_token = request.cookie("session_token");
		Session session = new Session(session_token);
		
		boolean session_active = session.active();

		switch (target_object)
			{
			case "session" :
				{
				if (session_active)
					{
					Connection sql_connection = null;
					try {
						sql_connection = Server.sql_connection();
						
						DB db = new DB(sql_connection);
						
						JSONObject user = db.select_user("id", session.user_id());
						
						String currency = user.getString("currency");
						
						int withdrawal_locked = user.getInt("withdrawal_locked");
						
						double
						
						btc_balance = user.getDouble("btc_balance"),
						rc_balance = user.getDouble("rc_balance"),
						available_balance = add(btc_balance, rc_balance, 0),
						referral_offer = user.getDouble("referral_offer"),
						rollover_quota = user.getDouble("rollover_quota"),
						rollover_progress = user.getDouble("rollover_progress"),
						btcusd_last_price = db.get_last_price("BTCUSD"),
						currency_last_price = db.get_last_price(currency);
						
						String currency_description = db.get_currency_description(currency);
						
						JSONObject session_properties = new JSONObject();

						if (!user.isNull("referral_promo_code"))
							{
							String referral_promo_code = user.getString("referral_promo_code");
							session_properties.put("referral_promo_code", referral_promo_code);
							}
						
						session_properties.put("username", session.username());
						session_properties.put("user_level", session.user_level());
						session_properties.put("referral_offer", referral_offer);
						session_properties.put("btc_balance", btc_balance);
						session_properties.put("rc_balance", rc_balance);
						session_properties.put("available_balance", available_balance);
						session_properties.put("withdrawal_locked", withdrawal_locked);
						session_properties.put("rollover_quota", rollover_quota);
						session_properties.put("rollover_progress", rollover_progress);
						session_properties.put("contest_status", user.getInt("contest_status"));
						session_properties.put("currency", currency);
						session_properties.put("btcusd_last_price", btcusd_last_price);
						session_properties.put("currency_last_price", currency_last_price);
						session_properties.put("currency_description", currency_description);
						
						response_data = "<script>window.session = " + session_properties.toString() + ";</script>";
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
				else 
					{
					Connection sql_connection = null;
					try {
						sql_connection = Server.sql_connection();
						
						DB db = new DB(sql_connection);
		
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
				} break;
			case "nav" :
				{
				if (!session_active) response_data = Utils.read_to_string(ssi_directory + "nav_inactive.html");
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
					Connection sql_connection = null;
					try {
						sql_connection = Server.sql_connection();

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
				else response_data = " ";
				} break;
			case "lobby_header" :
				{
				if (!session_active) response_data = Utils.read_to_string(ssi_directory + "lobby_inactive.html");
				else response_data = Utils.read_to_string(ssi_directory + "lobby_active.html");
				} break;
			}

		if (response_data != null)
			{
			ByteArrayInputStream stream = new ByteArrayInputStream(response_data.getBytes(Utils.ENCODING));

			byte[] buffer = new byte[1024];

			for (int n; (n = stream.read(buffer, 0, buffer.length)) != -1;)
				{
				response.write(buffer, 0, n);
				response.flush();
				}
			
			stream.close();
			}
		else 
			{
			log("!!!!! Unauthorized SSI request: " + target_url);
			
			response.write(new String(" ").getBytes());
			response.flush();
			}
		}
	}