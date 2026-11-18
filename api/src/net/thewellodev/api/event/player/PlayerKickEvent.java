package net.thewellodev.api.event.player;

import net.thewellodev.api.entity.Player;
import net.thewellodev.api.event.Cancellable;

/** Called before a player is kicked. If cancelled, the player is not kicked. */
public class PlayerKickEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private String reason;
    private String leaveMessage;

    public PlayerKickEvent(Player player, String reason, String leaveMessage) {
        super(player);
        this.reason = reason;
        this.leaveMessage = leaveMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
