package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.CallCGS;
import com.coinroster.internal.UserMail;

public class CheckDeposit extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public CheckDeposit(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			long transaction_timestamp = System.currentTimeMillis();
			
			String 
			
			user_id = session.user_id(),
			created_by = user_id,
			transaction_type = "BTC-DEPOSIT",
			from_account = "",
			to_account = user_id,
			from_currency = "BTC",
			to_currency = "BTC",
			memo = "CGS BTC deposit";
			
			// start using SQL
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			JSONObject 
			user = null,
			cash_register = db.select_user("username", "internal_cash_register");
	
			String 
			
			cgs_address = null,
			new_cgs_address = null,
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "Cash Register Admin";
					
			boolean 
			
			success = false,
			deposit_bonus_activated = false;
			
			double deposit_amount = 0;
			
			try {
				lock : {
				
					user = db.select_user("id", user_id);
					
					cgs_address = user.getString("cgs_address");
					
					// CallCGS ----------------------------------------------------------------------------
					
					JSONObject rpc_call = new JSONObject();
					
					rpc_call.put("method", "getBalance");
					
					JSONObject rpc_method_params = new JSONObject();

					rpc_method_params.put("address", cgs_address);
					rpc_method_params.put("type", "btc");
					
					rpc_call.put("params", rpc_method_params);
					
					CallCGS call = new CallCGS(rpc_call);
					
					JSONObject 
					
					result = call.get_result(),
					balance = null;
					
					if (result != null) 
						{
						log(result.toString());
						balance = result.getJSONObject("balance");
						}
					else
						{
						log(call.get_error().toString());
						output.put("error", "No new funds have been received and confirmed.");
						break lock;
						}

					// end CallCGS ------------------------------------------------------------------------
					
					double 
					
					cgs_last_balance = user.getDouble("cgs_last_balance"),
					cgs_unconfirmed_amount = balance.getDouble("bitcoin_unc"),
					cgs_current_balance = balance.getDouble("bitcoin_cnf");

					if (cgs_current_balance == 0)
						{
						if (cgs_unconfirmed_amount > 0) 
							{
							log("unconfirmed balance");
							output.put("error", "There is an unconfirmed balance of " + cgs_unconfirmed_amount + " BTC. We will credit your account once this amount is confirmed.");
							break lock;
							}
						else
							{
							log("no pending tx");
							output.put("error", "No new funds have been received and confirmed.");
							break lock;
							}
						}			

					log("Processing deposit");
					JSONObject liability_account = db.select_user("username", "internal_liability");
					from_account = liability_account.getString("user_id");
					
					// get account balances:
				  
					double
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					user_btc_balance = user.getDouble("btc_balance");
	
					// subtract received amount from internal_btc_liability:
					
					Double new_btc_liability_balance = subtract(btc_liability_balance, cgs_current_balance, 0);
					
					db.update_btc_balance(from_account, new_btc_liability_balance);

					// add received amount to user balance:
					
					double new_btc_balance = add(user_btc_balance, cgs_current_balance, 0);
					
					db.update_btc_balance(user_id, new_btc_balance);
					db.update_cgs_balance(user_id, cgs_current_balance);
					
			
					// activate deposit bonus (if applicable)
					deposit_amount = cgs_current_balance;
					deposit_bonus_activated = db.enable_deposit_bonus(user, deposit_amount);
					success = true;

					// push deposited amount to cold storage
					
					// CallCGS ----------------------------------------------------------------------------
					
					JSONObject rpc_call_cold_storage = new JSONObject();
					
					rpc_call_cold_storage.put("method", "pushToColdStorage");
					
					JSONObject rpc_method_params_cold_storage = new JSONObject();

					rpc_method_params_cold_storage.put("address", cgs_address);
					rpc_method_params_cold_storage.put("type", "btc");
					
					rpc_call_cold_storage.put("params", rpc_method_params);
					
					CallCGS call_cold_storage = new CallCGS(rpc_call_cold_storage);
					
					JSONObject 
					
					result_cold_storage = call_cold_storage.get_result();
					
					if (result_cold_storage != null) 
						{
						log("Successfully pushed deposit amount to cold storage" + result_cold_storage.toString());
						}
					else
						{
						log("Error pushing to cold storage" + call_cold_storage.get_error().toString());
						}
					
					// ------------------call new account------------------
					
					JSONObject rpc_call_new_account = new JSONObject();

					rpc_call_new_account.put("method", "newAccount");
					
					JSONObject rpc_method_params_new_account = new JSONObject();

					rpc_method_params_new_account.put("type", "btc");
					rpc_method_params_new_account.put("craccount", user.getString("username"));
					
					rpc_call_new_account.put("params", rpc_method_params_new_account);
					
					CallCGS call_new_account = new CallCGS(rpc_call_new_account);
					
					JSONObject result_new_account = call_new_account.get_result();
					
					if (result_new_account != null) new_cgs_address = result_new_account.getString("account");
					else
						{
						output.put("error", "Could not generate bitcoin wallet. Please try again shortly.");
						break lock;
						}

					// end CallCGS ------------------------------------------------------------------------
					db.update_cgs_address(user_id, new_cgs_address);
					}
				}
			catch (Exception e)
				{
				output.put("error", "No new funds have been received and confirmed.");
				log("ERROR DEPOSITING TO " + session.username());
				Server.exception(e);
				}
			finally
				{
				statement.execute("unlock tables");
				}

			if (success)
				{
				int 
				
				pending_flag = 1;
				
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				new_transaction.setLong(1, transaction_timestamp);
				new_transaction.setString(2, created_by);
				new_transaction.setString(3, transaction_type);
				new_transaction.setString(4, from_account);
				new_transaction.setString(5, to_account);
				new_transaction.setDouble(6, deposit_amount);
				new_transaction.setString(7, from_currency);
				new_transaction.setString(8, to_currency);
				new_transaction.setString(9, memo);
				new_transaction.setInt(10, pending_flag);
				new_transaction.execute();
				
				ResultSet rs = new_transaction.getGeneratedKeys();
			    rs.next();
			    int transaction_id = rs.getInt(1);
				
				// communications
				
				String
				
				subject = "",
				message_body = "",
				
				subject_admin = "",
				message_body_admin = "";
				
//				if (deposit_bonus_activated)
//					{
//					subject = "Claim your Deposit Bonus!";
//					
//					message_body  = "Hi <b><!--USERNAME--></b>";
//					message_body += "<br/>";
//					message_body += "<br/>";
//					message_body += "We have received your first deposit and have credited to your account.";
//					message_body += "<br/>";
//					message_body += "<br/>";
//					message_body += "Transaction ID: <b>" + transaction_id + "</b>";
//					message_body += "<br/>";
//					message_body += "Amount received: <b>" + format_btc(deposit_amount) + " BTC</b>";
//					message_body += "<br/>";
//					message_body += "<br/>";
//					message_body += "<a href='" + Server.host + "/account/deposit_bonus.html'>Click here</a> to claim your deposit bonus!";
//					}
//				else
//					{
				
				subject = "Deposit confirmation";
				
				message_body  = "Hi <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have received your deposit and have credited to your account.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body += "<br/>";
				message_body += "Amount received: <b>" + format_btc(deposit_amount) + " BTC</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
				
//					}
				
				subject_admin = "Deposit confirmation";
				
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Deposit received from user <b>" + session.username() + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "Amount received: <b>" + format_btc(deposit_amount) + " BTC</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "The amount will be pushed to cold storage shortly.";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";

				Server.send_mail(cash_register_email_address, cash_register_admin, subject_admin, message_body_admin);
				
				new UserMail(user, subject, message_body);
				
				output.put("deposit_amount", deposit_amount);
				output.put("status", "1");
				}
			else if (!output.has("error"))
				{
				String subject = "CGS Deposit Error!",
						
				message_body = "An error was encountered in processing a deposit for: <b>" + session.username() + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "User bitcoin address: <b>" + cgs_address + "<b>";
				
				Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);
				
				log("Message sent to cash register admin");
				output.put("status", 0);
				output.put("error", "We cannot process this transaction automatically. An admin has been notified and will get back to you shortly.");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}