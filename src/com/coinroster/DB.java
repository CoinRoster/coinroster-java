package com.coinroster;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.internal.UserMail;


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
			double referral_program = result_set.getDouble(14);
			String referrer = result_set.getString(15);
			int ext_address_secure_flag = result_set.getInt(16);
			int free_play = result_set.getInt(17);
			Long last_active = result_set.getLong(18);
			int contest_status = result_set.getInt(19);
			String currency = result_set.getString(20);
			double referral_offer = result_set.getDouble(21);
			String promo_code = result_set.getString(22);
			int withdrawal_locked = result_set.getInt(23);
			double rollover_quota = result_set.getDouble(24);
			double rollover_progress = result_set.getDouble(25);
			double first_deposit = result_set.getDouble(26);
			int deposit_bonus_claimed = result_set.getInt(27);
			double deposit_bonus_cap = result_set.getDouble(28);
			int deposit_bonus_rollover_multiple = result_set.getInt(29);
			String odds_format = result_set.getString(30);
			String cgs_address = result_set.getString(31);
			double cgs_last_balance = result_set.getDouble(32);
			String referrer_key = result_set.getString(33);
			
			if (ext_address == null) ext_address = "";
			if (cgs_address == null) cgs_address = "";
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
			user.put("last_active", last_active);
			user.put("contest_status", contest_status);
			user.put("currency", currency);
			user.put("referral_offer", referral_offer);
			user.put("promo_code", promo_code);
			user.put("withdrawal_locked", withdrawal_locked);
			user.put("rollover_quota", rollover_quota);
			user.put("rollover_progress", rollover_progress);
			user.put("first_deposit", first_deposit);
			user.put("deposit_bonus_claimed", deposit_bonus_claimed);
			user.put("deposit_bonus_cap", deposit_bonus_cap);
			user.put("deposit_bonus_rollover_multiple", deposit_bonus_rollover_multiple);
			user.put("odds_format", odds_format);
			user.put("cgs_address", cgs_address);
			user.put("cgs_last_balance", cgs_last_balance);
			user.put("referrer_key", referrer_key);
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

	
	// GET FANTASY POINTS FROM PLAYER TABLE
	public double get_fantasy_points(int player_id, String sub_category) throws Exception
	{
		
	double points = 0;
	PreparedStatement get_points = sql_connection.prepareStatement("select points from player where id = ? and sport_type = ?");
	get_points.setInt(1, player_id);
	get_points.setString(2, sub_category);
	
	ResultSet result_set = get_points.executeQuery();

	if (result_set.next()) points = result_set.getDouble(1);
	
	return points;
	}

//------------------------------------------------------------------------------------

	
	// CHECK IF CONTESTS ARE IN PLAY

	public JSONObject check_if_in_play(String category, String sub_category, String contest_type) throws Exception
	{

		JSONObject contests = new JSONObject();
		PreparedStatement get_live_contests = sql_connection.prepareStatement("select id, scoring_rules from contest where category = ? and sub_category = ? and contest_type = ? and status=2");
		get_live_contests.setString(1, category);
		get_live_contests.setString(2, sub_category);
		get_live_contests.setString(3, contest_type);
	
		ResultSet result_set = get_live_contests.executeQuery();
		
		while (result_set.next())
			contests.put(String.valueOf(result_set.getInt(1)), result_set.getString(2));

		return contests;
	}
	
//------------------------------------------------------------------------------------
	
	//get ids from voting table
	public ArrayList<Integer> check_if_in_play() throws Exception
	{
		ArrayList<Integer> contest_ids = new ArrayList<Integer>();
		PreparedStatement get_live_contests = sql_connection.prepareStatement("select id from voting where status = 1");

		ResultSet result_set = get_live_contests.executeQuery();
		
		while (result_set.next()){
			contest_ids.add(result_set.getInt(1));
		}
		return contest_ids;
		
	}
