package com.coinroster;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

public class DB 
	{
	Connection sql_connection;
	
	public DB (Connection sql_connection)
		{
		this.sql_connection = sql_connection;
		}

//------------------------------------------------------------------------------------
	
	// GET USERNAME FOR ID

	public String get_username_for_id(String id) throws Exception
		{
		String username = null;

		PreparedStatement select_username = sql_connection.prepareStatement("select username from user where id = ?");
		select_username.setString(1, id);
		ResultSet result_set = select_username.executeQuery();

		if (result_set.next()) username = result_set.getString(1);

		return username;
		}

//------------------------------------------------------------------------------------
	
	// GET USERNAME FOR ID

	public String get_id_for_username(String username) throws Exception
		{
		String id = null;

		PreparedStatement select_username = sql_connection.prepareStatement("select id from user where username = ?");
		select_username.setString(1, username);
		ResultSet result_set = select_username.executeQuery();

		if (result_set.next()) id = result_set.getString(1);

		return id;
		}

//------------------------------------------------------------------------------------

	// SELECT USER
	
	public JSONObject select_user(String column, String value) throws Exception
		{
		JSONObject user = null;

		PreparedStatement select_user = sql_connection.prepareStatement("select * from user where " + column + " = ?");
		select_user.setString(1, value);
		ResultSet result_set = select_user.executeQuery();

		if (result_set.next())
			{
			user = new JSONObject();
			
			String user_id = result_set.getString(1);
			String username = result_set.getString(2);
			String stored_password_hash = result_set.getString(3);
			int user_level = result_set.getInt(4);
			Long created = result_set.getLong(5);
			Long last_login = result_set.getLong(6);
			double btc_balance = result_set.getDouble(7);
			double rc_balance = result_set.getDouble(8);
			String ext_address = result_set.getString(9);
			String email_address = result_set.getString(10);
			String email_ver_key = result_set.getString(11);
			int email_ver_flag = result_set.getInt(12);
			int newsletter_flag = result_set.getInt(13);
			int referral_program = result_set.getInt(14);
			String referrer = result_set.getString(15);
			int ext_address_secure_flag = result_set.getInt(16);
			int free_play = result_set.getInt(17);

			if (ext_address == null) ext_address = "";
			if (email_address == null) email_address = "";
			if (referrer == null) referrer = "";
			
			user.put("user_id", user_id);
			user.put("username", username);
			user.put("stored_password_hash", stored_password_hash);
			user.put("user_level", user_level);
			user.put("created", created);
			user.put("last_login", last_login);
			user.put("btc_balance", btc_balance);
			user.put("rc_balance", rc_balance);
			user.put("ext_address", ext_address);
			user.put("email_address", email_address);
			user.put("email_ver_key", email_ver_key);
			user.put("email_ver_flag", email_ver_flag);
			user.put("newsletter_flag", newsletter_flag);
			user.put("referral_program", referral_program);
			user.put("referrer", referrer);
			user.put("ext_address_secure_flag", ext_address_secure_flag);
			user.put("free_play", free_play);
			}

		return user;
		}

//------------------------------------------------------------------------------------
	
	// SELECT TRANSACTION

	public JSONObject select_transaction(int transaction_id) throws Exception
		{
		JSONObject transaction = null;

		PreparedStatement select_transaction  = sql_connection.prepareStatement("select * from transaction where id = ?");
		select_transaction.setInt(1, transaction_id);

		ResultSet result_set = select_transaction.executeQuery();

		if (result_set.next())
			{			
			transaction = new JSONObject();
			
			//int transaction_id = result_set.getInt(1);
			long created = result_set.getLong(2);
			String created_by = result_set.getString(3);
			String trans_type = result_set.getString(4);
			String from_account = result_set.getString(5);
			String to_account = result_set.getString(6);
			double amount = result_set.getDouble(7);
			String from_currency = result_set.getString(8);
			String to_currency = result_set.getString(9);
			String memo = result_set.getString(10);
			int pending_flag = result_set.getInt(11);
			String ext_address = result_set.getString(12);
			int contest_id = result_set.getInt(13);
			int cancelled_flag = result_set.getInt(14);
			
			transaction.put("transaction_id", transaction_id);
			transaction.put("created", created);
			transaction.put("created_by", created_by);
			transaction.put("trans_type", trans_type);
			transaction.put("from_account", from_account);
			transaction.put("to_account", to_account);
			transaction.put("amount", amount);
			transaction.put("from_currency", from_currency);
			transaction.put("to_currency", to_currency);
			transaction.put("memo", memo);
			transaction.put("pending_flag", pending_flag);
			transaction.put("ext_address", ext_address);
			transaction.put("contest_id", contest_id);
			transaction.put("cancelled_flag", cancelled_flag);
			}

		return transaction;
		}

//------------------------------------------------------------------------------------
	
	// GET CATEGORY DESCRIPTION

	public String get_category_description(String code) throws Exception
		{
		String description = null;

		PreparedStatement select_category = sql_connection.prepareStatement("select description from category where code = ?");
		select_category.setString(1, code);
		ResultSet result_set = select_category.executeQuery();

		if (result_set.next()) description = result_set.getString(1);

		return description;
		}

//------------------------------------------------------------------------------------
	
	// GET SUB-CATEGORY DESCRIPTION

	public String get_sub_category_description(String code) throws Exception
		{
		String description = null;

		PreparedStatement select_sub_category = sql_connection.prepareStatement("select description from sub_category where code = ?");
		select_sub_category.setString(1, code);
		ResultSet result_set = select_sub_category.executeQuery();

		if (result_set.next()) description = result_set.getString(1);

		return description;
		}

//------------------------------------------------------------------------------------

	// GET CONTEST TITLE

	public String get_contest_title(int contest_id) throws Exception
		{
		String contest_title = null;

		PreparedStatement select_contest_title = sql_connection.prepareStatement("select title from contest where id = ?");
		select_contest_title.setInt(1, contest_id);
		ResultSet result_set = select_contest_title.executeQuery();

		if (result_set.next()) contest_title = result_set.getString(1);

		return contest_title;
		}

//------------------------------------------------------------------------------------

	// GET CONTEST PRIZE POOL

	public double get_contest_prize_pool(int contest_id) throws Exception
		{
		double total_prize_pool = 0;

		PreparedStatement get_contest_prize_pool = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ?");
		get_contest_prize_pool.setInt(1, contest_id);
		ResultSet result_set = get_contest_prize_pool.executeQuery();

		if (result_set.next()) total_prize_pool = result_set.getDouble(1);

		return total_prize_pool;
		}

//------------------------------------------------------------------------------------

	// GET NUMBER OF USERS IN CONTEST

	public int get_contest_current_users(int contest_id) throws Exception
		{
		int current_users = 0;

		PreparedStatement get_contest_current_users = sql_connection.prepareStatement("select count(distinct(user_id)) from entry where contest_id = ?");
		get_contest_current_users.setInt(1, contest_id);
		ResultSet result_set = get_contest_current_users.executeQuery();

		if (result_set.next()) current_users = result_set.getInt(1);

		return current_users;
		}

//------------------------------------------------------------------------------------

	// GET WAGER TOTAL FOR PARI-MUTUEL OPTION

	public double get_option_wager_total(int contest_id, int option_id) throws Exception
		{
		double option_wager_total = 0;

		PreparedStatement get_option_wager_total = sql_connection.prepareStatement("select sum(amount) from entry where contest_id = ? and entry_data = ?");
		get_option_wager_total.setInt(1, contest_id);
		get_option_wager_total.setInt(2, option_id);
		ResultSet result_set = get_option_wager_total.executeQuery();

		if (result_set.next()) option_wager_total = result_set.getDouble(1);

		return option_wager_total;
		}

//------------------------------------------------------------------------------------
	
	// SELECT CONTEST
	
	public JSONObject select_contest(int contest_id) throws Exception
		{
		JSONObject contest = null;

		PreparedStatement select_user = sql_connection.prepareStatement("select * from contest where id = ?");
		select_user.setInt(1, contest_id);
		ResultSet result_set = select_user.executeQuery();

		if (result_set.next())
			{
			contest = new JSONObject();
			
			//int contest_id = result_set.getInt(1);
			Long created = result_set.getLong(2);
			String created_by = result_set.getString(3);
			String category = result_set.getString(4);
			String sub_category = result_set.getString(5);
			String contest_type = result_set.getString(6);
			String title = result_set.getString(7);
			String description = result_set.getString(8);
			String settlement_type = result_set.getString(9);
			String pay_table = result_set.getString(10);
			String option_table = result_set.getString(11);
			double rake = result_set.getDouble(12);
			double salary_cap = result_set.getDouble(13);
			double cost_per_entry = result_set.getDouble(14);
			int min_users = result_set.getInt(15);
			int max_users = result_set.getInt(16);
			int entries_per_user = result_set.getInt(17);
			Long registration_deadline = result_set.getLong(18);
			int status = result_set.getInt(19);
			int roster_size = result_set.getInt(20);
			String odds_source = result_set.getString(21);
			String settled_by = result_set.getString(22);
			Long settled = result_set.getLong(23);
			String score_header = result_set.getString(24);
			
			contest.put("contest_id", contest_id);
			contest.put("created", created);
			contest.put("created_by", created_by);
			contest.put("category", category);
			contest.put("sub_category", sub_category);
			contest.put("contest_type", contest_type);
			contest.put("title", title);
			contest.put("description", description);
			contest.put("settlement_type", settlement_type);
			contest.put("pay_table", pay_table);
			contest.put("option_table", option_table);
			contest.put("rake", rake);
			contest.put("salary_cap", salary_cap);
			contest.put("cost_per_entry", cost_per_entry);
			contest.put("min_users", min_users);
			contest.put("max_users", max_users);
			contest.put("entries_per_user", entries_per_user);
			contest.put("registration_deadline", registration_deadline);
			contest.put("status", status);
			contest.put("roster_size", roster_size);
			contest.put("odds_source", odds_source);
			contest.put("settled_by", settled_by);
			contest.put("settled", settled);
			contest.put("score_header", score_header);
			}

		return contest;
		}

//------------------------------------------------------------------------------------

	// SELECT CONTEST ENTRY 

	public JSONObject select_entry(int entry_id) throws Exception
		{
		JSONObject entry = null;
		
		PreparedStatement select_user = sql_connection.prepareStatement("select * from entry where id = ?");
		select_user.setInt(1, entry_id);
		ResultSet result_set = select_user.executeQuery();

		if (result_set.next())
			{
			int contest_id = result_set.getInt(2);
			String user_id = result_set.getString(3);
			Long created = result_set.getLong(4);
			double amount = result_set.getDouble(5);
			String entry_data = result_set.getString(6);

			entry = new JSONObject();
			
			entry.put("entry_id", entry_id);
			entry.put("contest_id", contest_id);
			entry.put("user_id", user_id);
			entry.put("created", created);
			entry.put("amount", amount);
			entry.put("entry_data", entry_data);
			}

		return entry;
		}
//------------------------------------------------------------------------------------

	// SELECT CONTEST ENTRIES (WITH/WITHOUT USER ID)

	public JSONArray select_contest_entries(int contest_id) throws Exception
		{
		JSONArray entries = new JSONArray();
		
		PreparedStatement select_user = sql_connection.prepareStatement("select * from entry where contest_id = ?");
		select_user.setInt(1, contest_id);
		ResultSet result_set = select_user.executeQuery();

		while (result_set.next())
			{
			JSONObject entry = new JSONObject();
			
			int entry_id = result_set.getInt(1);
			String user_id = result_set.getString(3);
			Long created = result_set.getLong(4);
			double amount = result_set.getDouble(5);
			String entry_data = result_set.getString(6);

			entry.put("entry_id", entry_id);
			entry.put("contest_id", contest_id);
			entry.put("user_id", user_id);
			entry.put("created", created);
			entry.put("amount", amount);
			entry.put("entry_data", entry_data);
			
			entries.put(entry);
			}

		return entries;
		}

	public JSONArray select_contest_entries(int contest_id, String user_id) throws Exception
		{
		JSONArray entries = new JSONArray();
		
		PreparedStatement select_user = sql_connection.prepareStatement("select * from entry where contest_id = ? and user_id = ?");
		select_user.setInt(1, contest_id);
		select_user.setString(2, user_id);
		ResultSet result_set = select_user.executeQuery();

		while (result_set.next())
			{
			JSONObject entry = new JSONObject();
			
			int entry_id = result_set.getInt(1);
			Long created = result_set.getLong(4);
			double amount = result_set.getDouble(5);
			String entry_data = result_set.getString(6);

			entry.put("entry_id", entry_id);
			entry.put("contest_id", contest_id);
			entry.put("user_id", user_id);
			entry.put("created", created);
			entry.put("amount", amount);
			entry.put("entry_data", entry_data);
			
			entries.put(entry);
			}

		return entries;
		}

//------------------------------------------------------------------------------------

	// CHECK IF USER HAS ENTERED CONTEST

	public boolean has_user_entered_contest(int contest_id, String user_id) throws Exception
		{
		boolean result = false;

		PreparedStatement has_user_entered_contest = sql_connection.prepareStatement("select count(*) from entry where contest_id = ? and user_id = ?");
		has_user_entered_contest.setInt(1, contest_id);
		has_user_entered_contest.setString(2, user_id);
		ResultSet result_set = has_user_entered_contest.executeQuery();

		result_set.next();
		
		if (result_set.getInt(1) != 0) result = true;

		return result;
		}

//------------------------------------------------------------------------------------
	
	// SELECT REFERRAL

	public String[] select_referral(String referral_key) throws Exception
		{
		String[] referral = null;

		PreparedStatement select_referral = sql_connection.prepareStatement("select * from referral where referral_key = ?");
		select_referral.setString(1, referral_key);
		ResultSet result_set = select_referral.executeQuery();

		if (result_set.next())
			{
			String 
			
			referrer_id = result_set.getString(2),
			referrer_username = result_set.getString(3),
			email_address = result_set.getString(4),
			referral_program = result_set.getString(5),
			created = result_set.getString(6);
			
			referral = new String[]
				{
				referrer_id,
				referrer_username,
				email_address,
				referral_program,
				created
				};
			}

		return referral;
		}
	
//------------------------------------------------------------------------------------

	// SELECT PASSWORD RESET RECORD

	public String[] select_password_reset(String reset_key) throws Exception
		{
		String[] password_reset = null;

		PreparedStatement select_password_reset = sql_connection.prepareStatement("select * from password_reset where reset_key = ?");
		select_password_reset.setString(1, reset_key);
		ResultSet result_set = select_password_reset.executeQuery();

		if (result_set.next())
			{
			String 
			
			user_id = result_set.getString(2),
			created = result_set.getString(3);
			
			password_reset = new String[]
				{
				reset_key,
				user_id,
				created
				};
			}

		return password_reset;
		}

//------------------------------------------------------------------------------------
	
	// STORE VERIFIED EMAIL ADDRESS

	public void send_verification_email(String username, String email_address, String email_ver_key) throws Exception
		{
		String
		
		subject = "Verify your e-mail address",
		message_body = "Please <a href='" + Server.host + "/verify.html?" + email_ver_key + "'>click here</a> to verify your e-mail address.";

		Server.send_mail(email_address, username, subject, message_body);
		}

//------------------------------------------------------------------------------------
	
	// STORE VERIFIED EMAIL ADDRESS

	public void store_verified_email(String user_id, String email_address) throws Exception
		{
		PreparedStatement store_verified_email = sql_connection.prepareStatement("insert ignore into verified_email(user_id, email_address, created) values(?, ?, ?)");				
		store_verified_email.setString(1, user_id);				
		store_verified_email.setString(2, email_address);
		store_verified_email.setLong(3, System.currentTimeMillis());
		store_verified_email.executeUpdate();
		}

//------------------------------------------------------------------------------------
	
	// UPDATE BTC BALANCE
	
	public void update_btc_balance(String user_id, double new_btc_balance) throws Exception
		{
		PreparedStatement update_btc_balance = sql_connection.prepareStatement("update user set btc_balance = ? where id = ?");
		update_btc_balance.setDouble(1, new_btc_balance);
		update_btc_balance.setString(2, user_id);
		update_btc_balance.executeUpdate();
		}
	
//------------------------------------------------------------------------------------

	// UPDATE RC BALANCE
	
	public void update_rc_balance(String user_id, double new_rc_balance) throws Exception
		{
		PreparedStatement update_rc_balance = sql_connection.prepareStatement("update user set rc_balance = ? where id = ?");
		update_rc_balance.setDouble(1, new_rc_balance);
		update_rc_balance.setString(2, user_id);
		update_rc_balance.executeUpdate();
		}
	
//------------------------------------------------------------------------------------

	// UPDATE ROSTER SCORE / PAYOUT
	
	public void update_roster_score(int entry_id, double score) throws Exception
		{
		PreparedStatement update_entry = sql_connection.prepareStatement("update entry set score = ? where id = ?");
		update_entry.setDouble(1, score);
		update_entry.setInt(2, entry_id);
		update_entry.executeUpdate();
		}
	
	public void update_roster_payout(int entry_id, double payout) throws Exception
		{
		PreparedStatement update_entry = sql_connection.prepareStatement("update entry set payout = ? where id = ?");
		update_entry.setDouble(1, payout);
		update_entry.setInt(2, entry_id);
		update_entry.executeUpdate();
		}
	}
