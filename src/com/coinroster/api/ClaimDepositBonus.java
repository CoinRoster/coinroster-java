package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class ClaimDepositBonus extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public ClaimDepositBonus(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_id = session.user_id(),
			btc_liability_id = null;
			
			JSONObject user = null;
			
			double actual_bonus = 0;
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, transaction write");
			
			boolean success = false;

			try {
				lock : {

					user = db.select_user("id", user_id);

					double first_deposit = user.getDouble("first_deposit");
					
					if (first_deposit == 0)
						{
						output.put("error", "Please make a deposit.");
						break lock;
						}

					int deposit_bonus_claimed = user.getInt("deposit_bonus_claimed");
					
					if (deposit_bonus_claimed == 1)
						{
						output.put("error", "You have already claimed your deposit bonus.");
						break lock;
						}

					// if we get here, user can claim a deposit bonus

					// select liability account
					
					JSONObject liability_account = db.select_user("username", "internal_liability");
					btc_liability_id = liability_account.getString("user_id");
					
					double 
					
					deposit_bonus_cap = user.getDouble("deposit_bonus_cap"),
					deposit_bonus_rollover_multiple = user.getInt("deposit_bonus_rollover_multiple"),
					current_rollover_quota = user.getDouble("rollover_quota"),
					user_btc_balance = user.getDouble("btc_balance");

					// calculations
					
					actual_bonus = Math.min(first_deposit, deposit_bonus_cap);
					
					double 

		            additional_playing_requirement = multiply(actual_bonus, deposit_bonus_rollover_multiple, 0),
		            new_rollover_quota = add(current_rollover_quota, additional_playing_requirement, 0),
					new_btc_balance = add(user_btc_balance, actual_bonus, 0),

					// calculate and update liability balance
					
					btc_liability_balance = liability_account.getDouble("btc_balance"),
					new_btc_liability_balance = subtract(btc_liability_balance, actual_bonus, 0);

					db.update_btc_balance(btc_liability_id, new_btc_liability_balance);
					
					// update user
					
					PreparedStatement update_user = sql_connection.prepareStatement("update user set deposit_bonus_claimed = 1, withdrawal_locked = 1, btc_balance = ?, rollover_quota = ? where id = ?");
					update_user.setDouble(1, new_btc_balance);
					update_user.setDouble(2, new_rollover_quota);
					update_user.setString(3, user_id);
					update_user.executeUpdate();
					
					success = true;
					}
				}
			catch (Exception e)
				{
				Server.exception(e);
				output.put("error_message", "Unable to claim bonus");
				}
			finally
				{
				statement.execute("unlock tables");
				}
			
			if (success)
				{
				String
				
				created_by = session.user_id(),
				transaction_type = "BTC-DEPOSIT-BONUS",
				from_account = btc_liability_id,
				to_account = user_id,
				from_currency = "BTC",
				to_currency = "BTC",
				memo = "Deposit Match Bonus";
				
				PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
				new_transaction.setLong(1, System.currentTimeMillis());
				new_transaction.setString(2, created_by);
				new_transaction.setString(3, transaction_type);
				new_transaction.setString(4, from_account);
				new_transaction.setString(5, to_account);
				new_transaction.setDouble(6, actual_bonus);
				new_transaction.setString(7, from_currency);
				new_transaction.setString(8, to_currency);
				new_transaction.setString(9, memo);
				new_transaction.executeUpdate();
				
				String
				
				subject = "Your Deposit Match bonus has been credited!",
				
				message_body = "Hi <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Your Deposit Match bonus has been credited to your account!";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "<a href='" + Server.host + "/account'>Click here</a> to view your account.";
				
				new UserMail(user, subject, message_body);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}