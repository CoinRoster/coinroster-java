package com.coinroster;

/* Development Log

Phase 1

Date		Hours	Change
	
21/10/2015	6		Set up stack, started working on session handling, created login method, cleaned up JSON-based method calling
22/10/2015	6		Finished session handling, finished method calling - added restricted and unrestricted calls, added logout, cleaned up API handler and API js
23/10/2015	5		Installed MySQL, created a database, created a user table, installed JDBC connector, wrote a Java program that can add a user to the table, converted login to SQL query, created select_user function, added user account creation
26/10/2015	2		Users can now change passwords, added admin-level calls, code cleanup
28/10/2015	5		Created admin and account panel directories with a proxy-pass for Java session auth, discovered NGINX ssi, created call for session details, created ssi header with contingent nav for admin and account panels, created automatic login prompt if not authenticated for the panels; debugged and fixed duplicate cookie issue;
29/10/2015	5		Created another panel for user profile; put all functionality to login or logout into the SSI header; started working on header UI (centering container, logo placeholder); converted login and signup from raw inputs to form; Created directory for lobby; landing page is now feature-less and auto-forwards to lobby if logged in
30/10/2015	2		Browsed iStockPhoto for carousel photos; created directories for login and signup mainly for aesthetic reasons ("/login" instead of "/login.html"); made lobby a protected directory; added user icon;
02/11/2015	2		Created full-screen carousel for landing page
03/11/2015	2		Merged profile and account into account; fixed bug in carousel; created horizontally and vertically centered area on landing page; created "C" logo;
04/11/2015	1		Finalized logo: C made from inner and outer circle with one line through it, like a cent (will be the icon for roster coins - like cents to the Bitcoin's B dollar sign);
05/11/2015	3		Created a much more polished landing page; started looking for good carousel photos
06/11/2015	5		Created proper sign-up page; ran into lots of form, input, and redirect issues; got rid of separate login page; cleaned up some of the ssi includes;
09/11/2015	6		Found a few more slides; compressed slide jpgs; added image post-loading to carousel; changed to simple background for protected panes; started wire-framing account pane (balance section, toggling elements, buttons, etc); added messaging to change-password interface;
10/11/2015	3		More work on account pane: wrote a javascript function that makes it easy to create subwindows with toggle buttons; worked on page styling; added deposit / withdraw buttons; mapped out account table and transaction table; created "Contests" pane
11/11/2015	1		A few structural changes to account pane;
13/12/2015	3		Set up PC at home for work; finalized tables for user xref and transaction; created tables; figured out how to commit multiple PreparedStatements in one call: account creation now creates a user_xref row as well;
16/12/2015	5		Users can now add an email address, change their email address, request a re-send of the verification email, subscribe/unsubscribe from the newsletter; BTC and RC balances now come from the database; dummy methods in place for sending verification and referral emails;
27/12/2015	4		Figured out how to relay mail through Google Apps; completed email verification; simplified "manage email" subwindow on account page;
28/12/2015	6		Added mime-type to panel gateway; admin panel: brought over pane switching from ASI, created user report with sorting, created transaction report pane / settings, created new transaction pane; created internal accounts with level = 2; completed affiliate referral signup pathway
29/12/2015	2		Re-wrote referral signup pathway so that email address is automatically populated and verified for new user; all verified emails are now stashed in a table;
30/12/2015	2		Updated DNS for coinroster.com; purchased and installed SSL certificate: the site is now live over HTTPS on coinroster.com;
31/12/2015	1		Added command gateway and halt procedure; strengthened method call validation;
02/01/2016	4		Admin can now create four transactions: BTC-DEPOSIT, BTC-WITHDRAWAL, RC-DEPOSIT, RC-WITHDRAWAL, with client- and server- side validation of all fields; transactions are written to transaction table, all necessary balances are updated;
03/01/2016	2		Figured out table locking for SQL transactions; fixed some bugs in transaction logic
04/01/2016	8		Completed transaction reports in Admin Panel and User Account Pane; did an audit of all Java methods; added link to blog on landing page; added username/password character length messages to sign-up page; got management@coinroster.com set up for mail relay; transaction notification email; auto-log-off of admin sessions
05/01/2016	4		Created production server on CoinRoster's DigitalOcean account; migrated everything; the site is live; changed rewrite on coinroster.nlphd.com for dev work; fixed bug in referral; forked into production / dev versions (mainly email at this point);
06/01/2016	4		Added control table to SQL and parser in Java; production flag is now set based on control table variable ("0" on dev server, "1" on live server); now checking at startup to make sure callable API methods are secured; added pending referrals report;
08/01/2016	1		Added simple User Report; added Last Login;
11/01/2016	6		Added encrypted password management to admin panel;
21/01/2016	5		Added proper restart; moved all implementation-specific variables to top of server class; session_map now persits across runs; added password reset;
22/01/2016	1		Referrer is emailed when referral converts;
25/01/2016	2		Code review of api_handler; corrected security flaw where key to password blob was same as key in database table;
26/01/2016	5		Changed username char limit to 40; added loop for purging expired password_reset keys; added infinite expiriy for session_token; researched and implemented proper variable scoping for java; reserached and implemented SQL connection pooling; researched and implemented thread pool;	
27/01/2016	6		Rewrote serversockets completely: now using one port + thread pool for everything; created server_worker; now properly cleaning up sockets; fixed a lot of bugs; rewrote NGINX config to be much simpler; researched / rewrote NGINX host rewriting; added 404 to static_handler (old panel_gateway); cleaned up HTML directories; 
28/01/2016	6		Debugging JVM crash due to SEGV_ACCERR at java.util.zip.ZipFile.getEntry while trying to load a class URL; added "version" call to command_handler; added firewall to Ubuntu; redirecting all output to out.txt, added as main arg[0]; changed restart call to addShutdownHook; added "clear log" to command_handler; 
29/01/2016	5		Major rewrite and re-org of SSI/CS/JS to take advantage of caching and to make everything more normal under the hood; SEO improvements (Google is rejecting the site); submitted coinroster to Google, verified ownership; misc debugging / research related to c3p0 and JVM

Total:		136

Phase 2

06/10/2016	4		Incorporated latest developments from other Java projects (cache and rerun, command handler upgrades, crons, saving and loading of assets); started converting API into package with classes rather than a single class with methods - for scalability; misc refactoring;
09/10/2016	6		Finished converting API, created DB class with generic DB access methods (like select_user), created new template, created new process for controlling method level, simplified MethodCall;
13/10/2016	2		Added HttpsOnly and Secure flags to session_token, verified that these settings have taken effect; re-enabled account creation, improved messaging on sign-up page; cleaned up guest pages and authentication Javascript (root.html, signup.html, verify.html, forgot.html, reset.html)

*/

