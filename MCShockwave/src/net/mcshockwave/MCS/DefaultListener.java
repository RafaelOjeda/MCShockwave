package net.mcshockwave.MCS;

import net.mcshockwave.MCS.SQLTable.Rank;
import net.mcshockwave.MCS.Challenges.Challenge.ChallengeType;
import net.mcshockwave.MCS.Challenges.ChallengeManager;
import net.mcshockwave.MCS.Commands.FriendCommand;
import net.mcshockwave.MCS.Commands.VanishCommand;
import net.mcshockwave.MCS.Commands.VoteCommand;
import net.mcshockwave.MCS.Currency.LevelUtils;
import net.mcshockwave.MCS.Currency.PointsUtils;
import net.mcshockwave.MCS.Menu.ItemMenu;
import net.mcshockwave.MCS.Menu.ItemMenu.Button;
import net.mcshockwave.MCS.Stats.Statistics;
import net.mcshockwave.MCS.Utils.FireworkLaunchUtils;
import net.mcshockwave.MCS.Utils.ItemMetaUtils;
import net.mcshockwave.MCS.Utils.LocUtils;
import net.mcshockwave.MCS.Utils.PacketUtils;
import net.mcshockwave.MCS.Utils.PacketUtils.PacketPlayOutHeaderFooter;
import net.mcshockwave.MCS.Utils.PacketUtils.ParticleEffect;
import net.mcshockwave.MCS.Utils.SchedulerUtils;
import net.minecraft.server.v1_7_R4.ChatSerializer;
import net.minecraft.server.v1_7_R4.PacketPlayOutChat;
import net.minecraft.util.org.apache.commons.lang3.text.WordUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultListener implements Listener {

	MCShockwave	plugin;

	public DefaultListener(MCShockwave instance) {
		plugin = instance;
	}

	String[]							cuss		= null;

	HashMap<Player, String>				lastMessage	= new HashMap<Player, String>();

	String								VIPLink		= "buy.mcshockwave.net";

	Random								rand		= new Random();

	public static ArrayList<TNTPrimed>	tntnoboom	= new ArrayList<>();

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player p = event.getEntity();

		if (p.getLastDamageCause() == null) {
			return;
		}

		DamageCause dc = p.getLastDamageCause().getCause();
		if (dc != DamageCause.CUSTOM && dc != DamageCause.SUICIDE) {
			Statistics.incrDeaths(p.getName());
			if (p.getKiller() != null) {
				Statistics.incrKills(p.getKiller().getName());
				ChallengeManager.incrChallenge(ChallengeType.Kills, null, null, p.getKiller().getName(), 1, false);
			}
		}
	}

	@EventHandler
	public void vote(InventoryClickEvent event) {
		if (event.getInventory().getName().equalsIgnoreCase(ChatColor.DARK_PURPLE + "Vote!")
				&& event.getWhoClicked() instanceof Player) {
			Player p = (Player) event.getWhoClicked();
			event.setCancelled(true);
			ItemStack it = event.getCurrentItem();
			if (it != null) {
				String nam = ItemMetaUtils.getItemName(it).replaceFirst(ChatColor.GOLD.toString(), "")
						.replaceAll(" ", "_");
				for (String n : VoteCommand.votes.keySet()) {
					if (n.equalsIgnoreCase(nam)) {
						int v = VoteCommand.votes.get(nam);
						VoteCommand.votes.put(nam, v + 1);
						Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has cast their vote!");
						VoteCommand.voters.add(p.getName());
						p.closeInventory();
					}
				}
			}
		}
	}

	HashMap<Player, Long>	cooldown	= new HashMap<>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player p = event.getPlayer();
		Action a = event.getAction();
		Block b = event.getClickedBlock();
		ItemStack it = p.getItemInHand();

		if (a == Action.RIGHT_CLICK_BLOCK
				&& event.getClickedBlock().getType() == Material.valueOf(SQLTable.Settings.get("Setting",
						"Scavenger_Material", "Value"))) {
			Location l = event.getClickedBlock().getLocation();
			String loc = l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
			String where = "Server='" + MCShockwave.server + "' AND Location='" + loc + "'";
			int id = SQLTable.Scavenger.getIntWhere(where, "ID");
			String players = SQLTable.Scavenger.getWhere(where, "PlayersFound");
			String clue = SQLTable.Scavenger.getWhere(where, "Clue");
			int amount = players.split(",").length - 1;
			if (players.contains(p.getName())) {
				p.sendMessage("§a" + amount + " " + (amount == 1 ? "person has" : "people have") + " found this clue!");
				p.sendMessage(ChatColor.BLUE + clue);
				return;
			}

			for (int i = 0; i < id; i++) {
				for (String s : SQLTable.Scavenger.getAll("ID", i + "", "PlayersFound")) {
					if (!s.contains(p.getName())) {
						return;
					}
				}
			}

			String place = (amount + 1) + "";
			if (place.endsWith("1")) {
				place += "st";
			} else if (place.endsWith("2")) {
				place += "nd";
			} else if (place.endsWith("3")) {
				place += "rd";
			} else {
				place += "th";
			}
			p.sendMessage(ChatColor.RED + "You found the next "
					+ WordUtils.capitalizeFully(b.getType().name().replace('_', ' ')) + "!\n§aYou were the " + place
					+ " person to find this clue\n" + ChatColor.AQUA + "Next clue:");
			p.sendMessage(ChatColor.BLUE + clue);

			SQLTable.Scavenger.set("PlayersFound",
					SQLTable.Scavenger.get("ID", id + "", "PlayersFound") + "," + p.getName(), "ID", id + "");
		}

		if (it.getType() == Material.BLAZE_ROD
				&& ItemMetaUtils.hasCustomName(it)
				&& ItemMetaUtils.getItemName(it).equalsIgnoreCase("Blax's Rod")
				&& (cooldown.containsKey(p) && cooldown.get(p) <= System.currentTimeMillis() || !cooldown
						.containsKey(p))) {
			cooldown.put(p, System.currentTimeMillis() + 1000);
			p.getWorld().playEffect(p.getLocation(), Effect.MOBSPAWNER_FLAMES, 4);
			if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
				for (Entity e : p.getNearbyEntities(8, 8, 8)) {
					if (e != p) {
						Location l = p.getLocation();
						Location l2 = e.getLocation();
						e.setVelocity(new Vector(l2.getX() - l.getX(), 0.6, l2.getZ() - l.getZ()).multiply(5 / l2
								.distance(l)));
					}
				}
				p.getWorld().playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1, 1);
			}
			if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
				for (Entity e : p.getNearbyEntities(10, 10, 10)) {
					if (e != p) {
						Location l = p.getLocation();
						Location l2 = e.getLocation();
						e.setVelocity(new Vector(l.getX() - l2.getX(), -0.4, l.getZ() - l2.getZ()).multiply(5 / l2
								.distance(l)));
					}
				}
				p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 1);
			}
		}

		if (it.getType() == Material.NETHER_STAR && ItemMetaUtils.hasCustomName(it)
				&& ItemMetaUtils.getItemName(it).equalsIgnoreCase("Mage Call") && a.name().contains("RIGHT_CLICK")) {
			event.setCancelled(true);

			ArrayList<Location> ci = LocUtils.circle(p, p.getLocation(), 6, 1, true, false, 5);
			final Location pl = p.getLocation();

			SchedulerUtils sc = SchedulerUtils.getNew();

			for (int i = 0; i < ci.size(); i++) {
				final Location l = ci.get(i);
				sc.add(new Runnable() {
					public void run() {
						FireworkLaunchUtils.playFirework(l, Color.YELLOW);
					}
				});
				if (i % 2 == 0) {
					sc.add(2);
				}
			}
			sc.add(8);
			for (int i = 0; i < 5; i++) {
				sc.add(new Runnable() {
					public void run() {
						pl.getWorld().strikeLightningEffect(pl);
					}
				});
				sc.add(3);
			}

			sc.execute();
		}
	}

	@EventHandler
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		final Player p = event.getPlayer();

		if (MCShockwave.chatSilenced && !SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
			p.sendMessage("§cChat is silenced!");
			event.setCancelled(true);
			return;
		}
		if (cuss == null) {
			cuss = SQLTable.Settings.get("Setting", "CussBlock", "Value").split(",");
		}
		String mes = event.getMessage();
		for (String m : cuss) {
			String star = "";
			for (int i = 0; i < m.length(); i++) {
				star += "*";
			}
			mes = mes.replaceAll("(?i)" + new StringBuffer(m).reverse().toString(), star);
		}
		event.setMessage(mes);
		if (!SQLTable.hasRank(p.getName(), Rank.JR_MOD) && lastMessage.containsKey(p)
				&& lastMessage.get(p).equalsIgnoreCase(event.getMessage())) {
			event.setCancelled(true);
			p.sendMessage(ChatColor.RED + "No spamming, please");
			for (Player p2 : Bukkit.getOnlinePlayers()) {
				if (p2.isOp()) {
					p2.sendMessage(ChatColor.GOLD + "Player " + p.getName() + " tried to spam. M: "
							+ event.getMessage());
				}
			}
		} else {
			lastMessage.remove(p);
			lastMessage.put(p, event.getMessage());
		}
		if (SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
			event.setMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
		}
		p.setDisplayName(p.getDisplayName().replace(
				ChatColor.RED + "[" + ChatColor.BOLD + "MUTED" + ChatColor.RESET + ChatColor.RED + "] "
						+ ChatColor.RESET, ""));
		if (SQLTable.Muted.has("Username", p.getName())) {
			if (SQLTable.Muted.getInt("Username", p.getName(), "Time") < TimeUnit.MILLISECONDS.toMinutes(System
					.currentTimeMillis())) {
				SQLTable.Muted.del("Username", p.getName());
			} else {
				p.setDisplayName(ChatColor.RED + "[" + ChatColor.BOLD + "MUTED" + ChatColor.RESET + ChatColor.RED
						+ "] " + ChatColor.RESET + p.getDisplayName());
				event.getRecipients().clear();
				event.getRecipients().add(p);
				for (Player p2 : Bukkit.getOnlinePlayers()) {
					if (SQLTable.hasRank(p2.getName(), Rank.JR_MOD)) {
						event.getRecipients().add(p2);
					}
				}
				p.sendMessage(ChatColor.RED + "You have been muted.");
			}
		}
		for (Player p2 : Bukkit.getOnlinePlayers()) {
			if (SQLTable.PrivateMutes.hasWhere("Muted", "Username='" + p2.getName() + "' AND Muted='" + p.getName()
					+ "'")) {
				event.getRecipients().remove(p2);
			}
		}
		Pattern pat = Pattern.compile("(%s|%n)", Pattern.CASE_INSENSITIVE);
		Matcher mat = pat.matcher(event.getMessage());
		if (mat.find()) {
			event.setCancelled(true);
			MCShockwave.send(p, "Refrain from abusing %s, please", "chat functions");
			return;
		}
		if (event.getMessage().startsWith("@")) {
			event.getRecipients().clear();
			String server = MCShockwave.server != null ? " §a[§l" + MCShockwave.server + "§a]§7" : "";
			String ames = p.getDisplayName() + server + "§f: §7" + mes.replaceFirst("@", "");
			if (!SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
				p.sendMessage(ames);
			}
			MCShockwave.sendMessageToRank(ames, Rank.JR_MOD);
		}
		String cscm = ChatColor.stripColor(event.getMessage());
		if (cscm.length() > 3 && cscm.toUpperCase().equals(cscm)) {
			event.setMessage(WordUtils.capitalizeFully(event.getMessage()));
		}
		if (!SQLTable.hasRank(p.getName(), Rank.JR_MOD) && (event.getMessage().toLowerCase().contains("hack"))) {
			event.setCancelled(true);
			p.sendMessage("Please obtain proof or alert a staff member using '@' before a message instead of calling out hacks!");
		}
		// if (event.isCancelled() || event.getMessage().startsWith("!"))
		// return;
		// if (MCShockwave.chatEnabled) {
		// for (Player rec : event.getRecipients()) {
		// rec.sendMessage(p.getDisplayName() + ChatColor.RESET + ": " +
		// event.getMessage());
		// }
		// }
		// event.getRecipients().clear();
		if (event.getMessage().contains("JSON")) {
			String json = "{\"text\":\"\",\"extra\":[{\"text\":\"§8[{{LVL}}§8]\",\"hoverEvent\":{\"action"
					+ "\":\"show_text\",\"value\":\"{{LVL.TEXT}}\"}},{\"text\":\""
					+ "{{DISPLAYNAME}}\",\"hoverEvent\":{\"action\":\"show_text\",\"value\""
					+ ":\"{{DISPLAYNAME.TEXT}}\"}},{\"text\":\": {{MESSAGE}}\"}]}";

			int lvl = LevelUtils.getLevelFromXP(LevelUtils.getXP(p));

			String[] form = { "LVL:" + LevelUtils.getSuffixColor(lvl) + "§l" + lvl,
					"LVL.TEXT:User: " + p.getName() + "\\n\\n§cLevel information here",
					"DISPLAYNAME:" + p.getDisplayName().substring(p.getDisplayName().indexOf(']', 2) + 1),
					"DISPLAYNAME.TEXT:User: " + p.getName() + "\\n\\n§cSome information here",
					"MESSAGE:" + event.getMessage() };
			for (String s : form) {
				String[] ss = s.split(":", 2);
				json = json.replace("{{" + ss[0] + "}}", ss[1]);
			}

			for (Player rec : event.getRecipients()) {
				PacketUtils.sendPacket(rec, new PacketPlayOutChat(ChatSerializer.a(json)));
			}

			event.setCancelled(true);
		} else {
			event.setFormat(p.getDisplayName() + ChatColor.RESET + ": " + event.getMessage());
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		String message = e.getMessage();
		String mlc = message.toLowerCase();
		Player p = e.getPlayer();
		String[] args = message.split(" ");
		String[] argslc = mlc.split(" ");
		argslc[0] = argslc[0].replaceFirst("/bukkit:", "/").replaceFirst("/minecraft:", "/");

		boolean showToAdmins = true;

		try { // MCShockwave.isOp() throws an exception
			if (argslc[0].equalsIgnoreCase("/op")
					&& (!SQLTable.hasRank(p.getName(), Rank.ADMIN) && !MCShockwave.isOp(p.getName()))) {
				e.setCancelled(true);
			}
		} catch (Exception e1) { // we can ignore it
		}
		if (argslc[0].equalsIgnoreCase("/kill")) {
			e.setCancelled(true);
		}
		if ((argslc[0].equalsIgnoreCase("/deop")) && (!SQLTable.hasRank(p.getName(), Rank.ADMIN))) {
			e.setCancelled(true);
		}
		if (argslc[0].equalsIgnoreCase("/?") || argslc[0].equalsIgnoreCase("/help")) {
			e.setCancelled(true);
			p.sendMessage("§bIf you need help, contact an online staff member by\n"
					+ "starting your chat message with '@' [no quotes]\n"
					+ "§cIf no staff members are online, try our forums at forums.mcshockwave.net");
		}
		if (argslc[0].equalsIgnoreCase("/me") || argslc[0].equalsIgnoreCase("/pl")
				|| argslc[0].equalsIgnoreCase("/plugins")) {
			e.setCancelled(true);
			p.sendMessage("Unknown command. Type \"/help\" for help.");
		}
		if (argslc[0].equalsIgnoreCase("/tell") || argslc[0].equalsIgnoreCase("/w")
				|| argslc[0].equalsIgnoreCase("/msg")) {
			showToAdmins = false;
			boolean muted = SQLTable.Muted.has("Username", p.getName());
			e.setCancelled(true);
			if (!muted) {
				String mes = message;
				for (String m : cuss) {
					String star = "";
					for (int i = 0; i < m.length(); i++) {
						star += "*";
					}
					mes = mes.replaceAll("(?i)" + new StringBuffer(m).reverse().toString(), star);
				}
				message = message.replaceFirst(args[0] + " " + args[1], "");

				MCShockwave.privateMessage(p.getName(), args[1], message);
			}
		}
		if (argslc[0].equalsIgnoreCase("/r")) {
			showToAdmins = false;
			boolean muted = SQLTable.Muted.has("Username", p.getName());
			e.setCancelled(true);
			if (!muted) {
				String mes = message;
				for (String m : cuss) {
					String star = "";
					for (int i = 0; i < m.length(); i++) {
						star += "*";
					}
					mes = mes.replaceAll("(?i)" + new StringBuffer(m).reverse().toString(), star);
				}
				message = message.replaceFirst(args[0], "");

				MCShockwave.slashR(p.getName(), message);
			}
		}
		if (argslc[0].equalsIgnoreCase("/ban") && SQLTable.hasRank(p.getName(), Rank.MOD)) {
			e.setCancelled(true);
			if (SQLTable.hasRank(args[1], Rank.ADMIN)) {
				return;
			}
			String toBan = args[1];
			String reason = "";

			for (int i = 2; i < args.length; i++) {
				reason += " " + args[i];
			}
			reason = reason.replaceFirst(" ", "");

			BanManager.setBanned(toBan, -1, reason, p.getName(), "Permanent");
			MCShockwave.sendMessageToRank("§6[" + MCShockwave.server + "] §e" + p.getName() + " banned " + toBan
					+ " permanently" + (reason.length() > 0 ? " for " + reason : ""), Rank.JR_MOD);
		}
		if (argslc[0].equalsIgnoreCase("/kick") && SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
			e.setCancelled(true);
			if (SQLTable.hasRank(args[1], Rank.ADMIN)) {
				return;
			}
			String string = "";
			String pre = "Kicked by " + p.getName() + ": ";
			if (args.length >= 3) {
				for (int i = 2; i < args.length; i++) {
					string += " " + args[i];
				}
			}
			MCShockwave.sendMessageToRank(
					ChatColor.GOLD + "[" + MCShockwave.server + "] " + ChatColor.YELLOW + p.getName() + " Kicked "
							+ args[1] + " for " + string, Rank.JR_MOD);
			string = pre + string;
			if (Bukkit.getPlayer(args[1]) != null) {
				Bukkit.getPlayer(args[1]).kickPlayer(string);
			}
		}
		if (argslc[0].equalsIgnoreCase("/pardon") && SQLTable.hasRank(p.getName(), Rank.MOD)) {
			e.setCancelled(true);
			SQLTable.Banned.del("Username", args[1]);
			MCShockwave.sendMessageToRank(
					ChatColor.GOLD + "[" + MCShockwave.server + "] " + ChatColor.GREEN + p.getName() + " pardoned "
							+ args[1], Rank.JR_MOD);
		}
		if (argslc[0].equalsIgnoreCase("/say")) {
			e.setCancelled(true);
			String s = args[1].replace(
					"@r",
					Bukkit.getOnlinePlayers().toArray(new Player[0])[rand.nextInt(Bukkit.getOnlinePlayers().size())]
							.getName()).replace("&", "§");
			for (int i = 2; i < args.length; i++) {
				s += " "
						+ args[i].replace(
								"@r",
								Bukkit.getOnlinePlayers().toArray(new Player[0])[rand.nextInt(Bukkit.getOnlinePlayers()
										.size())].getName()).replace("&", "§");
			}
			if (SQLTable.hasRank(p.getName(), Rank.ADMIN)) {
				Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.RED + "ADMIN" + ChatColor.DARK_GRAY
						+ "] " + ChatColor.GRAY + s);
			} else if (SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
				Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "MODERATOR" + ChatColor.DARK_GRAY
						+ "] " + ChatColor.GRAY + s);
			}
		}
		if (showToAdmins && SQLTable.hasRank(p.getName(), Rank.JR_MOD) && !SQLTable.hasRank(p.getName(), Rank.ADMIN)) {
			if (!MCShockwave.server.equalsIgnoreCase("event")) {
				for (Player p2 : Bukkit.getOnlinePlayers()) {
					if (SQLTable.hasRank(p2.getName(), Rank.ADMIN)) {
						p2.sendMessage(ChatColor.YELLOW + p.getName() + ChatColor.AQUA + " executed command: "
								+ ChatColor.GOLD + message);
					}
				}
				SQLTable.ModCommands.add("Username", p.getName(), "Command", e.getMessage());
			}
		}
		if (p.isOp()) {
			String mes = e.getMessage();

			if (mes.contains("@all")) {
				e.setCancelled(true);
				for (Player p2 : Bukkit.getOnlinePlayers()) {
					p.performCommand(mes.replaceFirst("/", "").replace("@all", p2.getName()));
				}
			}
		}
	}

	public static String checkNameChange(String plName) {
		for (String s : MCShockwave.getNameChangesFor(plName)) {
			s = s.split(":::")[0];
			boolean isLoggedPastUser = s.equalsIgnoreCase(plName)
					|| SQLTable.OldUsernames.hasWhere("Username", "Username='" + plName + "' AND OldName='" + s + "'");
			if (!isLoggedPastUser) {
				return s;
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerLogin(final PlayerLoginEvent event) {
		final String pl = event.getPlayer().getName();
		// if (SQLTable.Banned.has("Username", pl)) {
		// long min =
		// TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
		// int time = SQLTable.Banned.getInt("Username", pl, "Time");
		// if (time <= min && SQLTable.Banned.getInt("Username", pl, "Time") !=
		// 0) {
		// SQLTable.Banned.del("Username", pl);
		// }
		// }
		// if (SQLTable.Banned.has("Username", pl) ||
		// SQLTable.Banned.getInt("Username", pl, "Time") == 0) {
		// String s = ChatColor.GREEN + "You are banned from this server: "
		// + SQLTable.Banned.get("Username", pl, "Reason") +
		// "\nAppeal at forums.mcshockwave.net\n";
		// int time = SQLTable.Banned.getInt("Username", pl, "Time");
		// if (time >= 1) {
		// long min =
		// TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
		// long diff = time - min;
		// s += "Time left of ban: " + diff + " minutes.";
		// }
		// event.disallow(Result.KICK_BANNED, s);
		// }

		if (MCShockwave.server.equalsIgnoreCase("hub")) {
			new BukkitRunnable() {
				public void run() {
					String s = checkNameChange(pl);
					if (s != null) {
						MCShockwave.registerNameChange(s, pl);
					}
				}
			}.runTaskAsynchronously(MCShockwave.instance);
		}

		if (BanManager.isBanned(pl)) {
			event.disallow(
					Result.KICK_BANNED,
					BanManager.getBanReason(pl)
							+ "      §cIf you feel you were wrongfully banned, appeal on our site at §b§ohttp://forums.mcshockwave.net/");
		}

		if (MCShockwave.min != null && !SQLTable.hasRank(pl, MCShockwave.min)
				&& !Bukkit.getWhitelistedPlayers().contains(Bukkit.getOfflinePlayer(pl))) {
			event.disallow(Result.KICK_WHITELIST, ChatColor.GREEN + "Only " + MCShockwave.min.name().replace('_', ' ')
					+ "+'s can join this server!");
		}

		if (MCShockwave.minLevel > -1 && LevelUtils.getLevelFromXP(LevelUtils.getXP(pl)) < MCShockwave.minLevel) {
			event.disallow(Result.KICK_WHITELIST, ChatColor.GREEN + "Only level " + MCShockwave.minLevel
					+ "+'s can join this server!\nEarn XP by killing players in our gamemodes to level up!");
		}

		if ((!SQLTable.hasRank(pl, Rank.COAL) && !SQLTable.Youtubers.has("Username", pl))
				&& MCShockwave.maxPlayers <= Bukkit.getOnlinePlayers().size()) {
			event.disallow(Result.KICK_FULL, ChatColor.GREEN + "Server full! Buy VIP to join when a server is full!\n"
					+ VIPLink + "\nOr join another one of our servers!");
		}
	}

	@EventHandler
	public void setLeaveMessage(PlayerQuitEvent event) {
		final Player p = event.getPlayer();
		event.setQuitMessage(MCShockwave.mesLeave.replace("&", "§").replaceAll("%p", p.getName()));

		Bukkit.getScheduler().runTaskLater(MCShockwave.instance, new Runnable() {
			public void run() {
				for (Player p2 : Bukkit.getOnlinePlayers()) {
					if (p2 != p) {
						MCShockwave.updateTab(p2);
					}
				}
			}
		}, 10l);

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"enjin setpoints " + p.getName() + " " + PointsUtils.getPoints(p));
	}

	@EventHandler
	public void setKickMessage(PlayerKickEvent event) {
		final Player p = event.getPlayer();
		event.setLeaveMessage(MCShockwave.mesKick.replace("&", "§").replaceAll("%p", p.getName()));

		Bukkit.getScheduler().runTaskLater(MCShockwave.instance, new Runnable() {
			public void run() {
				for (Player p2 : Bukkit.getOnlinePlayers()) {
					if (p2 != p) {
						MCShockwave.updateTab(p2);
					}
				}
			}
		}, 10l);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player p = event.getPlayer();

		if (SQLTable.hasRank(p.getName(), Rank.COAL)) {
			int time = SQLTable.VIPS.getInt("Username", p.getName(), "TimeLeft");
			if (time >= TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())) {
				long timeleft = time - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());
				p.sendMessage(ChatColor.GREEN + "You have " + timeleft + " days of VIP left!");
			}
		}

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				setDisplayName(p);
			}
		}, 1L);

		event.setJoinMessage(MCShockwave.mesJoin.replace("&", "§").replaceAll("%p", p.getName()));
		if (SQLTable.Settings.get("Setting", "Silent_Joins", "Value").contains("," + p.getName())) {
			event.setJoinMessage("");
		}

		for (Player p2 : VanishCommand.vanished.keySet()) {
			if (!VanishCommand.vanished.get(p2))
				continue;
			p.hidePlayer(p2);
		}

		Bukkit.getScheduler().runTaskLater(MCShockwave.instance, new Runnable() {
			public void run() {
				// TabAPI.disableTabForPlayer(p);
				// TabAPI.setPriority(MCShockwave.instance, p, -2);

				MCShockwave.updateTab(p);

				for (Player p2 : Bukkit.getOnlinePlayers()) {
					if (p2 != p) {
						MCShockwave.updateTab(p2);
					}
				}
			}
		}, 10l);

		Statistics.initStats(p.getName());

		new BukkitRunnable() {
			public void run() {
				if (MCShockwave.getClientVersion(p) == 47) {
					String head = "{text:\"" + MCShockwave.tabHeader + "\"}";
					String foot = "{text:\"" + MCShockwave.tabFooter + "\"}";
					PacketPlayOutHeaderFooter pack = new PacketPlayOutHeaderFooter(head, foot);
					PacketUtils.sendPacket(p, pack);

					if (MCShockwave.server.equalsIgnoreCase("hub")) {
						PacketUtils.playTitle(p, 10, 10, 60, "§c§lMCShockwave Network", null);
					}
				}
			}
		}.runTaskLater(MCShockwave.instance, 20);

		try {
			p.sendMessage(SQLTable.Settings.get("Setting", "JM-" + MCShockwave.server, "Value"));
		} catch (Exception e) {
		}

		try {
			boolean op = MCShockwave.isOp(p.getName());
			if (p.isOp() != op) {
				if (op) {
					p.sendMessage("§a§lYou are now op!");
				} else {
					p.sendMessage("§c§lYou are no longer op!");
				}
			}
			p.setOp(op);
		} catch (SQLException e) {
		}
	}

	public static String getPrefix(Player p) {
		String pre = SQLTable.nickNames.get("Username", p.getName(), "Prefix");

		String vipPre = "";
		if (!SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
			if (SQLTable.hasRank(p.getName(), Rank.COAL)) {
				vipPre = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "C" + ChatColor.DARK_GRAY + "oal "
						+ ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.IRON)) {
				vipPre = ChatColor.GRAY + "" + ChatColor.BOLD + "I" + ChatColor.GRAY + "ron " + ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.GOLD)) {
				vipPre = ChatColor.YELLOW + "" + ChatColor.BOLD + "G" + ChatColor.YELLOW + "old " + ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.DIAMOND)) {
				vipPre = ChatColor.AQUA + "" + ChatColor.BOLD + "D" + ChatColor.AQUA + "iamond " + ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.EMERALD)) {
				vipPre = ChatColor.GREEN + "" + ChatColor.BOLD + "E" + ChatColor.GREEN + "merald " + ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.OBSIDIAN)) {
				vipPre = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "O" + ChatColor.DARK_PURPLE + "bsidian "
						+ ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.NETHER)) {
				vipPre = ChatColor.DARK_RED + "" + ChatColor.BOLD + "N" + ChatColor.DARK_RED + "ether "
						+ ChatColor.RESET;
			}
			if (SQLTable.hasRank(p.getName(), Rank.ENDER)) {
				vipPre = ChatColor.BLACK + "" + ChatColor.BOLD + "E" + ChatColor.BLACK + "nder " + ChatColor.RESET;
			}
			if (SQLTable.Youtubers.has("Username", p.getName())) {
				String s = "§c§lYou§fTuber §r" + vipPre;
				vipPre = s;
			}
		}

		return (pre == null || pre.equals("") ? "" : pre + " ") + vipPre;
	}

	public static void setDisplayName(Player p) {
		String name = p.getName();
		if (!SQLTable.hasRank(p.getName(), Rank.COAL)) {
			p.setDisplayName(p.getName());
		}
		if (SQLTable.hasRank(p.getName(), Rank.ADMIN)) {
			name = ChatColor.RED + p.getName();
		} else if (SQLTable.hasRank(p.getName(), Rank.MOD)) {
			name = ChatColor.GOLD + p.getName();
		} else if (SQLTable.hasRank(p.getName(), Rank.JR_MOD)) {
			name = ChatColor.GOLD + p.getName();
		}
		if (SQLTable.nickNames.has("Username", p.getName())
				&& SQLTable.nickNames.get("Username", p.getName(), "Color") != null
				&& SQLTable.nickNames.get("Username", p.getName(), "Nickname") != null) {
			name = ChatColor.valueOf(SQLTable.nickNames.get("Username", p.getName(), "Color"))
					+ SQLTable.nickNames.get("Username", p.getName(), "Nickname");
		}
		String pre = getPrefix(p);
		if (pre != null && pre != "") {
			name = "§7" + pre + "§r" + name;
		}

		int level = LevelUtils.getLevelFromXP(LevelUtils.getXP(p));
		String levelS = String.format("§8[%s§l%s§8]", LevelUtils.getSuffixColor(level), level);
		name = levelS + " " + name;
		name += "§r";

		p.setDisplayName(name + ChatColor.RESET);
	}

	@EventHandler
	public void onWeatherChange(WeatherChangeEvent event) {
		if (event.toWeatherState()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (tntnoboom.contains(event.getEntity())) {
			event.blockList().clear();
			tntnoboom.remove(event.getEntity());
		}
	}

	public Button getButtonFromSlot(HashMap<Button, Integer> bs, int slot) {
		for (Button b : bs.keySet()) {
			if (b.getItemMenu().buttons.get(b) == slot) {
				return b;
			}
		}
		throw new IllegalArgumentException("No Button Found");
	}

	@EventHandler
	public void BootsShop(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if (!e.getInventory().getTitle().equals(p.getName())) {
			return;
		}
		e.setCancelled(true);
		if (e.getSlot() != 4) {
			return;
		}
		int o = 0;
		if (SQLTable.MiscItems.has("Username", p.getName())) {
			o = SQLTable.MiscItems.getInt("Username", p.getName(), "Red_Boots");
		} else {
			SQLTable.MiscItems.add("Username", p.getName());
		}
		if (o == 1) {
			if (!MCShockwave.server.equalsIgnoreCase("hub")) {
				MCShockwave.send(p, "You can only use §cRed Boots§r§7 in the %s server!", "hub");
				p.getOpenInventory().close();
				return;
			}
			PacketUtils.playParticleEffect(ParticleEffect.FLAME, p.getEyeLocation(), 1, 5, 10);
			p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 2);
			MCShockwave.send(ChatColor.RED, p, "Removed your %s", "Red boots.");
			p.getInventory().setBoots(null);
			p.getOpenInventory().close();
			return;
		} else {
			int points = PointsUtils.getPoints(p);
			if (points < 250000) {
				MCShockwave.send(ChatColor.RED, p, "Not enough %s to buy that!", "points");
				return;
			}
			PointsUtils.addPoints(p, -250000, "buying Red Boots (Permanent)");
			SQLTable.MiscItems.set("Red_Boots", "" + 1, "Username", p.getName());
		}
		p.getOpenInventory().close();
	}

	@EventHandler
	public void onInventoryClick(final InventoryClickEvent event) {
		final Inventory i = event.getInventory();
		HumanEntity he = event.getWhoClicked();
		ItemStack cu = event.getCurrentItem();

		if (he instanceof Player) {
			final Player p = (Player) he;

			if (i.getName().endsWith(" Friends") && i.getName().startsWith(p.getName())) {
				if (cu.getType() == Material.SKULL_ITEM && ItemMetaUtils.hasCustomName(cu)) {
					String name = ItemMetaUtils.getItemName(cu);
					name = ChatColor.stripColor(name);

					p.performCommand("friend " + name);
					p.closeInventory();
					final int st = FriendCommand.state.get(p.getName());
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							ItemMenu m = FriendCommand.getFriendsList(p);
							m.open(p);

							getButtonFromSlot(m.buttons, i.getSize() - 6 + st).onClick.run(p, event);
						}
					}, 1l);
				}
			}

			if (i.getName().startsWith("Edit Rank: ")) {
				event.setCancelled(true);
				String edit = i.getName().replaceFirst("Edit Rank: ", "");

				if (cu.getType() == Material.STICK) {
					exec(p, edit, "REMOVE");
				}
				if (cu.getType() == Material.BLAZE_ROD) {
					SQLTable.nickNames.del("Username", edit);
				}
				if (cu.getType() == Material.COAL) {
					exec(p, edit, "COAL");
				}
				if (cu.getType() == Material.IRON_INGOT) {
					exec(p, edit, "IRON");
				}
				if (cu.getType() == Material.GOLD_INGOT) {
					exec(p, edit, "GOLD");
				}
				if (cu.getType() == Material.DIAMOND) {
					exec(p, edit, "DIAMOND");
				}
				if (cu.getType() == Material.EMERALD) {
					exec(p, edit, "EMERALD");
				}
				if (cu.getType() == Material.OBSIDIAN) {
					exec(p, edit, "OBSIDIAN");
				}
				if (cu.getType() == Material.NETHERRACK) {
					exec(p, edit, "NETHER");
				}
				if (cu.getType() == Material.ENDER_STONE) {
					exec(p, edit, "ENDER");
				}
				if (cu.getType() == Material.FIREWORK) {
					exec(p, edit, "YOUTUBE");
				}

				if (cu.getType() == Material.WOOL) {
					short data = cu.getDurability();

					if (data == 4) {
						exec(p, edit, "JR_MOD");
						exec(p, edit, "prefix Jr._Moderator");
						exec(p, edit, "nick color YELLOW");
						exec(p, edit, "nick name " + edit);
					}
					if (data == 1) {
						exec(p, edit, "MOD");
						exec(p, edit, "prefix Moderator");
						exec(p, edit, "nick color GOLD");
						exec(p, edit, "nick name " + edit);
					}
					if (data == 3) {
						exec(p, edit, "SR_MOD");
						exec(p, edit, "prefix Sr._Moderator");
						exec(p, edit, "nick color AQUA");
						exec(p, edit, "nick name " + edit);
					}
					if (data == 8) {
						exec(p, edit, "prefix Builder");
					}
				}

				p.closeInventory();
			}
		}
	}

	public void exec(Player p, String edit, String s) {
		Bukkit.dispatchCommand(p, "editrank " + edit + " " + s);
	}

}
