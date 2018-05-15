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
					
			boolean 
			
			success = false,
			deposit_bonus_activated = false;
			
			double deposit_amount = 0;
			
			try {
				lock : {
				
					user = db.select_user("id", user_id);
					
					String cgs_address = user.getString("cgs_address");
					
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
					
					if (cgs_current_balance == cgs_last_balance)
						{
						if (cgs_unconfirmed_amount > 0) output.put("error", "There is an unconfirmed balance of " + cgs_unconfirmed_amount + " BTC. We will credit your account once this amount is confirmed.");
						else output.put("error", "No new funds have been received and confirmed.");
						break lock;
						}
					
					if (cgs_current_balance < cgs_last_balance) // can this even happen?
						{
						output.put("error", "No new funds have been received and confirmed.");
						break lock;
						}
					
					deposit_amount = subtract(cgs_current_balance, cgs_last_balance, 0);
					
					JSONObject liability_account = db.select_user("username", "internal_liability");
					from_account = liability_account.getString("user_id");
					
					// get account balances:
				  
					double
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					user_btc_balance = user.getDouble("btc_balance");
	
					// subtract received amount from internal_btc_liability:
					
					Double new_btc_liability_balance = subtract(btc_liability_balance, deposit_amount, 0);
					
					db.update_btc_balance(from_account, new_btc_liability_balance);

					// add received amount to user balance:
					
					double new_btc_balance = add(user_btc_balance, deposit_amount, 0);
					
					db.update_btc_balance(user_id, new_btc_balance);
					db.update_cgs_balance(user_id, cgs_current_balance);
			
					// activate deposit bonus (if applicable)
					
					deposit_bonus_activated = db.enable_deposit_bonus(user, deposit_amount);

					success = true;
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
				
				pending_flag = 0;
				
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
				
				cash_register_email_address = cash_register.getString("email_address"),
				cash_register_admin = "Cash Register Admin",
				subject_admin = "",
				message_body_admin = "";
				
				if (deposit_bonus_activated)
					{
					subject = "Claim your Deposit Bonus!";
					
					message_body  = "Hi <b><!--USERNAME--></b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "We have received your first deposit and have credited to your account.";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "Transaction ID: <b>" + transaction_id + "</b>";
					message_body += "<br/>";
					message_body += "Amount received: <b>" + format_btc(deposit_amount) + " BTC</b>";
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "<a href='" + Server.host + "/account/deposit_bonus.html'>Click here</a> to claim your deposit bonus!";
					}
				else
					{
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
					}
				
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
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}