import java.net.ServerSocket;
import java.net.BindException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.MessageDigest;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Server extends Utils
	{
	protected static String 

	java_path = "/usr/share/java/",
	java_out_path,
	java_object_path,
	jar_filename = "coinroster.jar",
	
	version = "1.2 - api break-out",
	start_time = new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()),

	sql_database = "jdbc:mysql://localhost:3306/coinroster",
	sql_username = "coinrostersql",
	sql_password = "crsqlpass",

	relay_email_from,
	relay_email_address,
	relay_email_password,

	host_dev = "https://coinroster.nlphd.com",
	host_live = "https://www.coinroster.com",
	
	developer_name = "Noah",
	developer_email_address = "noah@nlphd.com",

	cipher_name = "AES/CBC/PKCS5PADDING",
	init_vector = "AAAAAAAAAAAAAAAA";

	public static String host;

	static int
	
	proxy_port = 27038,
	
	pool_size,
	seconds_to_halt = 2,
	admin_timeout;
	
	public static int
	
	second = 1000,
	minute = second * 60,
	hour = minute * 60,
	day = hour * 24;

	static boolean 
	
	log_file_active = false,
	live_server = false,
	dev_server = false,
	listening = true;

	static HashSet<String> 

	guest_user_methods = new HashSet<String>(),
	standard_user_methods = new HashSet<String>(),
	admin_user_methods = new HashSet<String>();
	
	private static ServerSocket proxy_gateway;

	static ConcurrentHashMap<String, String[]> session_map = new ConcurrentHashMap<String, String[]>();
	
	static HashMap<String, String> control = new HashMap<String, String>();

	private static ExecutorService worker_pool;
	
	private static ComboPooledDataSource sql_pool;

	public static void kill_session(String session_token) 
		{
		session_map.remove(session_token);
		}
	
	static Connection sql_connection() throws SQLException 
		{
		return sql_pool.getConnection();
		}

	public static void main (String[] args) throws Exception
		{
		cache_and_rerun();

		check_method_security();
	
		set_system_out();

		load_control_variables();

		set_global_variables();

		initialize_pools();
		
		load_session_map();
		
		open_proxy_gateway();
		}