//------------------------------------------------------------------------------------

	// GET PARI-MUTUELS IN PLAY IF AUTO-SETTLE=1
	public JSONObject get_pari_mutuel_id(String sub_category, String contest_type) throws Exception
	{
		JSONObject contests = new JSONObject();
		PreparedStatement get_live_contests = sql_connection.prepareStatement("select id, scoring_rules from contest where sub_category = ? and contest_type = ? and auto_settle=1 and status=2");
		get_live_contests.setString(1, sub_category);
		get_live_contests.setString(2, contest_type);
	
		ResultSet result_set = get_live_contests.executeQuery();
		
		while (result_set.next())
			contests.put(String.valueOf(result_set.getInt(1)), result_set.getString(2));
		
		return contests;
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
			String scoring_rules = result_set.getString(12);
			double rake = result_set.getDouble(13);
			double salary_cap = result_set.getDouble(14);
			double cost_per_entry = result_set.getDouble(15);
			int min_users = result_set.getInt(16);
			int max_users = result_set.getInt(17);
			int entries_per_user = result_set.getInt(18);
			Long registration_deadline = result_set.getLong(19);
			int status = result_set.getInt(20);
			int roster_size = result_set.getInt(21);
			String odds_source = result_set.getString(22);
			String settled_by = result_set.getString(23);
			Long settled = result_set.getLong(24);
			String score_header = result_set.getString(25);
			Long scores_updated = result_set.getLong(26);
			String scoring_scheme = result_set.getString(27);
			String progressive = result_set.getString(28);
			double progressive_paid = result_set.getDouble(29);
			Long settlement_deadline = result_set.getLong(32);
			
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
			contest.put("scoring_rules", scoring_rules);
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
			contest.put("scores_updated", scores_updated);
			contest.put("scoring_scheme", scoring_scheme);
			contest.put("progressive", progressive);
			contest.put("progressive_paid", progressive_paid);
			contest.put("settlement_deadline", settlement_deadline);
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

	public JSONObject select_referral(String referral_key) throws Exception
		{
		JSONObject referral = null;

		PreparedStatement select_referral = sql_connection.prepareStatement("select * from referral where referral_key = ?");
		select_referral.setString(1, referral_key);
		ResultSet result_set = select_referral.executeQuery();

		if (result_set.next())
			{
			// String referral_key = result_set.getString(1);
			String referrer_id = result_set.getString(2);
			String referrer_username = result_set.getString(3);
			String email_address = result_set.getString(4);
			double referral_program = result_set.getDouble(5);
			Long created = result_set.getLong(6);
			String promo_code = result_set.getString(7);
			
			referral = new JSONObject();

			referral.put("referral_key", referral_key);
			referral.put("referrer_id", referrer_id);
			referral.put("referrer_username", referrer_username);
			referral.put("email_address", email_address);
			referral.put("referral_program", referral_program);
			referral.put("created", created);
			referral.put("promo_code", promo_code);
			}

		return referral;
		}

//------------------------------------------------------------------------------------
	
	// GET REFERRER'S ID FROM KEY

	public String get_referrer_id_for_key(String referrer_key) throws Exception
		{
		String id = null;
		
		PreparedStatement select_id = sql_connection.prepareStatement("select id from user where referrer_key = ?");
		select_id.setString(1, referrer_key);
		ResultSet result_set = select_id.executeQuery();

		if (result_set.next()) id = result_set.getString(1);
		
		return id;
		}

//------------------------------------------------------------------------------------
	
	// GET UNUSED REFERRER KEY

	public String get_new_referrer_key() throws Exception
		{
		String referrer_key = Server.generate_key("referrer_key").substring(0, 8);
		
		if (get_referrer_id_for_key(referrer_key) != null) referrer_key = get_new_referrer_key();

		return referrer_key;
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
	
	// SEND EMAIL VERIFICATION

	public void send_verification_email(String username, String email_address, String email_ver_key) throws Exception
		{
		String
		
		subject = "Verify your e-mail address",

		message_body = "Welcome to CoinRoster <b>" + username + "</b>!";
		message_body += "<br/>";
		message_body += "<br/>";
		message_body += "Please <a href='" + Server.host + "/verify.html?" + email_ver_key + "'>click here</a> to verify your e-mail address.";

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

	// CHECK IF EMAIL IS IN USE

	public boolean email_in_use(String email_address) throws Exception
		{
		PreparedStatement check_email_address = sql_connection.prepareStatement("select * from verified_email where email_address = ?");
		check_email_address.setString(1, email_address);
		ResultSet result_set = check_email_address.executeQuery();
		
		if (result_set.next()) return true;
		
		return false;
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

	// CHECK IF CONTEST IS VOTING ROUND
	
	public boolean is_voting_contest(int contest_id) throws Exception
		{
		PreparedStatement check_voting = sql_connection.prepareStatement("select original_contest_id from voting where id = ?");
		check_voting.setInt(1, contest_id);
		
		ResultSet voting_rs = check_voting.executeQuery();
		if(voting_rs.next()) return true;
		return false;
		}

//------------------------------------------------------------------------------------
	
	// UPDATE CGS BALANCE
	
	public void update_cgs_balance(String user_id, double new_btc_balance) throws Exception
		{
		PreparedStatement update_btc_balance = sql_connection.prepareStatement("update user set cgs_last_balance = ? where id = ?");
		update_btc_balance.setDouble(1, new_btc_balance);
		update_btc_balance.setString(2, user_id);
		update_btc_balance.executeUpdate();
		}

//------------------------------------------------------------------------------------
	// UPDATE CGS BALANCE
	
	public void update_cgs_address(String user_id, String new_cgs_address) throws Exception
		{
		PreparedStatement update_btc_balance = sql_connection.prepareStatement("update user set cgs_address = ? where id = ?");
		update_btc_balance.setString(1, new_cgs_address);
		update_btc_balance.setString(2, user_id);
		update_btc_balance.executeUpdate();
		}

//------------------------------------------------------------------------------------
	
	// RESERVE AND RETURN CGS ADDRESS (CREATE USER)
	
	public String reserve_cgs_address(String username) throws Exception
		{
		String cgs_address = null;
		
		PreparedStatement get_free_cgs_address = sql_connection.prepareStatement("select btc_address from cgs where cr_account is null limit 1");
		ResultSet result_set = get_free_cgs_address.executeQuery();
		
		if (result_set.next()) 
			{
			cgs_address = result_set.getString(1);

			PreparedStatement reserve_cgs_address = sql_connection.prepareStatement("update cgs set cr_account = ? where btc_address = ?");
			reserve_cgs_address.setString(1, username);
			reserve_cgs_address.setString(2, cgs_address);
			reserve_cgs_address.executeUpdate();
			}
		
		Server.log("Assigning CGS address: " + cgs_address);
		
		return cgs_address;
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
	
//------------------------------------------------------------------------------------

	// SELECT FX RECORD
	
	public JSONObject select_currency(String symbol) throws Exception
		{
		JSONObject currency = null;

		PreparedStatement select_currency = sql_connection.prepareStatement("select * from fx where symbol = ?");
		select_currency.setString(1, symbol);
		ResultSet result_set = select_currency.executeQuery();

		if (result_set.next()) 
			{
			//String symbol = result_set.getString(1);
			String source = result_set.getString(2);
			double last_price = result_set.getDouble(3);
			long last_updated = result_set.getLong(4);
			String description = result_set.getString(5);
			
			currency = new JSONObject();
			
			currency.put("symbol", symbol);
			currency.put("source", source);
			currency.put("last_price", last_price);
			currency.put("last_updated", last_updated);
			currency.put("description", description);
			}

		return currency;
		}
	
	// SELECT JUST THE LAST PRICE
	
	public double get_last_price(String symbol) throws SQLException
		{
		double last_price = 0;

		PreparedStatement get_last_price = sql_connection.prepareStatement("select last_price from fx where symbol = ?");
		get_last_price.setString(1, symbol);
		ResultSet result_set = get_last_price.executeQuery();

		if (result_set.next()) last_price = result_set.getDouble(1);

		return last_price;
		}

	// SELECT JUST THE DESCRIPTION
	
	public String get_currency_description(String symbol) throws SQLException
		{
		String description = null;
	
		PreparedStatement get_currency_description = sql_connection.prepareStatement("select description from fx where symbol = ?");
		get_currency_description.setString(1, symbol);
		ResultSet result_set = get_currency_description.executeQuery();
	
		if (result_set.next()) description = result_set.getString(1);
	
		return description;
		}
	
//------------------------------------------------------------------------------------

	// UPDATE FX RECORD
	
	public void update_fx(String symbol, double last_price, String source, String description) throws Exception
		{
		PreparedStatement update_fx = sql_connection.prepareStatement("replace into fx(symbol, source, last_price, last_updated, description) values(?, ?, ?, ?, ?)");
		update_fx.setString(1, symbol);
		update_fx.setString(2, source);
		update_fx.setDouble(3, last_price);
		update_fx.setLong(4, System.currentTimeMillis());
		update_fx.setString(5, description);
		update_fx.executeUpdate();
		}
	
//------------------------------------------------------------------------------------

	// SELECT PROMO RECORD
	
	public JSONObject select_promo(Object selector) throws Exception
		{
		JSONObject promo = null;
		
		PreparedStatement select_promo = null;
				
		if (selector instanceof String) // selecting by promo code
			{
			String promo_code = (String) selector;
			select_promo = sql_connection.prepareStatement("select * from promo where promo_code = ?");
			select_promo.setString(1, promo_code);
			}
		else // selecting by promo ID
			{
			int promo_id = (int) selector;
			select_promo = sql_connection.prepareStatement("select * from promo where id = ?");
			select_promo.setInt(1, promo_id);
			}

		ResultSet result_set = select_promo.executeQuery();

		if (result_set.next()) 
			{
			int id = result_set.getInt(1);
			long created = result_set.getLong(2);
			long expires = result_set.getLong(3);
			long cancelled = result_set.getLong(4);
			String approved_by = result_set.getString(5);
			String cancelled_by = result_set.getString(6);
			String promo_code = result_set.getString(7);
			String description = result_set.getString(8);
			String referrer = result_set.getString(9);
			double free_play_amount = result_set.getDouble(10);
			int rollover_multiple = result_set.getInt(11);
			String cancelled_reason = result_set.getString(12);
			int max_use = result_set.getInt(13);
			int times_used = result_set.getInt(14);
			
			promo = new JSONObject();
			
			promo.put("id", id);
			promo.put("created", created);
			promo.put("expires", expires);
			promo.put("cancelled", cancelled);
			promo.put("approved_by", approved_by);
			promo.put("cancelled_by", cancelled_by);
			promo.put("promo_code", promo_code);
			promo.put("description", description);
			promo.put("referrer", referrer);
			promo.put("free_play_amount", free_play_amount);
			promo.put("rollover_multiple", rollover_multiple);
			promo.put("cancelled_reason", cancelled_reason);
			promo.put("max_use", max_use);
			promo.put("times_used", times_used);
			}

		return promo;
		}

//------------------------------------------------------------------------------------

	// SELECT PROMO REQUEST
	
	public JSONObject select_promo_request(int request_id) throws Exception
		{
		JSONObject promo_request = null;

		PreparedStatement select_promo_request = sql_connection.prepareStatement("select * from promo_request where id = ?");
		select_promo_request.setInt(1, request_id);
		ResultSet result_set = select_promo_request.executeQuery();

		if (result_set.next()) 
			{
			int id = result_set.getInt(1);
			long created = result_set.getLong(2);
			String created_by = result_set.getString(3);
			String requested_code = result_set.getString(4);
			int approved = result_set.getInt(5);
			int denied = result_set.getInt(6);
			String denied_reason = result_set.getString(7);
			String denied_by = result_set.getString(8);
			
			promo_request = new JSONObject();
			
			promo_request.put("id", id);
			promo_request.put("created", created);
			promo_request.put("created_by", created_by);
			promo_request.put("requested_code", requested_code);
			promo_request.put("approved", approved);
			promo_request.put("denied", denied);
			promo_request.put("denied_reason", denied_reason);
			promo_request.put("denied_by", denied_by);
			}

		return promo_request;
		}

//------------------------------------------------------------------------------------

	// CREDIT / DEBIT ROLLOVER
	
	public void credit_user_rollover_progress(JSONObject user, double wager_amount) throws Exception
		{
		String user_id = user.getString("user_id");
		
		double 
		
		rollover_progress = user.getDouble("rollover_progress"),
		rollover_quota = user.getDouble("rollover_quota");
		
		rollover_progress = Utils.add(rollover_progress, wager_amount, 0);
		
		if (rollover_progress < rollover_quota) // user is still locked
			{
			PreparedStatement update_rollover_progress = sql_connection.prepareStatement("update user set rollover_progress = ? where id = ?");
			update_rollover_progress.setDouble(1, rollover_progress);
			update_rollover_progress.setString(2, user_id);
			update_rollover_progress.executeUpdate();
			}
		else // user has met their rollover quota - unlock
			{
			PreparedStatement update_rollover_progress = sql_connection.prepareStatement("update user set rollover_progress = ?, withdrawal_locked = 0 where id = ?");
			update_rollover_progress.setDouble(1, rollover_progress);
			update_rollover_progress.setString(2, user_id);
			update_rollover_progress.executeUpdate();
			
			String
			
			subject = "You have met your Free Play playing requirement!",
			message_body = "";
			
			message_body += "Congratulations <b><!--USERNAME--></b>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "You have met your Free Play playing requirement!";
			message_body += "<br/>";
			message_body += "<br/>";
			message_body += "You are now free to withdraw funds from your account.";
			
			new UserMail(user, subject, message_body);
			}
		}

	public void debit_user_rollover_progress(JSONObject user, double amount) throws Exception
		{
		String user_id = user.getString("user_id");
		
		double rollover_progress = user.getDouble("rollover_progress");
		rollover_progress = Utils.subtract(rollover_progress, amount, 0);

		PreparedStatement update_rollover_progress = sql_connection.prepareStatement("update user set rollover_progress = ? where id = ?");
		update_rollover_progress.setDouble(1, rollover_progress);
		update_rollover_progress.setString(2, user_id);
		update_rollover_progress.executeUpdate();
		}

//------------------------------------------------------------------------------------

	// ACTIVATE DEPOSIT MATCH (IF APPLICABLE)
	
	public boolean enable_deposit_bonus(JSONObject user, double deposit_amount) throws Exception
		{
		double first_deposit = user.getDouble("first_deposit");
		
		if (first_deposit > 0) return false;

		String user_id = user.getString("user_id");
		
		PreparedStatement update_user = sql_connection.prepareStatement("update user set first_deposit = ? where id = ?");
		update_user.setDouble(1, deposit_amount);
		update_user.setString(2, user_id);
		update_user.executeUpdate();

		return true;
		}

//------------------------------------------------------------------------------------

	public JSONObject select_progressive(Object selector) throws Exception
		{
		JSONObject progressive = null;
		
		PreparedStatement select_progressive = null;
		
		if (selector instanceof String) // selecting by code
			{
			String code = (String) selector;
			select_progressive = sql_connection.prepareStatement("select * from progressive where code = ?");
			select_progressive.setString(1, code);
			}
		else // selecting by ID
			{
			int id = (int) selector;
			select_progressive = sql_connection.prepareStatement("select * from progressive where id = ?");
			select_progressive.setInt(1, id);
			}

		ResultSet result_set = select_progressive.executeQuery();

		if (result_set.next()) 
			{
			int id = result_set.getInt(1);
			long created = result_set.getLong(2);
			String created_by = result_set.getString(3);
			String category = result_set.getString(4);
			String sub_category = result_set.getString(5);
			String code = result_set.getString(6);
			String payout_info = result_set.getString(7);
			double balance = result_set.getDouble(8);
			
			progressive = new JSONObject();
			
			progressive.put("id", id);
			progressive.put("created", created);
			progressive.put("created_by", created_by);
			progressive.put("category", category);
			progressive.put("sub_category", sub_category);
			progressive.put("code", code);
			progressive.put("payout_info", payout_info);
			progressive.put("balance", balance);
			}

		return progressive;
		}

//------------------------------------------------------------------------------------

	// GET CONTEST PRIZE POOL

	public double get_miner_fee() throws Exception
		{
		double miner_fee = 0;

		PreparedStatement get_contest_prize_pool = sql_connection.prepareStatement("select value from control where name='miner_fee'");
		ResultSet result_set = get_contest_prize_pool.executeQuery();

		if (result_set.next()) miner_fee = Double.parseDouble(result_set.getString(1));

		// convert satoshis to btc:
		
		miner_fee = Utils.satoshi_to_btc(miner_fee);
		
		return miner_fee;
		}
	
//------------------------------------------------------------------------------------

	// GET CONTEST PRIZE POOL

	public double get_cold_storage_balance() throws Exception
		{
		double cold_storage_balance = 0;

		PreparedStatement get_cold_storage_balance = sql_connection.prepareStatement("select value from control where name='cold_storage_balance'");
		ResultSet result_set = get_cold_storage_balance.executeQuery();

		if (result_set.next()) cold_storage_balance = Double.parseDouble(result_set.getString(1));
		
		return cold_storage_balance;
		}
	
//------------------------------------------------------------------------------------
	
	public double get_withdrawal_fee() throws Exception
		{
		double withdrawal_fee = 0;
	
		PreparedStatement get_contest_prize_pool = sql_connection.prepareStatement("select value from control where name='withdrawal_fee'");
		ResultSet result_set = get_contest_prize_pool.executeQuery();
	
		if (result_set.next()) withdrawal_fee = Double.parseDouble(result_set.getString(1));
	
		// convert satoshis to btc:
		
		withdrawal_fee = Utils.satoshi_to_btc(withdrawal_fee);
		
		return withdrawal_fee;
		}

//------------------------------------------------------------------------------------

	public ArrayList<String> getAllGameIDsDB(String sport) throws SQLException{
		ResultSet result_set = null;
		ArrayList<String> gameIDs = new ArrayList<String>();
		try {
			PreparedStatement get_games = sql_connection.prepareStatement("select distinct gameID from player where sport_type=?");
			get_games.setString(1, sport);
			result_set = get_games.executeQuery();		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		while(result_set.next()){
			gameIDs.add(result_set.getString(1));	
		}
		return gameIDs;
	}
	
//------------------------------------------------------------------------------------

	public ResultSet getAllPlayerIDs(String sport){
		ResultSet result_set = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id from player where sport_type=?");
			get_players.setString(1, sport);
			result_set = get_players.executeQuery();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return result_set;
	}
	
	//------------------------------------------------------------------------------------

	public ResultSet getOptionTable(String sport, boolean filtered, int filter) throws SQLException{
		
		ResultSet result_set = null;
		
		if(filtered){
			try{
				PreparedStatement get_players = sql_connection.prepareStatement("SELECT a.id, a.name, a.team_abr, a.salary, a.filter_on FROM player AS a "
						+ "WHERE (SELECT COUNT(*) FROM player AS b "
						+ "WHERE b.team_abr = a.team_abr AND b.filter_on >= a.filter_on) <= ? "
						+ "AND a.sport_type = ?"
						+ "ORDER BY a.team_abr ASC, a.filter_on DESC");
				get_players.setInt(1, filter);
				get_players.setString(2, sport);
				result_set = get_players.executeQuery();
			}
			catch(Exception e){
				Utils.log(e.toString());
			}
		}
		else{
			try {
				PreparedStatement get_players = sql_connection.prepareStatement("SELECT id, name, team_abr, salary from player where sport_type = ?");
				get_players.setString(1, sport);
				result_set = get_players.executeQuery();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		return result_set;
	}
	
	
//------------------------------------------------------------------------------------
	
	public ResultSet getPlayerScores(String sport) throws SQLException{
		ResultSet result_set = null;
		try {
			PreparedStatement get_players = sql_connection.prepareStatement("select id, data from player where sport_type=?");
			get_players.setString(1, sport);
			result_set = get_players.executeQuery();		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result_set;
	}

//------------------------------------------------------------------------------------

	public void editData(String data, int id, String sport) throws SQLException{
		PreparedStatement update_points = sql_connection.prepareStatement("update player set data = ? where id = ? and sport_type = ?");
		update_points.setString(1, data);
		update_points.setInt(2, id);
		update_points.setString(3, sport);
		update_points.executeUpdate();
	}

//------------------------------------------------------------------------------------

	public int get_original_contest(Integer contest_id) {
		int original_contest_id = 0;
		try {
			PreparedStatement get_original_contest = sql_connection.prepareStatement("select original_contest_id from voting where id = ?");
			get_original_contest.setInt(1, contest_id);
			ResultSet result_set = get_original_contest.executeQuery();		
			if (result_set.next()) original_contest_id = result_set.getInt(1);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return original_contest_id;
	}
	
//------------------------------------------------------------------------------------

	public JSONArray getRosterTemplates(String sub_category) throws SQLException{
		
		Utils.log("reading contests templates for " + sub_category + " contests");
		JSONArray templates = new JSONArray();
		ResultSet result_set = null;
		try {
			PreparedStatement get_contests = sql_connection.prepareStatement("select * from contest_template where sub_category = ? and active = 1");
			get_contests.setString(1, sub_category);
			result_set = get_contests.executeQuery();	
		}
		catch(Exception e){
			e.printStackTrace();
		}	

		while (result_set.next()){
			JSONObject entry = new JSONObject();
			try{
				entry.put("category", result_set.getString(2));
				entry.put("sub_category", result_set.getString(3));
				entry.put("contest_type", result_set.getString(4));
				entry.put("title", result_set.getString(5));
				entry.put("description", result_set.getString(6));
				entry.put("settlement_type", result_set.getString(7));
				entry.put("pay_table", result_set.getString(8));
				entry.put("rake", result_set.getFloat(9));
				entry.put("salary_cap", result_set.getInt(10));
				entry.put("cost_per_entry", result_set.getDouble(11));
				entry.put("min_users", result_set.getInt(12));
				entry.put("max_users", result_set.getInt(13));
				entry.put("entries_per_user", result_set.getInt(14));
				entry.put("roster_size", result_set.getInt(15));
				entry.put("multi_stat", result_set.getBoolean(16));
				entry.put("prop_type", result_set.getObject(17));
				entry.put("scoring_rules", result_set.getObject(18));
				entry.put("score_header", result_set.getString(19));
				
				if(result_set.getObject(20) == null)
					entry.put("progressive", "");
				else
					entry.put("progressive", result_set.getObject(20));
				
				if(result_set.getObject(22) == null)
					entry.put("filter", 0);
				else
					entry.put("filter", result_set.getInt(22));
				
				templates.put(entry);
			} catch (Exception e) {
				Utils.log(e.getMessage());
				Utils.log(e.getLocalizedMessage());
				Utils.log(e.toString());
			}
		}
		return templates;
	}


//------------------------------------------------------------------------------------

	public double get_voting_contest_commission() {
		double voting_contest_creator_commission = 0;
		try {
			PreparedStatement voting_contest_commission = sql_connection.prepareStatement("select value from control where name='voting_contest_creator_commission'");
			ResultSet result_set = voting_contest_commission.executeQuery();		
			if (result_set.next()) voting_contest_creator_commission = result_set.getInt(1);
		}
		catch(Exception e){
			Utils.log(e.toString());
			e.printStackTrace(System.out);
		}
		Utils.log("creator commission: " + voting_contest_creator_commission);
		return voting_contest_creator_commission;
	}
	
//------------------------------------------------------------------------------------
	public ArrayList<Integer> get_pending_voting_contests() {
		ArrayList<Integer> voting_contests = new ArrayList<>();
		try {
			PreparedStatement pending_voting_contests = sql_connection.prepareStatement("select id from voting where status = '5'");
			ResultSet result_set = pending_voting_contests.executeQuery();		
			while (result_set.next()) voting_contests.add(result_set.getInt(1));
		}
		catch(Exception e){
			Utils.log(e.toString());
			e.printStackTrace(System.out);
		}
		return voting_contests;
	}
	
//------------------------------------------------------------------------------------
	

}

