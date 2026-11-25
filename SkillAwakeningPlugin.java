package com.vgaming70.skillawakening;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import org.bukkit.block.Container;

import java.util.*;

/** Skill Awakening Paper port. Creator: Vgaming70. Standard inventory GUIs are Geyser-friendly. */
public final class SkillAwakeningPlugin extends JavaPlugin implements Listener {
    private static final String MAIN="§5✦ Skill Awakening ✦", EQUIP="§0✦ Skill Equipment ✦", ARCHIVE="§0✦ Skill Archive ✦", CAST="§cChoose a skill", ADMIN="§4Mythical Admin";
    private static final String[] MYTHICAL={"World Breaker","Spatial Crack","Reality Shift"};
    private final LinkedHashMap<String,String> ranks=new LinkedHashMap<>();
    private final Map<UUID,LinkedHashSet<String>> learned=new HashMap<>(), equipped=new HashMap<>();
    private final Map<UUID,Map<String,Long>> cooldowns=new HashMap<>();
    private final Map<String,UUID> mythicalClaim=new HashMap<>();
    private final Random random=new Random();
    private NamespacedKey kindKey, skillKey;

    @Override public void onEnable(){
        kindKey=new NamespacedKey(this,"kind"); skillKey=new NamespacedKey(this,"skill");
        add("Common","Dash","Stone Skin","Minor Heal","Water Breathing","Double Jump","Night Vision","Swift Hands","Fire Resistance");
        add("Rare","Lightning Step","Frost Touch","Wind Burst","Vampiric Touch","Earth Grip","Shadow Cloak");
        add("Epic","Thunder Strike","Ice Prison","Regeneration","Gravity Well","Flame Wave");
        add("Legendary","Meteor Fall","Time Freeze","Dragon Aura","Void Shift");
        add("Mythical",MYTHICAL);
        loadProgress(); getServer().getPluginManager().registerEvents(this,this); registerTabletRecipe();
        getLogger().info("Skill Awakening 8.9.1 enabled; creator Vgaming70. Gameplay needs live-server validation.");
    }
    @Override public void onDisable(){saveProgress();}
    private void add(String rank,String... names){for(String n:names)ranks.put(n,rank);}

    // Exact recipe recovered from the supplied 8.9.0 compiled JAR: LLL / EDE / BBB.
    private void registerTabletRecipe(){
        NamespacedKey key=new NamespacedKey(this,"ancient_tablet");
        Bukkit.removeRecipe(key);
        ShapedRecipe r=new ShapedRecipe(key,tablet()); r.shape("LLL","EDE","BBB");
        r.setIngredient('L',Material.LAPIS_LAZULI);r.setIngredient('E',Material.EXPERIENCE_BOTTLE);r.setIngredient('D',Material.DIAMOND_BLOCK);r.setIngredient('B',Material.BOOK);
        Bukkit.addRecipe(r);
    }
    private ItemStack tagged(Material material,String name,String kind,String skill){
        ItemStack it=new ItemStack(material);ItemMeta m=it.getItemMeta();m.setDisplayName(name);
        m.getPersistentDataContainer().set(kindKey,PersistentDataType.STRING,kind);
        if(skill!=null)m.getPersistentDataContainer().set(skillKey,PersistentDataType.STRING,skill);
        it.setItemMeta(m);return it;
    }
    private ItemStack menuToken(){return tagged(Material.WRITTEN_BOOK,"§b§lSKILL MENU §7(Use)","menu",null);}
    private ItemStack tablet(){return tagged(Material.ENCHANTED_BOOK,"§d§lANCIENT TABLET","tablet",null);}
    private ItemStack book(String s){ItemStack it=tagged(Material.BOOK,"§dAncient Skill Book: §f"+s,"skillbook",s);ItemMeta m=it.getItemMeta();m.setLore(List.of("§7Rarity: §f"+ranks.get(s),"§8Right-click to learn"));it.setItemMeta(m);return it;}
    private String kind(ItemStack it){return it!=null&&it.hasItemMeta()?it.getItemMeta().getPersistentDataContainer().get(kindKey,PersistentDataType.STRING):null;}
    private String skill(ItemStack it){return it!=null&&it.hasItemMeta()?it.getItemMeta().getPersistentDataContainer().get(skillKey,PersistentDataType.STRING):null;}
    private void consume(ItemStack it){if(it==null)return;if(it.getAmount()<=1)it.setAmount(0);else it.setAmount(it.getAmount()-1);}

