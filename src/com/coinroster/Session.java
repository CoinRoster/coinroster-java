package com.coinroster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
public class Session 
	{
	private String 
	
	session_token = null,
	username = null,
	user_id = null,
	user_level = null;

	protected Session(String session_token)
		{
		this.session_token = session_token;
		}

	public String create_session(Connection sql_connection, Session session, String username, String user_id, int user_level) throws Exception
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
				Integer.toString(user_level),
				Long.toString(now)
				}
			);
	
		// update last login:
		
		PreparedStatement update_last_login = sql_connection.prepareStatement("update user set last_login = ? where id = ?");
		update_last_login.setLong(1, now);
		update_last_login.setString(2, user_id);
		update_last_login.executeUpdate();
		
		return new_session_token;
		}

	public void update_user_level(String match_user_id, int user_level) 
		{
		for (String session_token : Server.session_map.keySet()) 
			{
			String[] session_vars = Server.session_map.get(session_token);
			
			String
			
			username = session_vars[0],
			user_id = session_vars[1];
			
			if (user_id.equals(match_user_id))
				{
				Server.session_map.put
					(
					session_token, 
					new String[]
						{
						username,
						user_id,
						Integer.toString(user_level),
						Long.toString(System.currentTimeMillis())
						}
					);
				}
			}
		}
	
	public boolean active()
		{
		if (session_token == null) return false;
		else if (Server.session_map.containsKey(session_token)) 
			{
			username = Server.session_map.get(session_token)[0];
			user_id = Server.session_map.get(session_token)[1];
			user_level = Server.session_map.get(session_token)[2];
			Server.session_map.put
				(
				session_token, 
				new String[]
					{
					username,
					user_id,
					user_level,
					Long.toString(System.currentTimeMillis())
					}
				);
			new Thread() 
				{
				public void run() 
					{
					Connection sql_connection = null;
					try {
						sql_connection = Server.sql_connection();
						PreparedStatement update_last_active = sql_connection.prepareStatement("update user set last_active = ? where id = ?");
						update_last_active.setLong(1, System.currentTimeMillis());
						update_last_active.setString(2, user_id);
						update_last_active.executeUpdate();
						} 
					catch (Exception e) 
						{
						Server.exception(e);
						} 
					finally
						{
						if (sql_connection != null)
							{
							try {sql_connection.close();} 
							catch (SQLException ignore) {}
							}
						}
					}
				}.start();
			return true;
			}
		else return false;
		}
	
	public String username()
		{
		return username;
		}
	
	public String user_id()
		{
		return user_id;
		}

	public String user_level()
		{
		return user_level;
		}
	
	public String token()
		{
		return session_token;
		}
	}