//------------------------------------------------------------------------------------

	// cache and rerun server (if not yet done):
	
	static void cache_and_rerun()
		{
		try {
			String jar_path = Server.class.getProtectionDomain().getCodeSource().getLocation().toString();
			
			if (!jar_path.contains("/cache."))
				{
				log("Copying to cache...");
				
				Path 
				
				read_path = Paths.get(java_path + jar_filename),
				write_path = Paths.get(java_path + "cache." + jar_filename);
				
				Files.copy(read_path, write_path, StandardCopyOption.REPLACE_EXISTING);
				
				Runtime.getRuntime().addShutdownHook(new Thread()
					{
					public void run()
						{
						try {
							run_jar("cache." + jar_filename);
							} 
						catch (Exception e) {}
						}
					});

				System.exit(0);
				}
			else 
				{
				log("Application is running from cache");
				log("---");
				}
			} 
		catch (Exception e) 
			{
			exception(e);
			}
		}

//------------------------------------------------------------------------------------

	// make sure all methods are secured
	
	private static void check_method_security()
		{
		try {
			List<Class<?>> api = ClassFinder.find("com.coinroster.api");
			
			for (int i=0; i<api.size(); i++)
				{
				Class<?> method_class = api.get(i);
				
				String 
				
				class_name = method_class.getName(),
				method_name = class_name.substring(class_name.lastIndexOf(".") + 1, class_name.length());

				if (method_name.equals("_")) continue;
	
				try	{
					switch ((String) method_class.getDeclaredField("method_level").get(null))
						{
						case "guest":
							guest_user_methods.add(method_name);
							break;
						case "standard":
							standard_user_methods.add(method_name);
							break;
						case "admin":
							admin_user_methods.add(method_name);
							break;
						}
					}
				catch (Exception e) 
					{
					log("Method level has not been set for " + method_name);
					}
				}
			}
		catch (Exception e){exception(e);}
		}

//------------------------------------------------------------------------------------

	// process input args, if applicable:
	
	static void set_system_out()
		{
		String jar_name = jar_filename.substring(0, jar_filename.indexOf(".jar"));
		
		java_out_path = java_path + jar_name + ".txt";
		java_object_path = java_path + jar_name.replaceAll("\\.","_") + "/";
		
		// check / create java object directory
	
		File java_object_directory = new File(java_object_path);
		
		if (!java_object_directory.exists()) 
			{
			try	{
				java_object_directory.mkdir();
				} 
			catch (Exception e)
				{
				exception(e);
				}		
			}
	
		try {
			System.setOut(new PrintStream(new FileOutputStream(java_out_path, true)));
			log_file_active = true;
			} 
		catch (Exception ignore) {}
		}

//------------------------------------------------------------------------------------

	// load control variables
	
	private static void load_control_variables()
		{
		try {
			Connection sql_connection = DriverManager.getConnection(sql_database, sql_username, sql_password);
			PreparedStatement prepared_statement = sql_connection.prepareStatement("select * from control");
			ResultSet result_set = prepared_statement.executeQuery();
			
			while (result_set.next())
				{
				String 
				
				name = result_set.getString(1),
				value = result_set.getString(2);
				
				control.put(name, value);
				}
			
			sql_connection.close();
			} 
		catch (Exception e) 
			{
			exception(e);
			Server.exit(false);
			}
		}