    @EventHandler public void onFirstJoin(PlayerJoinEvent e){Player p=e.getPlayer();String path="starter."+p.getUniqueId();if(getConfig().getBoolean(path,false))return;p.getInventory().addItem(menuToken(),tablet());getConfig().set(path,true);saveProgress();p.sendMessage("§dSkill Awakening §7— use your Skill Menu book to open the system.");}
    @EventHandler public void onUse(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;ItemStack it=e.getItem();String k=kind(it);if(k==null)return;e.setCancelled(true);Player p=e.getPlayer();switch(k){case "menu"->openMain(p);case "tablet"->rollAnimated(p,it);case "skillbook"->learnBook(p,it);}}
    private void learnBook(Player p,ItemStack it){String s=skill(it);if(s==null||!ranks.containsKey(s))return;if(ranks.get(s).equals("Mythical")&&!p.getUniqueId().equals(mythicalClaim.get(s))){p.sendMessage("§cThis Mythical skill is bound to its world claimant.");return;}var set=learned.computeIfAbsent(p.getUniqueId(),x->new LinkedHashSet<>());if(set.add(s)){consume(it);saveProgress();p.sendMessage("§aSkill learned: §f"+s);}else p.sendMessage("§cYou already know this skill. Withdraw it from the Archive to get a book.");}
    private void rollAnimated(Player p,ItemStack tabletItem){if(kind(tabletItem)==null)return;consume(tabletItem);p.playSound(p.getLocation(),Sound.BLOCK_BEACON_AMBIENT,0.7f,1.6f);final int[] tick={0};Bukkit.getScheduler().runTaskTimer(this,task->{tick[0]++;p.spawnParticle(Particle.PORTAL,p.getLocation().add(0,1,0),12,0.5,0.6,0.5,0.5);p.playSound(p.getLocation(),Sound.UI_BUTTON_CLICK,0.6f,0.7f+tick[0]*0.05f);if(tick[0]>=20){task.cancel();String s=rollSkill();if(s==null){p.sendMessage("§cNo available skills.");return;}p.getInventory().addItem(book(s));p.sendMessage("§d✦ AWAKENING ✦ §f"+ranks.get(s)+" §7— §b"+s);p.playSound(p.getLocation(),Sound.UI_TOAST_CHALLENGE_COMPLETE,1,1);if(ranks.get(s).equals("Mythical")){mythicalClaim.put(s,p.getUniqueId());Bukkit.broadcastMessage("§5✦ MYTHICAL §d"+p.getName()+" §7obtained the skill book: §f"+s+"§7!");saveProgress();}}},0L,1L);}
    private String rollSkill(){String[] tiers={"Common","Rare","Epic","Legendary","Mythical"};int roll=random.nextInt(100);String tier=roll<60?tiers[0]:roll<85?tiers[1]:roll<95?tiers[2]:roll<99?tiers[3]:tiers[4];List<String> pool=pool(tier);if(pool.isEmpty()){tier=tiers[random.nextInt(4)];pool=pool(tier);}return pool.isEmpty()?null:pool.get(random.nextInt(pool.size()));}
    private List<String> pool(String rank){List<String> list=new ArrayList<>();for(var entry:ranks.entrySet())if(entry.getValue().equals(rank)&&(!rank.equals("Mythical")||!mythicalClaim.containsKey(entry.getKey())))list.add(entry.getKey());return list;}

