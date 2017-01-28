package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;

import org.json.JSONObject;

import com.coinroster.DB;

public class ContestSettlement
	{
	Connection sql_connection;
	int contest_id;
	String subject, message_body;
	DB db;
	
	HashSet<String> users = new HashSet<String>();
	
	public ContestSettlement(Connection sql_connection, int contest_id, String settlement_type) throws Exception
		{
		this.sql_connection = sql_connection;
		this.contest_id = contest_id;
		this.db = new DB(sql_connection);
		
		JSONObject contest = db.select_contest(contest_id);
		
		String contest_title = contest.getString("title");
		
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
				
				unwind_contest();
				
				subject = contest_title + " is under-subscribed";
				
				message_body = "Hello <b><!--USERNAME--></b>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Not enough users entered " + contest_title + ".";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "The contest has been cancelled and your entry fees have been credited back to your account.";
				
				break;
				
			case "CANCELLED" :

				// do settlement here
				
				new UpdateContestStatus(sql_connection, contest_id, 5);

				unwind_contest();
				
				subject = contest_title + " has been cancelled";
				
				message_body = "Hello <b><!--USERNAME--></b>,";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "We have decided to cancel " + contest_title + ".";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Your entry fees have been credited back to your account.";
				
				break;
			}
		
		// whatever happens, notify users
		
		for (String user_id : users) new UserMail(db.select_user("id", user_id), subject, message_body);
		
		/*
		
		// swap RC liability to BTC liability
		
		String user_id = "set me";
		
		double rc_swap_amount = 0; // set me
		
		JSONObject liability_account = db.select_user("username", "internal_liability");
		
		String liability_account_id = liability_account.getString("user_id");
				
		double
		
		rc_liability_balance = liability_account.getDouble("rc_balance"),
		btc_liability_balance = liability_account.getDouble("btc_balance");
		
		rc_liability_balance += rc_swap_amount; // add transaction amount (decreases liability)
		btc_liability_balance -= rc_swap_amount; // subtract transaction amount (increases liability)
		
		// update liability balances
		
		PreparedStatement update_liability = sql_connection.prepareStatement("update user set rc_balance = ?, btc_balance = ? where id = ?");
		update_liability.setDouble(1, rc_liability_balance);
		update_liability.setDouble(2, btc_liability_balance);
		update_liability.setString(2, liability_account_id);
		update_liability.executeUpdate();
		
		// create swap transaction

		String
		
		created_by = user_id,
		transaction_type = "RC-SWAP-TO-BTC",
		from_account = liability_account_id,
		to_account = liability_account_id,
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
	
	private void unwind_contest() throws Exception
		{
		// lock it all
		
		Statement statement = sql_connection.createStatement();
		statement.execute("lock tables user write, contest write, entry write, transaction write");

		JSONObject contest_account = db.select_user("username", "internal_contest_asset");
		
		String contest_account_id = contest_account.getString("user_id");
		
		double
		
		btc_contest_account_balance = contest_account.getDouble("btc_balance"),
		rc_contest_account_balance = contest_account.getDouble("rc_balance");
		
		PreparedStatement select_transactions = sql_connection.prepareStatement("select * from transaction where contest_id = ?");
		select_transactions.setInt(1, contest_id);
		ResultSet transaction = select_transactions.executeQuery();
		
		// loop through all contests with open registration

		while (transaction.next())
			{
			int transaction_id = transaction.getInt(1);
			String created_by = transaction.getString(3);
			String trans_type = transaction.getString(4);
			String from_account = transaction.getString(5);
			String to_account = transaction.getString(6);
			double amount = transaction.getDouble(7);
			String from_currency = transaction.getString(8);
			String to_currency = transaction.getString(9);
			String memo = transaction.getString(10);
			int contest_id = transaction.getInt(13);
			
			String user_id = from_account;
			
			if (trans_type.endsWith("CONTEST-ENTRY"))
				{
				// build up unique list of users for communications
				
				if (!users.contains(user_id)) users.add(user_id);
				
				// select user and update balance
				
				JSONObject user = db.select_user("id", user_id);
				
				double user_balance = user.getDouble(from_currency.toLowerCase() + "_balance");
				
				user_balance += amount;

				if (from_currency.equals("BTC")) btc_contest_account_balance -= amount;
				else if (from_currency.equals("RC")) rc_contest_account_balance -= amount;
				
				PreparedStatement update_user_balance = sql_connection.prepareStatement("update user set " + from_currency.toLowerCase() + "_balance = ? where id = ?");
				update_user_balance.setDouble(1, user_balance);
				update_user_balance.setString(2, user_id);
				update_user_balance.executeUpdate();
				
				// create reversal transaction
				
				created_by = contest_account_id;
				trans_type = from_currency + "-CONTEST-ENTRY-REVERSAL";
				from_account = contest_account_id;
				to_account = user_id;
				memo = "Reversal of transaction " + transaction_id;
				
				PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				create_transaction.setLong(1, System.currentTimeMillis());
				create_transaction.setString(2, created_by);
				create_transaction.setString(3, trans_type);
				create_transaction.setString(4, from_account);
				create_transaction.setString(5, to_account);
				create_transaction.setDouble(6, amount);
				create_transaction.setString(7, from_currency);
				create_transaction.setString(8, to_currency);
				create_transaction.setString(9, memo);
				create_transaction.setInt(10, contest_id);
				create_transaction.executeUpdate();
				}
			}
		
		PreparedStatement update_user_balances = sql_connection.prepareStatement("update user set btc_balance = ?, rc_balance = ? where id = ?");
		update_user_balances.setDouble(1, btc_contest_account_balance);
		update_user_balances.setDouble(2, rc_contest_account_balance);
		update_user_balances.setString(3, contest_account_id);
		update_user_balances.executeUpdate();
		
		statement.execute("unlock tables");
		}
	}