//------------------------------------------------------------------------------------

	// set global variables for dev / live
	
	private static void set_global_variables()
		{
		if (control.get("production").equals("1"))
			{
			live_server = true;
			
			pool_size = 20;
			
			admin_timeout = 10 * minute;
			
			host = "https://www.coinroster.com";
			relay_email_from = "CoinRoster";
			relay_email_address = "management@coinroster.com";
			relay_email_password = "CoinRoster123";
			}
		else if (control.get("production").equals("0"))
			{
			dev_server = true;
			
			pool_size = 20;
	
			admin_timeout = 60 * minute;

			host = "https://coinroster.nlphd.com";
			relay_email_from = "CoinRoster Dev";
			relay_email_address = "coinroster@gmail.com";
			relay_email_password = "CoinRoster123!";
			}
		else
			{
			log("No control variable found for 'production'");
			Server.exit(false);
			}
		}
	
//------------------------------------------------------------------------------------

	// set up thread and database pools
	
	private static void initialize_pools()
		{
		worker_pool = Executors.newFixedThreadPool(pool_size);
		sql_pool = new ComboPooledDataSource();
		try {
			sql_pool.setMinPoolSize(3);
			sql_pool.setMaxPoolSize(pool_size + 2);
			sql_pool.setAcquireIncrement(2);
			sql_pool.setMaxIdleTime(21600);
			sql_pool.setDriverClass("com.mysql.jdbc.Driver");
			sql_pool.setJdbcUrl(sql_database);
			sql_pool.setUser(sql_username);
			sql_pool.setPassword(sql_password);
			} 
		catch (PropertyVetoException e) 
			{
			exception(e);
			Server.exit(false);
			}
		}

//------------------------------------------------------------------------------------

	// load session map
	
	@SuppressWarnings("unchecked")
	private static void load_session_map()
		{
		Object session_map_object = load_from_disk("session_map", true);
		
		if (session_map_object != null) session_map = (ConcurrentHashMap<String, String[]>) session_map_object;
		}

//------------------------------------------------------------------------------------

	// proxy gateway loop
	
	static void open_proxy_gateway()
		{
		try {
			proxy_gateway = new ServerSocket(proxy_port);
			
			log("Proxy Gateway Online!");
			
			IterativeLoops.start();
			
			while (listening)
				{
				try {
					worker_pool.submit(new ServerWorker(proxy_gateway.accept()));
					}
				catch (SocketException e) 
					{
					if (listening) exception(e);
					}
				catch (Exception e) 
					{
					exception(e);
					}
				}
			}
		catch (BindException b)
			{
			log("Port " + proxy_port + " is taken");
			} 
		catch (Exception e) 
			{
			exception(e);
			}	
		finally 
			{
			try {proxy_gateway.close();} 
			catch (IOException ignore) {}
			}
		}
		
//------------------------------------------------------------------------------------

	// shutdown procedure

	static void pre_exit() throws Exception // to command_handler
		{
		try {
			log("---");
			
			log("Preparing to halt...");
			
			log("---");

			save_to_disk(session_map, "session_map", true);

			listening = false;

			while (seconds_to_halt > 0) 
				{
				seconds_to_halt--;
				Thread.sleep(second);
				}
			
			proxy_gateway.close();
			}
		catch (Exception e)
			{
			exception(e);
			}
		}
	
	static void exit(boolean restart)
		{
		if (restart)
			{
			log("System is restarting now!");
			log("---");
			
			Runtime.getRuntime().addShutdownHook(new Thread()
				{
				public void run()
					{
					try {
						run_jar(jar_filename);
						} 
					catch (Exception e) {}
					}
				});
			}
		else
			{
			log("System is halting now!");
			log("---");
			}
		
		System.exit(0);
		}
	
	static void exception(Exception e)
		{
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		log(errors.toString());
		}
	