    private void openMain(Player p){Inventory inv=Bukkit.createInventory(null,27,MAIN);inv.setItem(10,tagged(Material.NETHER_STAR,"§a§lEquip Skills","equip",null));inv.setItem(13,tagged(Material.BOOK,"§e§lSkill Archive / Withdraw","archive",null));inv.setItem(16,tagged(Material.BLAZE_POWDER,"§c§lActivate Equipped","activate",null));if(p.isOp())inv.setItem(22,tagged(Material.COMMAND_BLOCK,"§4§lMythical Admin","admin",null));p.openInventory(inv);}
    private void openEquip(Player p){Inventory inv=Bukkit.createInventory(null,54,EQUIP);int i=0;Set<String> eq=equipped.computeIfAbsent(p.getUniqueId(),x->new LinkedHashSet<>());for(String s:learned.getOrDefault(p.getUniqueId(),new LinkedHashSet<>())){if(i>=45)break;Material icon=rankIcon(ranks.get(s));ItemStack it=tagged(icon,(eq.contains(s)?"§a✓ ":"§f")+s,"toggle",s);ItemMeta m=it.getItemMeta();m.setLore(List.of("§7Rarity: §f"+ranks.get(s),eq.contains(s)?"§aEquipped — click to remove":"§eClick to equip (max 5)"));it.setItemMeta(m);inv.setItem(i++,it);}inv.setItem(49,tagged(Material.ARROW,"§cBack","back",null));p.openInventory(inv);}
    private void openArchive(Player p){Inventory inv=Bukkit.createInventory(null,54,ARCHIVE);int i=0;for(String s:learned.getOrDefault(p.getUniqueId(),new LinkedHashSet<>())){if(i>=45)break;ItemStack it=tagged(Material.BOOK,"§eWithdraw §f"+s,"withdraw",s);ItemMeta m=it.getItemMeta();m.setLore(List.of("§7Withdraw skill from learned list","§7Unequips automatically; returns a physical book"));it.setItemMeta(m);inv.setItem(i++,it);}inv.setItem(49,tagged(Material.ARROW,"§cBack","back",null));p.openInventory(inv);}
    private void openAdmin(Player p){if(!p.isOp())return;Inventory inv=Bukkit.createInventory(null,27,ADMIN);for(int i=0;i<3;i++){String s=MYTHICAL[i];UUID owner=mythicalClaim.get(s);inv.setItem(10+i,tagged(Material.NETHER_STAR,"§d"+s+" §7["+(owner==null?"§aAVAILABLE":"§eCLAIMED")+"]","release",s));}inv.setItem(22,tagged(Material.BARRIER,"§cClose","close",null));p.openInventory(inv);}
    @EventHandler public void menuClick(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(!List.of(MAIN,EQUIP,ARCHIVE,ADMIN,CAST).contains(title))return;e.setCancelled(true);ItemStack it=e.getCurrentItem();if(it==null)return;String k=kind(it),s=skill(it);switch(k==null?"":k){case "equip"->openEquip(p);case "archive"->openArchive(p);case "activate"->openCast(p);case "admin"->{if(p.isOp())openAdmin(p);}case "back"->openMain(p);case "close"->p.closeInventory();case "toggle"->{if(s==null)return;var set=equipped.computeIfAbsent(p.getUniqueId(),x->new LinkedHashSet<>());if(!set.remove(s)){if(set.size()>=5){p.sendMessage("§cEquipment limit: five skills.");return;}set.add(s);p.sendMessage("§aEquipped §f"+s);}else p.sendMessage("§eUnequipped §f"+s);saveProgress();openEquip(p);}case "withdraw"->{if(s==null)return;learned.getOrDefault(p.getUniqueId(),new LinkedHashSet<>()).remove(s);equipped.getOrDefault(p.getUniqueId(),new LinkedHashSet<>()).remove(s);p.getInventory().addItem(book(s));saveProgress();p.sendMessage("§eWithdrawn §f"+s+"§e. Physical book returned.");openArchive(p);}case "release"->{if(p.isOp()&&s!=null){mythicalClaim.remove(s);saveProgress();openAdmin(p);p.sendMessage("§eMythical uniqueness released for §f"+s);}}case "cast"->{if(s!=null){p.closeInventory();activate(p,s);}}}}
    private void openCast(Player p){Inventory inv=Bukkit.createInventory(null,27,CAST);int i=0;for(String s:equipped.getOrDefault(p.getUniqueId(),new LinkedHashSet<>()))inv.setItem(i++,tagged(Material.BLAZE_POWDER,"§cCast §f"+s,"cast",s));inv.setItem(22,tagged(Material.BARRIER,"§cClose","close",null));p.openInventory(inv);}
    private Material rankIcon(String rank){return switch(rank){case "Rare"->Material.LAPIS_LAZULI;case "Epic"->Material.AMETHYST_SHARD;case "Legendary"->Material.GOLD_INGOT;case "Mythical"->Material.NETHER_STAR;default->Material.IRON_INGOT;};}
    private void activate(Player p,String s){int seconds=s.equals("World Breaker")||s.equals("Spatial Crack")?210:s.equals("Reality Shift")?330:30;if(!cooldown(p,s,seconds))return;switch(s){case "Minor Heal"->p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+4));case "Stone Skin"->p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,160,0));case "Reality Shift"->{p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,200,9));p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,200,0));}case "Spatial Crack"->{Location dest=raySafe(p,30);if(dest!=null)p.teleport(dest);p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,1));}case "World Breaker"->{LivingEntity target=target(p,5);if(target!=null)target.setHealth(Math.max(0,target.getHealth()-8.0));}case "Frost Touch"->near(p,5,t->t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,1)));case "Lightning Step"->{Location dest=raySafe(p,8);if(dest!=null)p.teleport(dest);}case "Water Breathing"->p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,600,0));case "Night Vision"->p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,600,0));case "Swift Hands"->p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,600,0));case "Fire Resistance"->p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,600,0));case "Regeneration"->p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,300,1));case "Dragon Aura"->p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,300,1));case "Shadow Cloak"->p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,200,0));case "Time Freeze"->near(p,4,t->t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,10)));case "Dash","Void Shift"->p.setVelocity(p.getLocation().getDirection().multiply(1.8));case "Double Jump"->p.setVelocity(new Vector(0,0.8,0));case "Wind Burst"->near(p,5,t->t.setVelocity(t.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(-1.4).setY(0.5)));case "Vampiric Touch"->{LivingEntity target=target(p,5);if(target!=null){target.damage(4,p);p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+2));}}case "Earth Grip"->near(p,4,t->t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,80,3)));case "Thunder Strike"->{LivingEntity t=target(p,8);if(t!=null){t.getWorld().strikeLightningEffect(t.getLocation());t.damage(8,p);}}case "Ice Prison"->near(p,4,t->t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,100,5)));case "Gravity Well"->near(p,6,t->t.setVelocity(p.getLocation().toVector().subtract(t.getLocation().toVector()).normalize().multiply(1.1)));case "Flame Wave"->near(p,5,t->{t.setFireTicks(100);t.damage(4,p);});case "Meteor Fall"->near(p,8,t->t.damage(12,p));default->{}}p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation().add(0,1,0),24,0.5,0.7,0.5,0.08);p.playSound(p.getLocation(),Sound.ENTITY_EVOKER_CAST_SPELL,1,1.15f);}
    private boolean cooldown(Player p,String s,int sec){long now=System.currentTimeMillis(),end=cooldowns.computeIfAbsent(p.getUniqueId(),x->new HashMap<>()).getOrDefault(s,0L);if(now<end){p.sendMessage("§cCooldown: "+((end-now+999)/1000)+" seconds");return false;}cooldowns.get(p.getUniqueId()).put(s,now+sec*1000L);return true;}
    private LivingEntity target(Player p,double range){return p.getNearbyEntities(range,range,range).stream().filter(e->e instanceof LivingEntity&&e!=p).map(e->(LivingEntity)e).min(Comparator.comparingDouble(e->e.getLocation().distanceSquared(p.getLocation()))).orElse(null);}
    private void near(Player p,double range,java.util.function.Consumer<LivingEntity> f){for(Entity e:p.getNearbyEntities(range,range,range))if(e instanceof LivingEntity t&&e!=p)f.accept(t);}
    private Location raySafe(Player p,int range){Location eye=p.getEyeLocation();Vector dir=eye.getDirection().normalize();Location last=p.getLocation();for(int n=1;n<=range*4;n++){Location point=eye.clone().add(dir.clone().multiply(n*0.25));if(!point.getBlock().isPassable())break;Location feet=point.clone().subtract(0,1,0);if(feet.getBlock().isPassable()&&feet.clone().add(0,1,0).getBlock().isPassable()&&feet.clone().subtract(0,1,0).getBlock().isSolid())last=feet;}return last.equals(p.getLocation())?null:last;}
    @EventHandler public void pvpSkillDrop(EntityDeathEvent e){if(!(e.getEntity() instanceof Player victim))return;Player killer=victim.getKiller();if(killer==null)return;var skills=learned.get(victim.getUniqueId());if(skills==null||skills.size()<=3)return;List<String> eligible=new ArrayList<>();for(String skillName:skills)if(!ranks.get(skillName).equals("Mythical"))eligible.add(skillName);if(eligible.isEmpty())return;String dropped=eligible.get(random.nextInt(eligible.size()));skills.remove(dropped);equipped.getOrDefault(victim.getUniqueId(),new LinkedHashSet<>()).remove(dropped);e.getDrops().add(book(dropped));saveProgress();victim.sendMessage("§cPvP drop: §f"+dropped+" §7(skill book)");}
    @EventHandler public void protectAwakeningItems(PlayerDeathEvent e){e.getDrops().removeIf(it->"menu".equals(kind(it))||"tablet".equals(kind(it)));}
    @EventHandler public void structureChestTablet(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null||!e.getClickedBlock().getType().name().contains("CHEST"))return;if(random.nextDouble()>0.04)return;if(!(e.getClickedBlock().getState() instanceof Container c))return;String key="tabletChestReward."+e.getClickedBlock().getWorld().getName()+"."+e.getClickedBlock().getX()+"."+e.getClickedBlock().getY()+"."+e.getClickedBlock().getZ();if(getConfig().getBoolean(key,false))return;c.getInventory().addItem(tablet());getConfig().set(key,true);saveProgress();e.getPlayer().sendMessage("§dYou discovered an Ancient Tablet chest reward!");}
    private void loadProgress(){for(String s:MYTHICAL){String v=getConfig().getString("mythical."+s);if(v!=null)try{mythicalClaim.put(s,UUID.fromString(v));}catch(IllegalArgumentException ignored){}}var section=getConfig().getConfigurationSection("players");if(section==null)return;for(String raw:section.getKeys(false))try{UUID id=UUID.fromString(raw);learned.put(id,new LinkedHashSet<>(getConfig().getStringList("players."+raw+".learned")));equipped.put(id,new LinkedHashSet<>(getConfig().getStringList("players."+raw+".equipped")));}catch(IllegalArgumentException ignored){}}
    private void saveProgress(){for(String s:MYTHICAL){UUID id=mythicalClaim.get(s);getConfig().set("mythical."+s,id==null?null:id.toString());}for(UUID id:learned.keySet()){getConfig().set("players."+id+".learned",new ArrayList<>(learned.get(id)));getConfig().set("players."+id+".equipped",new ArrayList<>(equipped.getOrDefault(id,new LinkedHashSet<>())));}saveConfig();}
}
