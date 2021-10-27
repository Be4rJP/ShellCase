package be4rjp.shellcase.entity;

import be4rjp.shellcase.data.settings.Settings;
import be4rjp.shellcase.match.Match;
import be4rjp.shellcase.match.team.ShellCaseTeam;
import be4rjp.shellcase.player.ShellCasePlayer;
import be4rjp.shellcase.util.BoundingBox;
import be4rjp.shellcase.util.LocationUtil;
import be4rjp.shellcase.util.RayTrace;
import be4rjp.shellcase.util.ShellCaseSound;
import be4rjp.shellcase.util.particle.BlockParticle;
import be4rjp.shellcase.util.particle.NormalParticle;
import be4rjp.shellcase.util.particle.ShellCaseParticle;
import be4rjp.shellcase.weapon.ShellCaseWeapon;
import be4rjp.shellcase.weapon.gadget.GadgetWeapon;
import be4rjp.shellcase.weapon.gun.GunWeapon;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class AsyncRPGBulletEntity implements ShellCaseEntity{
    
    private static final ShellCaseSound SOUND1 = new ShellCaseSound(Sound.ENTITY_PIG_STEP, 1.5F, 0.8F);
    private static final ShellCaseSound SOUND2 = new ShellCaseSound(Sound.ENTITY_PIG_STEP, 1.5F, 1.1F);
    private static final ShellCaseSound SOUND3 = new ShellCaseSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8F, 0.8F);
    private static final ShellCaseSound SOUND4 = new ShellCaseSound(Sound.BLOCK_FIRE_EXTINGUISH, 1.1F, 0.9F);
    private static final ShellCaseSound SOUND5 = new ShellCaseSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8F, 0.55F);
    

    private final Match match;
    private final ShellCaseTeam team;
    private Location location;
    private final GadgetWeapon gadgetWeapon;

    private ShellCasePlayer shooter;
    private Vector direction = new Vector(0, 0, 0);
    private Vector originalDirection = new Vector(0.0, 0.0, 0.0);
    private boolean hitSound = false;
    private boolean hitParticle = false;
    private double bulletSize = 0.2;
    private boolean particle = true;

    private int tick = 0;
    private int fallTick = 0;

    private boolean remove = false;
    private int removeTick = 0;
    private boolean isDead = false;
    
    private double G = -2.5;

    private Set<ShellCasePlayer> showPlayer = new HashSet<>();

    public AsyncRPGBulletEntity(ShellCaseTeam team, Location location, GadgetWeapon gadgetWeapon){
        this.team = team;
        this.match = team.getMatch();
        this.location = location.clone();
        this.gadgetWeapon = gadgetWeapon;
    }
    
    public void shootInitialize(ShellCasePlayer shooter, Vector direction, int fallTick){
        this.direction = direction.clone();
        this.originalDirection = direction.clone();
        this.shooter = shooter;
        this.fallTick = fallTick;
    }

    public ShellCasePlayer getShooter(){return this.shooter;}

    public boolean isHitParticle() {return hitParticle;}

    public boolean isHitSound() {return hitSound;}

    public void setHitParticle(boolean hitParticle) {this.hitParticle = hitParticle;}

    public void setHitSound(boolean hitSound) {this.hitSound = hitSound;}

    public GadgetWeapon getGadgetWeapon() {return gadgetWeapon;}

    public double getBulletSize() {return bulletSize;}

    public void setBulletSize(double bulletSize) {this.bulletSize = bulletSize;}

    public void setParticle(boolean particle) {this.particle = particle;}
    
    public void setG(double g) {G = g;}
    
    @Override
    public void tick() {

        if(tick >= 2000 || location.getY() < 0 || (remove && removeTick == 1)){
            remove();
            return;
        }
        if(remove) removeTick++;
        
        //Sound
        if(tick == 0){
            match.playSound(SOUND1, location);
            match.playSound(SOUND2, location);
            match.playSound(SOUND3, location);
    
            match.spawnParticle(new NormalParticle(Particle.SMOKE_LARGE, 5, 0.5, 0.5, 0.5, 1), location);
        }
        if(tick == 1){
            match.playSound(SOUND4, location);
            match.playSound(SOUND5, location);
        }

        
        Location oldLocation = location.clone();
        Vector oldDirection = direction.clone();

        Location hitLocation = null;
        if(!remove) {

            Location playerHitLocation = null;
            ShellCasePlayer target = null;

            RayTrace rayTrace = new RayTrace(oldLocation.toVector(), oldDirection);
            for (ShellCasePlayer shellCasePlayer : match.getPlayers()) {
                Player player = shellCasePlayer.getBukkitPlayer();
                if (player == null) continue;
                if (shellCasePlayer.getShellCaseTeam() == null) continue;
                if (shellCasePlayer.isDeath()) continue;

                BoundingBox boundingBox = new BoundingBox(player, bulletSize);
                Vector position = rayTrace.intersects(boundingBox, oldDirection.length(), 0.01);
                if (position == null) continue;
                if (this.team == shellCasePlayer.getShellCaseTeam()) continue;

                playerHitLocation = position.toLocation(location.getWorld());
                target = shellCasePlayer;

                remove = true;
            }

            Location blockHitLocation = null;

            try {
                RayTraceResult rayTraceResult = oldLocation.getWorld().rayTraceBlocks(oldLocation, oldDirection, oldDirection.length());
                if (rayTraceResult != null) {
                    blockHitLocation = rayTraceResult.getHitPosition().toLocation(oldLocation.getWorld());

                    remove = true;
                }
            } catch (Exception e) {
                remove();
            }


            boolean playerHit = false;
            boolean blockHit = false;
            if(playerHitLocation != null && blockHitLocation == null) {
                playerHit = true;
            }
            if(playerHitLocation == null && blockHitLocation != null){
                blockHit = true;
            }
            if(playerHitLocation != null && blockHitLocation != null){
                if(LocationUtil.distanceSquaredSafeDifferentWorld(playerHitLocation, location) < LocationUtil.distanceSquaredSafeDifferentWorld(blockHitLocation, location)){
                    playerHit = true;
                }else{
                    blockHit = true;
                }
            }

            if(playerHit){
                hitLocation = playerHitLocation;
            }
            if(blockHit){
                hitLocation = blockHitLocation;
            }
        }

        if(tick >= fallTick) {
            double t = ((double) tick / 20.0);
            direction = originalDirection.clone().add(new Vector(0.0, t * t * G, 0.0));
        }
        location.add(direction);

        if(particle){
            match.spawnParticle(new NormalParticle(Particle.FLAME, 0, 0.0, 0.0, 0.0, 1), location);
            match.spawnParticle(new NormalParticle(Particle.CAMPFIRE_COSY_SMOKE, 0, 0.0, 0.0, 0.0, 1), location);
            match.spawnParticle(new NormalParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, 0, 0.0, 0.0, 0.0, 1), location);
        }
        
        if(hitLocation != null){
    
            ShellCaseWeapon.createExplosion(shooter, gadgetWeapon, hitLocation, 5.0, 0.5);
            this.remove();
            
        }

        tick++;
    }

    @Override
    public int getEntityID() {
        return 0;
    }

    @Override
    public void spawn() {
        for(ShellCasePlayer ShellCasePlayer : match.getPlayers()) {
            Player player = ShellCasePlayer.getBukkitPlayer();
            if (player == null) continue;

            Location playerLoc = ShellCasePlayer.getLocation();
            if (LocationUtil.distanceSquaredSafeDifferentWorld(playerLoc, location) > ENTITY_DRAW_DISTANCE_SQUARE) continue;

            showPlayer.add(ShellCasePlayer);
        }
        this.tick();
        match.getAsyncEntities().add(this);
    }

    @Override
    public void remove() {
        this.isDead = true;
        match.getAsyncEntities().remove(this);
    }

    @Override
    public boolean isDead() {
        return this.isDead;
    }
}
