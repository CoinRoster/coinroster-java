package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;

public class BackoutContest extends Utils {
	
	public BackoutContest(Connection sql_connection, int contest_id, String reason) {
		
		Server.log("Contest #" + contest_id + " is being backed out");
		
		DB db = new DB(sql_connection);
		try {

			new UpdateContestStatus(sql_connection, contest_id, 6);
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, contest write, entry write, transaction write");

			HashSet<String> users = new HashSet<String>();
			
			// get contest
			JSONObject contest = db.select_contest(contest_id);

			try {
				JSONObject contest_account = db.select_user("username", "internal_contest_asset");
				
				String contest_account_id = contest_account.getString("user_id");
				
				double
				
				contest_account_btc_balance = contest_account.getDouble("btc_balance"),
				contest_account_rc_balance = contest_account.getDouble("rc_balance");
				
				
				// get transactions involving contest
				PreparedStatement select_transactions = sql_connection.prepareStatement("select * from transaction where contest_id = ?");
				select_transactions.setInt(1, contest_id);
				ResultSet transaction = select_transactions.executeQuery();
				
				// loop through all contests with open registration

				while (transaction.next())
					{
					String trans_type = transaction.getString(4);
					String from_account = transaction.getString(5);
					String to_account = transaction.getString(6);
					double amount = transaction.getDouble(7);
					String from_currency = transaction.getString(8);
					String to_currency = transaction.getString(9);
					
					String user_id = from_account; // transaction was created by a user - we need to key off their user_id
					from_account = contest_account_id; // now we invert the transaction (to_account is assigned in loop)

					String created_by = contest_account_id;
					String memo = "Not enough users entered: " + contest.getString("title");
					
					// User transactions: entries and fixed-odds escrow
					if (trans_type.endsWith("CONTEST-ENTRY") || trans_type.equals("FIXED-ODDS-RISK-ESCROW"))
						{
						// build up unique list of users for communications
						
						if (!users.contains(user_id)) users.add(user_id);
						
						JSONObject user = db.select_user("id", user_id);

						// update balance
						
						if (from_currency.equals("BTC")) 
							{
							double user_btc_balance = user.getDouble("btc_balance");
							
							user_btc_balance = add(user_btc_balance, amount, 0); // credit user
							contest_account_btc_balance = subtract(contest_account_btc_balance, amount, 0); // debit contest account
							
							db.update_btc_balance(user_id, user_btc_balance);
							}
						else if (from_currency.equals("RC")) 
							{
							double user_rc_balance = user.getDouble("rc_balance");
							
							user_rc_balance = add(user_rc_balance, amount, 0); // credit user
							contest_account_rc_balance = subtract(contest_account_rc_balance, amount, 0); // debit contest account
							
							db.update_rc_balance(user_id, user_rc_balance);
							}

						// create reversal transaction

						trans_type = from_currency + "-CONTEST-ENTRY-REVERSAL";
						to_account = user_id;
						
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
				
				db.update_btc_balance(contest_account_id, contest_account_btc_balance);
				db.update_rc_balance(contest_account_id, contest_account_rc_balance);
				}
			catch (Exception e)
				{
				Server.exception(e);
				}
			finally
				{
				statement.execute("unlock tables");
				}
			
			String subject = contest.getString("title") + " has been cancelled";
			
			String message_body  = "Hi <b><!--USERNAME--></b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "You were participating in the contest: <b>" + contest.getString("title")  + "</b>";
			message_body += "<br/>";
			message_body += "<br/>";
			if(reason.equals("TIE"))
				message_body += "The contest has resulted in a push due to a tie. Your entry fees have been credited back to your account.";
			else
				message_body += "The contest has been cancelled and your entry fees have been credited back to your account.";
			
			for (String user_id : users) new UserMail(db.select_user("id", user_id), subject, message_body);
			
			// finally, if it's a voting contest, backout the betting round
			if(db.is_voting_contest(contest_id)) new BackoutContest(sql_connection, db.get_original_contest(contest_id), "");
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

	}
}