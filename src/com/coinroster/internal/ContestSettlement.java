package com.coinroster.internal;

import java.sql.Connection;
//import java.sql.PreparedStatement;

//import org.json.JSONObject;

//import com.coinroster.DB;

public class ContestSettlement
	{
	public ContestSettlement(Connection sql_connection, int contest_id, String settlement_type) throws Exception
		{
		//DB db = new DB(sql_connection);
		
		switch (settlement_type)
			{
			case "HEADS-UP" :
				
				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 3);
				
				break;

			case "DOUBLE-UP" :

				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 3);
				
				break;

			case "JACKPOT" :

				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 3);
				
				break;
				
			case "UNDER-SUBSCRIBED" :

				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 4);
				
				break;
				
			case "CANCELLED" :

				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 5);
				
				break;
				
			}
		
		/*

		// swap RC liability to BTC liability
		
		String user_id = "set me";
		
		double rc_swap_amount = 0; // set me
		
		JSONObject 
		
		rc_liability = db.select_user("username", "internal_rc_liability"),
		btc_liability = db.select_user("username", "internal_btc_liability");
		
		String
		
		rc_liability_id = rc_liability.getString("user_id"),
		btc_liability_id = btc_liability.getString("user_id");
				
		double
		
		rc_liability_balance = rc_liability.getDouble("rc_balance"),
		btc_liability_balance = btc_liability.getDouble("btc_balance");
		
		rc_liability_balance += rc_swap_amount; // add transaction amount (decreases liability)
		btc_liability_balance -= rc_swap_amount; // subtract transaction amount (increases liability)
		
		// update liability balances
		
		PreparedStatement update_rc_liability = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
		update_rc_liability.setDouble(1, rc_liability_balance);
		update_rc_liability.setString(2, rc_liability_id);
		update_rc_liability.executeUpdate();
		
		PreparedStatement update_btc_liability = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
		update_btc_liability.setDouble(1, btc_liability_balance);
		update_btc_liability.setString(2, btc_liability_id);
		update_btc_liability.executeUpdate();
		
		// create swap transaction

		String
		
		created_by = user_id,
		transaction_type = "RC-SWAP-TO-BTC",
		from_account = rc_liability_id,
		to_account = btc_liability_id,
		from_currency = "RC",
		to_currency = "BTC",
		memo = "SET ME";
		
		PreparedStatement swap = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");				
		swap.setLong(1, System.currentTimeMillis());
		swap.setString(2, created_by);
		swap.setString(3, transaction_type);
		swap.setString(4, from_account);
		swap.setString(5, to_account);
		swap.setDouble(6, rc_swap_amount);
		swap.setString(7, from_currency);
		swap.setString(8, to_currency);
		swap.setString(9, memo);
		swap.executeUpdate();
		
		*/
		}
	}
