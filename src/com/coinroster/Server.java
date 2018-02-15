package com.coinroster;

/*

To-do:

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
import java.util.Map;
import java.util.Random;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.MessageDigest;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class Server extends Utils
	{
	protected static String 
	
	java_path = "/usr/share/java/",
	java_out_path = "",
	java_object_path = "",
	jar_filename = "coinroster.jar",

	config_path = java_path + "coinroster.config.txt",
			
	version = "1.4 - finished pari-mutuels",
	start_time = new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()),

	sql_database = "jdbc:mysql://localhost:3306/coinroster",
	sql_username,
	sql_password,

	relay_email_from,
	relay_email_address,
	relay_email_password,

	host_dev = "https://165.227.40.220",
	host_live = "https://www.coinroster.com",
	
	developer_name = "Noah",
	developer_email_address = "noah@frontierweb.com",

	cipher_name = "AES/CBC/PKCS5PADDING",
	init_vector = "AAAAAAAAAAAAAAAA";

	public static String 
	
	html_path = "/usr/share/nginx/html/coinroster.com",
	
	host;

	static int
	
	port = 27038,
	
	free_address_quota = 20,
	
	pool_size,
	seconds_to_halt = 2;
	
	public static Long
	
	second = (long) 1000,
	minute = second * 60,
	hour = minute * 60,
	day = hour * 24,
	
	admin_timeout,
	
	lobby_settled_cutoff = 30 * day;

	static boolean 
	
	log_file_active = false,
	live_server = false,
	dev_server = false,
	listening = true;

	static HashSet<String> 

	guest_user_methods = new HashSet<String>(),
	standard_user_methods = new HashSet<String>(),
	admin_user_methods = new HashSet<String>(),
	score_bot_methods = new HashSet<String>();
	
	private static ServerSocket gateway;
	
	protected static Map<String, String> config_map = new TreeMap<String, String>();
	
	public static ConcurrentHashMap<String, String[]> session_map = new ConcurrentHashMap<String, String[]>();
	
	public static HashMap<String, String> control = new HashMap<String, String>();

	static ExecutorService
	
	cron_pool,
	worker_pool;
	
	public static ExecutorService async_updater;
	
	private static ComboPooledDataSource sql_pool;

	public static void kill_session(String session_token) 
		{
		session_map.remove(session_token);
		}
	
	public static Connection sql_connection() throws SQLException 
		{
		return sql_pool.getConnection();
		}

    public static String sql_dump_string() throws SQLException 
    	{
    	return "mysqldump --force --opt -u" + sql_username + " -p" + sql_password + " ";
    	}
    
	public static void main (String[] args) throws Exception
		{
		cache_and_rerun();

		check_method_security();
	
		set_system_out();

		load_control_variables();

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
						case "score_bot":
							admin_user_methods.add(method_name);
							score_bot_methods.add(method_name);
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
			List<String> config = Server.read(config_path);
			
			for (String line : config)
				{
				if (line.contains("::"))
					{
					String[] config_item = line.split("::");
					
					if (config_item.length == 2)
						{
						config_map.put(config_item[0], config_item[1]);
						continue;
						}
					}
				
				Server.log("Config ERROR: " + line);
				}
			
			String server_which = config_map.get("server");
			
			if (server_which.equals("live"))
				{
				live_server = true;
				
				pool_size = 20;
				
				admin_timeout = 60 * minute;
				
				host = host_live;
				relay_email_from = "CoinRoster";
				}
			else if (server_which.equals("dev"))
				{
				dev_server = true;
				
				pool_size = 3;
		
				admin_timeout = 1440 * minute;
	
				host = host_dev;
				relay_email_from = "CoinRoster Dev";
				}
			
			sql_username = config_map.get("sql_username");
			sql_password = config_map.get("sql_password");
			
			relay_email_address = config_map.get("relay_email_address");
			relay_email_password = config_map.get("relay_email_password");
			
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

	// set up thread and database pools
	
	private static void initialize_pools()
		{
		worker_pool = Executors.newFixedThreadPool(pool_size);
		cron_pool = Executors.newFixedThreadPool(3);
		async_updater = Executors.newFixedThreadPool(2);
		
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
		log("open_proxy_gateway method is called");
		try {
			gateway = new ServerSocket(port);
			
			log("Proxy Gateway Online!");

			CronDriver.start();
			
			while (listening)
				{
				try {
					new ServerThread(gateway.accept()).start();
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
			log("Port " + port + " is taken");
			} 
		catch (Exception e) 
			{
			exception(e);
			}	
		finally 
			{
			try {gateway.close();} 
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
			
			gateway.close();
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
	
	public static void exception(Exception e)
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

	public static void send_mail(String to_address, final String to_user, final String subject, String message_body)
		{
		final String 
		
		logo_cid = generate_key("logo_cid"),
		domain_directory = Server.html_path,
		factory_path = domain_directory + "/factory/";
		
		try {
			String email_template = Utils.read_to_string(factory_path + "email_template.html");
			email_template = email_template.replace("logo_cid", logo_cid);
			message_body = email_template.replace("<!-- factory:message_body -->", message_body);
			}
		catch (Exception e)
			{
			exception(e);
			}

		final String message_body_final = message_body;
		
		// if running on development server, redirect all email to developer:
		
		//if (dev_server) to_address = developer_email_address;
		
		//if (dev_server) return;
		
		final String to_address_final = to_address;
		
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
					// set up message
					
					Message msg = new MimeMessage(session);
					msg.setFrom(new InternetAddress(relay_email_address, relay_email_from));
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to_address_final, to_user));
					msg.setSubject(subject);
					
					// add message body
					
					MimeMultipart multipart = new MimeMultipart("related");
			        BodyPart part = new MimeBodyPart();
			        part.setContent(message_body_final, "text/html");
			        multipart.addBodyPart(part);
			        
			        // add logo to email
			        
					part = new MimeBodyPart();
					FileInputStream in = new FileInputStream(new File(domain_directory + "/img/CoinRoster.png"));
		            DataSource fds = new ByteArrayDataSource(inputstream_to_bytearray(in), "image/gif");
		            part.setDataHandler(new DataHandler(fds));
		            part.setHeader("Content-Disposition","inline");
		            part.setHeader("Content-ID","<" + logo_cid + ">");
		            multipart.addBodyPart(part);
		            in.close();
		            
		            // set message content and send
		            
		            msg.setContent(multipart);
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