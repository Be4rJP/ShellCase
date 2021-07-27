package be4rjp.shellcase.packet.manager;

import be4rjp.shellcase.player.ShellCasePlayer;
import be4rjp.shellcase.util.ShellCaseScoreboard;
import net.minecraft.server.v1_15_R1.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_15_R1.ScoreboardServer;

import java.lang.reflect.Field;

public class ScoreUpdatePacketManager {
    
    private static Field a;
    private static Field d;
    
    static {
        try{
            a = PacketPlayOutScoreboardScore.class.getDeclaredField("a");
            d = PacketPlayOutScoreboardScore.class.getDeclaredField("d");
            a.setAccessible(true);
            d.setAccessible(true);
        }catch (Exception e){e.printStackTrace();}
    }
    
    
    public static boolean write(PacketPlayOutScoreboardScore scorePacket, ShellCasePlayer shellCasePlayer){
        try {
            if(d.get(scorePacket) == ScoreboardServer.Action.REMOVE){
                return true;
            }
            
            String line = (String) a.get(scorePacket);
            
            if(shellCasePlayer.getScoreBoard() == null) return true;
            if(!line.contains("ShellCaseSB")) return true;
            
            ShellCaseScoreboard scoreboard = shellCasePlayer.getScoreBoard();
            int index = Integer.parseInt(line.replace("ShellCaseSB", ""));
            
            String newLine = scoreboard.getSidebarLine(shellCasePlayer, index);
            if(newLine == null) return false;
            
            a.set(scorePacket, newLine);
            return true;
        }catch (Exception e){e.printStackTrace();}
        return true;
    }
    
}
