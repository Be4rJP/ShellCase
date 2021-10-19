package be4rjp.shellcase.listener;

import be4rjp.shellcase.ShellCase;
import be4rjp.shellcase.ShellCaseConfig;
import be4rjp.shellcase.gui.MainMenuGUI;
import be4rjp.shellcase.map.ConquestPlayerClickableGUIRenderer;
import be4rjp.shellcase.map.ConquestPlayerMapRenderer;
import be4rjp.shellcase.map.PlayerGUIRenderer;
import be4rjp.shellcase.match.ConquestMatch;
import be4rjp.shellcase.match.Match;
import be4rjp.shellcase.match.MatchManager;
import be4rjp.shellcase.match.map.ConquestMap;
import be4rjp.shellcase.match.map.ShellCaseMap;
import be4rjp.shellcase.match.team.ShellCaseColor;
import be4rjp.shellcase.match.team.ShellCaseTeam;
import be4rjp.shellcase.packet.PacketHandler;
import be4rjp.shellcase.player.ShellCasePlayer;
import be4rjp.shellcase.util.TaskHandler;
import be4rjp.shellcase.weapon.attachment.Attachment;
import be4rjp.shellcase.weapon.gadget.Gadget;
import be4rjp.shellcase.weapon.gadget.GadgetStatusData;
import be4rjp.shellcase.weapon.gun.GunStatusData;
import be4rjp.shellcase.weapon.gun.GunWeapon;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.papermc.lib.PaperLib;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.scheduler.BukkitRunnable;
import world.chiyogami.chiyogamilib.scheduler.WorldThreadRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinQuitListener implements Listener {
    
    private static final ConquestMatch match;
    private static final ShellCaseTeam team0;
    private static final ShellCaseTeam team1;
    
    private static int index = 0;
    
    static {
        match = new ConquestMatch(ConquestMap.getRandomConquestMap());
        team0 = new ShellCaseTeam(match, ShellCaseColor.BLUE);
        team1 = new ShellCaseTeam(match, ShellCaseColor.ORANGE);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.teleport(ShellCaseConfig.getJoinLocation());
    
    
        TaskHandler.runAsync(() -> {
            player.setWalkSpeed(0.2F);
            player.getInventory().clear();

            ShellCasePlayer shellCasePlayer = ShellCasePlayer.getShellCasePlayer(player);
            shellCasePlayer.updateBukkitPlayer(player);
            shellCasePlayer.sendSkinRequest();

            shellCasePlayer.loadAchievementFromSQL();
            
            for(int i = 0; i < 4; i++) {
                GunStatusData gunStatusData = new GunStatusData(GunWeapon.getGunWeaponBySaveNumber(i), shellCasePlayer);
                gunStatusData.addAttachment(Attachment.getAttachmentBySaveNumber(1));
                gunStatusData.addAttachment(Attachment.getAttachmentBySaveNumber(2));
                gunStatusData.addAttachment(Attachment.getAttachmentBySaveNumber(3));
                gunStatusData.addAttachment(Attachment.getAttachmentBySaveNumber(4));
                shellCasePlayer.getWeaponPossessionData().setGunStatusData(gunStatusData);
            }
            shellCasePlayer.getWeaponClass().setSubGadget(new GadgetStatusData(Gadget.GRAPPLE_GUN.getInstance(), shellCasePlayer));
            
            shellCasePlayer.giveItems();
            
            
            ShellCaseTeam ShellCaseTeam = shellCasePlayer.getShellCaseTeam();
            
            if(ShellCaseTeam != null) {
                if(ShellCaseTeam == ShellCase.getLobbyTeam()){
                    MatchManager matchManager = shellCasePlayer.getMatchManager();
                    if(matchManager != null){
                        Match match = matchManager.getMatch();
                        if(match != null){
                            shellCasePlayer.teleport(ShellCaseConfig.getLobbyLocation());
                            return;
                        }
                    }
                }
                
                if(ShellCaseTeam != ShellCase.getLobbyTeam()) {
                    Match match = ShellCaseTeam.getMatch();

                    switch (match.getMatchStatus()) {
                        case WAITING: {
                            shellCasePlayer.teleport(ShellCaseConfig.getLobbyLocation());
                            break;
                        }
    
                        case IN_PROGRESS: {
                            match.teleportToTeamLocation(shellCasePlayer);
                            break;
                        }
    
                        default: {
                            shellCasePlayer.reset();
                            shellCasePlayer.teleport(ShellCaseConfig.getLobbyLocation());
                            break;
                        }
                    }
                }
            }
        });
    }
    
    
    @EventHandler
    public void onjoin(PlayerJoinEvent event){
        //Inject packet handler
        Player player = event.getPlayer();
        
        PacketHandler packetHandler = new PacketHandler(player);
        
        try {
            ChannelPipeline pipeline = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel.pipeline();
            pipeline.addBefore("packet_handler", ShellCase.getPlugin().getName() + "PacketInjector:" + player.getName(), packetHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    @EventHandler
    public void onleave(PlayerQuitEvent event){
        Player player = event.getPlayer();
    
        TaskHandler.runAsync(() -> {
            ShellCasePlayer shellCasePlayer = ShellCasePlayer.getShellCasePlayer(player);
            try {
                if(shellCasePlayer.isLoadedSaveData()) shellCasePlayer.saveAchievementToSQL();
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        
        try {
            Channel channel = ((CraftPlayer)player).getHandle().playerConnection.networkManager.channel;
            
            channel.eventLoop().submit(() -> {
                channel.pipeline().remove(ShellCase.getPlugin().getName() + "PacketInjector:" + player.getName());
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        player.teleport(ShellCaseConfig.getJoinLocation());
    }
}
