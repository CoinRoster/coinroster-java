package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.CallCGS;
import com.coinroster.internal.UserMail;

public class UserWithdrawal extends Utils
	{
	public static String method_level = "standard";
	public UserWithdrawal(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			double 
			
			withdrawal_amount = input.getDouble("amount_to_withdraw"),
			miner_fee, 
			final_miner_fee = 0.0,
			withdrawal_fee = db.get_withdrawal_fee(),
			total_amount = 0.0,
			cash_register_balance = 0.0;

			try 
				{
				miner_fee = input.getDouble("custom_miner_fee");
				} 
			catch (Exception E) 
				{
				miner_fee = db.get_miner_fee();
				}
			
			if (withdrawal_amount <= 0)
				{
				output.put("error", "Amount must be positive");
				break method;
				}
			
			String 
			
			user_id = session.user_id(),
			btc_liability_id = db.get_id_for_username("internal_liability"),
			btc_withdrawal_id = db.get_id_for_username("internal_withdrawal_fee"),
			
			ext_address = "",
			
			created_by = user_id,
			transaction_type = "BTC-WITHDRAWAL",
			from_account = user_id,
			to_account = btc_liability_id,
			from_currency = "BTC",
			to_currency = "BTC",
			memo = "CGS BTC withdrawal";
			
			int pending_flag = 0;
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write"); // need control for miner_fee

			JSONObject 
			
			user = null,
			cash_register = db.select_user("username", "internal_cash_register");
			
			String 
			
			cash_register_email_address = cash_register.getString("email_address"),
			cash_register_admin = "Cash Register Admin";
			
			boolean success = false;
					
			try {
				lock : {
				
					user = db.select_user("id", user_id);
					
					if (user.getInt("withdrawal_locked") == 1)
						{
						output.put("error", "You cannot withdraw funds until you meet a playing requirement");
						break lock;
						}

					JSONObject 
					
					btc_liability = db.select_user("id", btc_liability_id),
					btc_withdrawal = db.select_user("id", btc_withdrawal_id);
					
					// get account balances:
				  
					double
					
					btc_liability_balance = btc_liability.getDouble("btc_balance"),
					user_btc_balance = user.getDouble("btc_balance"),
					btc_withdrawal_balance = btc_withdrawal.getDouble("btc_balance");

					
					// withdrawal-specific logic:
					
					if (add(withdrawal_amount, miner_fee, 0) > subtract(user_btc_balance, withdrawal_fee, 0)) 
						{					
						output.put("error", "Insufficient funds");
						break lock;
						}

					/*double 
					
					cgs_last_balance = user.getDouble("cgs_last_balance"),
					updated_cgs_balance = subtract(cgs_last_balance, withdrawal_amount, 0);
					
					if (updated_cgs_balance < 0) updated_cgs_balance = 0;
					
					db.update_cgs_balance(user_id, updated_cgs_balance);*/

					String cgs_address = user.getString("cgs_address");
					ext_address = user.getString("ext_address");
					
					double withdrawal_amount_satoshis = btc_to_satoshi(withdrawal_amount);
					
					DecimalFormat f = new DecimalFormat("0.00000000");
			        String withdrawal_amount_string = f.format(withdrawal_amount_satoshis);
			        
					// CallCGS ----------------------------------------------------------------------------
					
					JSONObject rpc_call = new JSONObject();
					
					rpc_call.put("method", "sendTransaction");
					
					JSONObject rpc_method_params = new JSONObject();

					rpc_method_params.put("fromAddress", cgs_address);
					rpc_method_params.put("toAddress", ext_address);
					rpc_method_params.put("type", "btc");
					rpc_method_params.put("user_balance", user_btc_balance);
					
					if (miner_fee > 0.0) {
						rpc_method_params.put("custom_fee", f.format(btc_to_satoshi(miner_fee)));
					}
					
					JSONObject amount = new JSONObject();
					amount.put("satoshi", withdrawal_amount_string);

					rpc_method_params.put("amount", amount);
					
					rpc_call.put("params", rpc_method_params);
					
					CallCGS call = new CallCGS(rpc_call);
					
					JSONObject 
					
					result = call.get_result(),
					error = call.get_error();
					
					if (result != null)
						{
						log(result);
						final_miner_fee = satoshi_to_btc(result.getDouble("fees"));
						total_amount = add(add(withdrawal_amount, final_miner_fee, 0), withdrawal_fee, 0);
						cash_register_balance = satoshi_to_btc(result.getDouble("cashRegisterBalance"));
						
						
						// deduct withdrawal amount from user:
						
						Double new_btc_balance = subtract(user_btc_balance, total_amount, 0);
				
						db.update_btc_balance(user_id, new_btc_balance);
							
						// add withdrawal amount to internal_btc_liability (decreases liability):
						
						Double new_btc_liability_balance = add(btc_liability_balance, total_amount, 0);
						
						db.update_btc_balance(btc_liability_id, new_btc_liability_balance);
						
						// deduct withdrawal amount from internal_btc_liability
						Double new_btc_liability_balance_withdrawal = subtract(new_btc_liability_balance, withdrawal_fee, 0);
						
						db.update_btc_balance(btc_liability_id, new_btc_liability_balance_withdrawal);
						
						// add withdrawal fee to internal_btc_liability (decreases liability):
						
						Double new_btc_withdrawal_balance = add(btc_withdrawal_balance, withdrawal_fee, 0);
						
						db.update_btc_balance(btc_withdrawal_id, new_btc_withdrawal_balance);
						
						success = true;
						}
					if (error != null)
						{						
						
						String subject = "CGS Withdrawal Error!",
						
						message_body = "An error was encountered in processing a withdrawal for: <b>" + session.username() + "</b>";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += error.toString();
						
						Server.send_mail(cash_register_email_address, cash_register_admin, subject, message_body);
						
						log("Message sent to cash register admin");
						success = false;
						}

					// end CallCGS ------------------------------------------------------------------------
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
		
			if (success)
				{
				Long transaction_timestamp = System.currentTimeMillis();
				
				// User transaction, with miner fee and withdrawal fee
				PreparedStatement user_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				user_transaction.setLong(1, transaction_timestamp);
				user_transaction.setString(2, created_by);
				user_transaction.setString(3, transaction_type);
				user_transaction.setString(4, from_account);
				user_transaction.setString(5, to_account);
				user_transaction.setDouble(6, total_amount);
				user_transaction.setString(7, from_currency);
				user_transaction.setString(8, to_currency);
				user_transaction.setString(9, memo);
				user_transaction.setInt(10, pending_flag);
				user_transaction.setString(11, ext_address);
				user_transaction.execute();
				
				ResultSet rs = user_transaction.getGeneratedKeys();
			    rs.next();
			    int transaction_id = rs.getInt(1);
			    
			    output.put("transaction_id", transaction_id);
				
			    PreparedStatement internal_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				internal_transaction.setLong(1, transaction_timestamp);
				internal_transaction.setString(2, created_by);
				internal_transaction.setString(3, "BTC-DEPOSIT");
				internal_transaction.setString(4, btc_liability_id);
				internal_transaction.setString(5, btc_withdrawal_id);
				internal_transaction.setDouble(6, withdrawal_fee);
				internal_transaction.setString(7, from_currency);
				internal_transaction.setString(8, to_currency);
				internal_transaction.setString(9, "Withdrawal fee credit");
				internal_transaction.setInt(10, pending_flag);
				internal_transaction.setString(11, ext_address);
				internal_transaction.execute();
				
				ResultSet internal_rs = internal_transaction.getGeneratedKeys();
			    internal_rs.next();
			    int internal_transaction_id = internal_rs.getInt(1);
			    String network = (Server.isLive_server()?"":"testnet");
			    // send withdrawal confirmation
			  			
				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(transaction_timestamp);
				
				String 

				subject_admin = "",
				message_body_admin = "",
				subject = "Withdrawal confirmation", 
				
				message_body = "Hi <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Your request to withdraw funds has been processed and the funds have been sent";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body += "<br/>";
				message_body += "Type: <b>" + transaction_type + "</b>";
				message_body += "<br/>";
				message_body += "Date and time: <b>" + formatter.format(calendar.getTime()) + "</b>";
				message_body += "<br/>";
				message_body += "Amount: <b>" + format_btc(total_amount) + " BTC</b>";
				message_body += "<br/>";
				message_body += "To (wallet on file): <b>" + ext_address + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You may view your account <a href='" + Server.host + "/account/'>here</a>.";
					
				subject_admin = "Withdrawal confirmation";
				
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Withdrawal deposited from cash register to user <b>" + session.username() + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Transaction ID: <b>" + transaction_id + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "Amount sent: <b>" + format_btc(total_amount) + " BTC</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Internal withdrawal fee transaction ID: <b>" + internal_transaction_id + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "Amount remaining in the cash register: <b>" + cash_register_balance + " BTC</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<a href=\"https://" + network + "blockchain.info/address/" + user.getString("cgs_address")+ "\"> View on the explorer</a>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";

				Server.send_mail(cash_register_email_address, cash_register_admin, subject_admin, message_body_admin);
				
				new UserMail(user, subject, message_body);
				
			    output.put("status", "1");
				}
			else
				{
				log("Insufficient balance in cash register");
				output.put("error", "We cannot process this transaction automatically. An admin has been notified and will get back to you shortly.");
				}
						
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}