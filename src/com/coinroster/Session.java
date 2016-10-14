package com.coinroster;

public class Session 
	{
	private String session_token = null;

	protected Session(String token)
		{
		session_token = token;
		}

	public boolean active()
		{
		if (session_token == null) return false;
		else if (Server.session_map.containsKey(session_token)) return true;
		else return false;
		}
	
	public String username()
		{
		return Server.session_map.get(session_token)[0];
		}
	
	public String user_id()
		{
		return Server.session_map.get(session_token)[1];
		}

	public String user_level()
		{
		return Server.session_map.get(session_token)[2];
		}
	
	public String token()
		{
		return session_token;
		}
	}