package com.coinroster;

public class ReferralRedirect extends Utils
	{
	protected ReferralRedirect(HttpRequest request, HttpResponse response) throws Exception
		{
		String referrer_key = request.target_object();
		
		if (referrer_key != null && referrer_key.length() == 8) response.set_cookie("referrer_key", referrer_key);
		
		response.redirect("/");
		}
	}