package com.coinroster;

public class Session 
	{
	private String 
	
	session_token = null,
	username = null,
	user_id = null,
	user_level = null;

	protected Session(String token)
		{
		session_token = token;
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