package com.coinroster;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

public class DB 
	{
	static Connection sql_connection;
	static Session session;
	
	public DB (MethodInstance method)
		{
		sql_connection = method.sql_connection;
		session = method.session;
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
	
	public String[] select_user(String column, String value) throws Exception
		{
		String[] user = null;

		PreparedStatement select_user = sql_connection.prepareStatement("select * from user where " + column + " = ?");
		select_user.setString(1, value);
		ResultSet result_set = select_user.executeQuery();

		if (result_set.next())
			{
			String 
			
			user_id = result_set.getString(1),
			username = result_set.getString(2),
			stored_password_hash = result_set.getString(3),
			user_level = result_set.getString(4),
			created = result_set.getString(5),
			last_login = result_set.getString(6);
			
			user = new String[]
				{
				user_id,
				username,
				stored_password_hash,
				user_level,
				created,
				last_login
				};
			}

		return user;
		}

//------------------------------------------------------------------------------------
	
	// SELECT USER XREF

	public String[] select_user_xref(String column, String value) throws Exception
		{
		String[] user_xref = null;

		PreparedStatement select_user_xref = sql_connection.prepareStatement("select * from user_xref where " + column + " = ?");
		select_user_xref.setString(1, value);
		ResultSet result_set = select_user_xref.executeQuery();
  
		if (result_set.next())
			{
			String 
			
			user_id = result_set.getString(1),
			btc_balance = result_set.getString(2),
			rc_balance = result_set.getString(3),
			ext_address = result_set.getString(4),
			email_address = result_set.getString(5),
			email_ver_key = result_set.getString(6),
			email_ver_flag = result_set.getString(7),
			newsletter_flag = result_set.getString(8),
			referral_program = result_set.getString(9),
			referrer = result_set.getString(10);

			user_xref = new String[]
				{
				user_id,
				btc_balance,
				rc_balance,
				ext_address,
				email_address,
				email_ver_key,
				email_ver_flag,
				newsletter_flag,
				referral_program,
				referrer
				};
			}

		return user_xref;
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

	// CREATE SESSION

	public String create_session(String username, String user_id, String user_level) throws Exception
		{
		if (session.active()) Server.kill_session(session.token());

		String new_session_token = Server.generate_key(user_id);
		
		Long now = System.currentTimeMillis();

		Server.session_map.put
			(
			new_session_token, 
			new String[]
				{
				username,
				user_id,
				user_level,
				Long.toString(now)
				}
			);

		// update last login:
		
		update_last_login(now, user_id);
		
		return new_session_token;
		}
	
//------------------------------------------------------------------------------------

	// UPDATE LAST LOGIN
	
	public void update_last_login(Long now, String user_id) throws SQLException
		{
		PreparedStatement update_last_login = sql_connection.prepareStatement("update user set last_login = ? where id = ?");
		update_last_login.setLong(1, now);
		update_last_login.setString(2, user_id);
		update_last_login.executeUpdate();
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
