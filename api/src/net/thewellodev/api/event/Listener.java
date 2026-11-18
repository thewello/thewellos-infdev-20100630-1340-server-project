package net.thewellodev.api.event;

/**
 * An event listener. Every public method that has an {@link EventHandler} annotation and takes one
 * {@link Event} subclass parameter is registered automatically:
 *
 * <pre>
 * public class MyListener implements Listener {
 *     &#64;EventHandler
 *     public void onJoin(PlayerJoinEvent e) {
 *         e.getPlayer().sendMessage("Hos geldin!");
 *     }
 * }
 * // inside onEnable:
 * getServer().getPluginManager().registerEvents(new MyListener(), this);
 * </pre>
 */
public interface Listener {
}