//------------------------------------------------------------------------------------
	
	// security / encryption

	static Random generator = new Random();

	static byte[] SHA256(String data) 
		{
		byte[] digest = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data.getBytes("UTF-8"));
			digest = md.digest();
			} 
		catch (Exception e) 
			{
			exception(e);
			return null;
			}
		return digest;
		}
	
	public static String generate_key(String salt) 
		{
		String key = SHA1(System.currentTimeMillis() + generator.nextInt(999999999) + salt);
		return key;
		}

	public static byte[] encrypt(byte[] plain_text, String _key) throws Exception 
		{
		SecretKeySpec key = new SecretKeySpec(SHA256(_key), "AES");
		
		Cipher cipher = Cipher.getInstance(cipher_name);
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(init_vector.getBytes("UTF-8")));
		
		return cipher.doFinal(plain_text);
		}

	public static byte[] decrypt(byte[] cipher_text, String _key) throws Exception
	  	{
		SecretKeySpec key = new SecretKeySpec(SHA256(_key), "AES");
		
		Cipher cipher = Cipher.getInstance(cipher_name);
		cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(init_vector.getBytes("UTF-8")));
		
		return cipher.doFinal(cipher_text);
	  	}

//------------------------------------------------------------------------------------
	
	// send email

	public static void send_mail(String to_address, final String to_user, final String subject, final String message_body)
		{
		// if runnong on development server, redirect all email to developer:
		
		if (dev_server) to_address = developer_email_address;
		
		final String final_to_address = to_address;
		
		new Thread() 
			{
			public void run() 
				{
				Properties props = new Properties();
				
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "smtp.gmail.com");
				props.put("mail.smtp.port", "587");

				Session session = Session.getInstance(props, new javax.mail.Authenticator() 
					{
					protected PasswordAuthentication getPasswordAuthentication() 
						{
						return new PasswordAuthentication(relay_email_address, relay_email_password);
						}
					});
		
				try {
					Message msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(relay_email_address, relay_email_from));
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(final_to_address, to_user));
					msg.setSubject(subject);
					msg.setContent(message_body, "text/html");
					Transport.send(msg);
					} 
				catch (AddressException e) 
					{
					exception(e);
					} 
				catch (MessagingException e) 
					{
					exception(e);
					}
				catch (Exception e)
					{
					exception(e);
					}
				}
			}.start();
		}

//------------------------------------------------------------------------------------
	
	// utilities
	
	static void run_jar(String jar_filename) throws Exception
		{
		Runtime.getRuntime().exec("java -jar " + java_path + jar_filename);
		}

	static boolean save_to_disk(Object object, String filename, boolean encrypt)
		{
		try {
			ByteArrayOutputStream byte_array_output_stream = new ByteArrayOutputStream();
			
			ObjectOutputStream o_out = new ObjectOutputStream(byte_array_output_stream);
			o_out.writeObject(object);
			o_out.close();
			
			byte[] byte_array = null;
			
			if (encrypt) byte_array = encrypt(byte_array_output_stream.toByteArray(), sql_password);
			else byte_array = byte_array_output_stream.toByteArray();
	
			FileOutputStream f_stream = new FileOutputStream(java_object_path + filename);
			f_stream.write(byte_array);
			f_stream.close();
			
			return true;
			}
		catch (Exception e)
			{
			exception(e);
			}
		return false;
		}

	static Object load_from_disk(String filename, boolean decrypt)
		{
		String object_path = java_object_path + filename;
		log("Loading: " + object_path);
		try {
			if (new File(object_path).exists())
				{
				byte[] byte_array = inputstream_to_bytearray(new FileInputStream(object_path));
				
				if (decrypt) byte_array = decrypt(byte_array, sql_password);
	
				ObjectInputStream o_in = new ObjectInputStream(new ByteArrayInputStream(byte_array));
				Object object = o_in.readObject();
				o_in.close();

				new File(object_path).delete();
				
				log("Successfully loaded object: " + object_path);
				
				return object;
				}
			else log("File not found: " + object_path);
			}
		catch (Exception e) 
			{
			exception(e);
			}
		
		return null;
		}

//------------------------------------------------------------------------------------
	
	}