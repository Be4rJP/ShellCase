package be4rjp.shootarian.player.death;

import be4rjp.shootarian.Shootarian;
import be4rjp.shootarian.match.Match;
import be4rjp.shootarian.match.team.ShootarianTeam;
import be4rjp.shootarian.player.ShootarianPlayer;
import be4rjp.shootarian.weapon.ShootarianWeapon;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerDeathRunnable extends BukkitRunnable {
    
    private static final double DISTANCE = 3.0;
    
    private final Location respawnLocation;
    private final ShootarianPlayer target;
    private final ShootarianPlayer killer;
    private final ShootarianWeapon ShootarianWeapon;
    private final DeathType deathType;
    
    private final EntityArmorStand armorStand;
    
    private Location center;
    
    private int tick = 0;
    private int timeLeft;
    
    public PlayerDeathRunnable(Location respawnLocation, ShootarianPlayer target, ShootarianPlayer killer, ShootarianWeapon ShootarianWeapon, DeathType deathType){
        this.respawnLocation = respawnLocation;
        this.target = target;
        this.killer = killer;
        this.ShootarianWeapon = ShootarianWeapon;
        this.deathType = deathType;
        
        this.center = killer.getLocation();
        target.teleport(center);
        
        this.timeLeft = 10;
        
        Location loc = killer.getLocation();
        this.armorStand = new EntityArmorStand(((CraftWorld)loc.getWorld()).getHandle(), loc.getX(), loc.getY(), loc.getZ());
        armorStand.setInvisible(true);
        PacketPlayOutSpawnEntityLiving spawn = new PacketPlayOutSpawnEntityLiving(armorStand);
        PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(armorStand.getId(), armorStand.getDataWatcher(), true);
        target.sendPacket(spawn);
        target.sendPacket(metadata);
    }
    
    
    @Override
    public void run() {
    
        ShootarianTeam shootarianTeam = target.getShootarianTeam();
        if(shootarianTeam == null){
            cancel();
            return;
        }
        if(shootarianTeam.getMatch().getMatchStatus() == Match.MatchStatus.FINISHED){
            cancel();
            return;
        }
        
        if(!killer.isDeath()){
            this.center = killer.getLocation();
        }
        
        double theta = tick * 2;
        double x = this.center.getX() + (DISTANCE * Math.sin(Math.toRadians(theta)));
        double y = this.center.getY() + 1.0;
        double z = this.center.getZ() + (DISTANCE * Math.cos(Math.toRadians(theta)));
    
        Vector direction = new Vector(this.center.getX() - x, this.center.getY() - y, this.center.getZ() - z).normalize();
        Location temp = this.center.clone();
        temp.setDirection(direction);
        float yaw = temp.getYaw();
        float pitch = temp.getPitch();
        
        this.armorStand.setLocation(x, y, z, yaw, pitch);
    
        PacketPlayOutEntityTeleport teleport = new PacketPlayOutEntityTeleport(armorStand);
        PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook look =
                new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(armorStand.getId(), (short) 0, (short) 0, (short) 0, (byte)((yaw * 256.0F) / 360.0F), (byte)((pitch * 256.0F) / 360.0F), true);
        PacketPlayOutEntityHeadRotation rotation = new PacketPlayOutEntityHeadRotation(armorStand, (byte)((yaw * 256.0F) / 360.0F));
        PacketPlayOutCamera camera = new PacketPlayOutCamera(armorStand);
        target.sendPacket(teleport);
        target.sendPacket(look);
        target.sendPacket(rotation);
        target.sendPacket(camera);
        
        
        //タイトルテキスト
        if(tick % 20 == 0) {
            switch (deathType) {
                case KILLED_BY_PLAYER: {
                    target.sendTextTitle("match-respawn-count", new Object[]{timeLeft},
                            "match-killed-title", new Object[]{killer.getDisplayName(), ShootarianWeapon.getDisplayName(target.getLang())},
                            0, 30, 0);
                    break;
                }
        
                case FELL_OUT_OF_THE_WORLD: {
                    target.sendTextTitle("match-respawn-count", new Object[]{timeLeft},
                            "match-fall-void-title", new Object[]{},
                            0, 30, 0);
                    break;
                }
            }
            if(timeLeft == 3) target.teleport(center);
    
            if(timeLeft == 0){
                this.cancel();
            }
            timeLeft--;
        }
        
        tick++;
    }
    
    
    @Override
    public synchronized void cancel() throws IllegalStateException {
        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(armorStand.getId());
        Player player = target.getBukkitPlayer();
        if(player != null){
            PacketPlayOutCamera camera = new PacketPlayOutCamera(((CraftPlayer)player).getHandle());
            target.sendPacket(camera);
        }
        target.sendPacket(destroy);
        target.resetTitle();
        target.respawn(respawnLocation);
        target.getWeaponClass().reset();
        PlayerDeathManager.deathPlayer.remove(target.getBukkitPlayer());
        super.cancel();
    }
    
    public void start(){this.runTaskTimerAsynchronously(Shootarian.getPlugin(), 0, 1);}
}
