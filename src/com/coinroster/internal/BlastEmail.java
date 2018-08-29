package com.coinroster.internal;

import java.util.ArrayList;

import com.coinroster.Server;

public class BlastEmail {
	ArrayList<String> emails = new ArrayList<>();
	
	public BlastEmail() {
		emails.add("jamestest12@riskingtime.com");
		emails.add("jamestest11@riskingtime.com");
		emails.add("jamestest13@riskingtime.com");
		emails.add("jamestest15@riskingtime.com");
		emails.add("jamestest16@riskingtime.com");
		emails.add("jamestest18@riskingtime.com");
		emails.add("jamestest20@riskingtime.com");
		emails.add("jamestest21@riskingtime.com");
		emails.add("jamestest23@riskingtime.com");
		emails.add("jamestest24@riskingtime.com");
		emails.add("jamestest25@riskingtime.com");
		emails.add("jamestest26@riskingtime.com");
		emails.add("colef@rogers.com");
		emails.add("gov1@rostercoins.com");
		emails.add("gov2@rostercoins.com");
		emails.add("gov3@rostercoins.com");
		emails.add("gov4@rostercoins.com");
		emails.add("gov5@rostercoins.com");
		emails.add("gov6@rostercoins.com");
		emails.add("gov7@rostercoins.com");
		emails.add("gov8@rostercoins.com");
		emails.add("gov9@rostercoins.com");
		emails.add("gov10@rostercoins.com");
		emails.add("gov11@rostercoins.com");
		emails.add("gov12@rostercoins.com");
		emails.add("gov13@rostercoins.com");
		emails.add("gov14@rostercoins.com");
		emails.add("gov15@rostercoins.com");
		emails.add("gov16@rostercoins.com");
		emails.add("gov17@rostercoins.com");
		emails.add("gov18@rostercoins.com");
		emails.add("gov18@rostercoins.com");
		emails.add("gov19@rostercoins.com");
		emails.add("gov21@rostercoins.com");
		emails.add("gov31@rostercoins.com");
		emails.add("gov41@rostercoins.com");
		emails.add("gov51@rostercoins.com");
		emails.add("gov61@rostercoins.com");
		emails.add("gov71@rostercoins.com");
		emails.add("gov81@rostercoins.com");
		emails.add("gov91@rostercoins.com");
		emails.add("gov111@rostercoins.com");
		
		for(String e : emails) {
			Server.send_mail(e, "abc", "test", "HI");
		}
	}
}
