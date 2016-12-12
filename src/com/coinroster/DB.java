package com.coinroster;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;

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
			}

		return user;
		}

//------------------------------------------------------------------------------------
	
	// SELECT TRANSACTION

	public String[] select_transaction(int transaction_id) throws Exception
		{
		String[] transaction = null;

		PreparedStatement select_transaction  = sql_connection.prepareStatement("select * from transaction where id = ?");
		select_transaction.setInt(1, transaction_id);

		ResultSet result_set = select_transaction.executeQuery();

		if (result_set.next())
			{
			String 
			
			id = result_set.getString(1),
			created = result_set.getString(2),
			created_by = result_set.getString(3),
			trans_type = result_set.getString(4),
			from_account = result_set.getString(5),
			to_account = result_set.getString(6),
			amount = result_set.getString(7),
			from_currency = result_set.getString(8),
			to_currency = result_set.getString(9),
			memo = result_set.getString(10),
			pending_flag = result_set.getString(11),
			ext_address = result_set.getString(12);

			transaction = new String[]
				{
				id, // 0
				created, // 1
				created_by, // 2
				trans_type, // 3
				from_account, // 4
				to_account, // 5
				amount, // 6
				from_currency, // 7
				to_currency, // 8
				memo, // 9
				pending_flag, // 10
				ext_address // 11
			 	};
			}

		return transaction;
		}

//------------------------------------------------------------------------------------

	// SELECT POOL
	
	public JSONObject select_pool(int pool_id) throws Exception
		{
		JSONObject pool = null;

		PreparedStatement select_user = sql_connection.prepareStatement("select * from pool where id = ?");
		select_user.setInt(1, pool_id);
		ResultSet result_set = select_user.executeQuery();

		if (result_set.next())
			{
			pool = new JSONObject();
			
			//int pool_id = result_set.getInt(1);
			Long created = result_set.getLong(2);
			String created_by = result_set.getString(3);
			String category = result_set.getString(4);
			String sub_category = result_set.getString(5);
			String title = result_set.getString(6);
			String description = result_set.getString(7);
			String settlement_type = result_set.getString(8);
			String pay_table = result_set.getString(9);
			String odds_table = result_set.getString(10);
			double rake = result_set.getDouble(11);
			double salary_cap = result_set.getDouble(12);
			double cost_per_entry = result_set.getDouble(13);
			int min_users = result_set.getInt(14);
			int max_users = result_set.getInt(15);
			int entries_per_user = result_set.getInt(16);
			Long registration_deadline = result_set.getLong(17);
			int status = result_set.getInt(18);
			int roster_size = result_set.getInt(19);
			String odds_source = result_set.getString(20);
			
			pool.put("pool_id", pool_id);
			pool.put("created", created);
			pool.put("created_by", created_by);
			pool.put("category", category);
			pool.put("sub_category", sub_category);
			pool.put("title", title);
			pool.put("description", description);
			pool.put("settlement_type", settlement_type);
			pool.put("pay_table", pay_table);
			pool.put("odds_table", odds_table);
			pool.put("rake", rake);
			pool.put("salary_cap", salary_cap);
			pool.put("cost_per_entry", cost_per_entry);
			pool.put("min_users", min_users);
			pool.put("max_users", max_users);
			pool.put("entries_per_user", entries_per_user);
			pool.put("registration_deadline", registration_deadline);
			pool.put("status", status);
			pool.put("roster_size", roster_size);
			pool.put("odds_source", odds_source);
			}

		return pool;
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

	public void store_verified_email(String email_address) throws Exception
		{
		PreparedStatement store_verified_email = sql_connection.prepareStatement("insert ignore into verified_email(email_address, created) values(?, ?)");				
		store_verified_email.setString(1, email_address);
		store_verified_email.setLong(2, System.currentTimeMillis());
		store_verified_email.executeUpdate();
		}

//------------------------------------------------------------------------------------
		
	}
