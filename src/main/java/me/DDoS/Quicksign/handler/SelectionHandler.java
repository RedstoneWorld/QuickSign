package me.DDoS.Quicksign.handler;

import java.util.Map.Entry;
import java.util.UUID;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import me.DDoS.Quicksign.session.StandardEditSession;
import me.DDoS.Quicksign.session.EditSession;
import me.DDoS.Quicksign.util.QSUtil;
import me.DDoS.Quicksign.permission.Permission;
import me.DDoS.Quicksign.QuickSign;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.Vector;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import static com.sk89q.worldguard.bukkit.BukkitUtil.toVector;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;

import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;

import org.bukkit.block.Block;

/**
 *
 * @author DDoS
 */
@SuppressWarnings("unchecked")
public class SelectionHandler {

    private final QuickSign plugin;
    //
    private WorldGuardPlugin wg = null;
    private LWC lwc = null;
    private boolean chestShop = false;
    private boolean residence = false;

    public SelectionHandler(QuickSign instance) {

        plugin = instance;

    }

    public void setWG(WorldGuardPlugin wg) {

        this.wg = wg;

    }

    public void setLWC(LWC lwc) {

        this.lwc = lwc;

    }

    public void setResidence(boolean residence) {

        this.residence = residence;

    }

    public void setChestShop(boolean chestShop) {
        this.chestShop = chestShop;
    }

    public void handleSignSelection(PlayerInteractEvent event, Sign sign, Player player) {

        if (!plugin.getBlackList().allows(sign, player)) {

            QSUtil.tell(player, "You cannot select this sign.");
            return;

        }

        if (checkForSelectionRights(player, sign.getBlock())) {

            if (event != null) {

                event.setCancelled(true);

            }

            Player owner = getOwner(sign);

            if (owner != null && !owner.equals(player)) {

                QSUtil.tell(player, "This sign is already selected.");
                return;

            }

            StandardEditSession session = (StandardEditSession) plugin.getSession(player);

            if (session.addSign(sign)) {

                QSUtil.tell(player, "Sign " + ChatColor.GREEN + "added " + ChatColor.GRAY + "to selection, "
                        + ChatColor.WHITE + session.getNumberOfSigns() + ChatColor.GRAY + " total.");
                return;

            } else {

                session.removeSign(sign);
                QSUtil.tell(player, "Sign " + ChatColor.RED + "removed " + ChatColor.GRAY + "from selection, "
                        + ChatColor.WHITE + session.getNumberOfSigns() + ChatColor.GRAY + " total.");
                return;

            }

        } else {

            QSUtil.tell(player, "You don't own this sign.");

        }
    }

    private boolean checkForWGMembership(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);

        if (set.size() != 0) {

            return set.isMemberOfAll(localPlayer);

        }

        return false;

    }

    private boolean checkForWGOwnership(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);

        if (set.size() != 0) {

            return set.isOwnerOfAll(localPlayer);

        }

        return false;

    }

    private boolean checkForWGBuildPermissions(Player player, Location location, World world) {

        Vector pt = toVector(location);
        LocalPlayer localPlayer = wg.wrapPlayer(player);
        RegionManager regionManager = wg.getRegionManager(world);
        ApplicableRegionSet set = regionManager.getApplicableRegions(pt);
        return set.canBuild(localPlayer);

    }

    private boolean checkForResidencePerms(World world, Location location, Player player, boolean forceRegion) {

        FlagPermissions.addFlag("build");
        ClaimedResidence res = Residence.getResidenceManager().getByLoc(location);

        if (res == null && !forceRegion) {

            return true;

        }

        if (res == null && forceRegion) {

            return false;

        }

        if (res != null) {

            ResidencePermissions perms = res.getPermissions();
            return perms.playerHas(player.getName(), world.getName(), "build", true);

        }

        return false;

    }

    private boolean checkForLWCPerms(Player player, Block block, boolean forceProtection) {

        Protection protection = lwc.findProtection(block);

        if (protection == null && !forceProtection) {

            return true;

        }

        if (protection == null && forceProtection) {

            return false;

        }

        if (protection != null) {

            return lwc.canAccessProtection(player, protection);

        }

        return false;

    }

    private boolean checkForChestShopPerms(Player player, Sign sign) {

        if (ChestShopSign.isValid(sign)) {
            String ownerName = sign.getLine(ChestShopSign.NAME_LINE);
            UUID ownerId = NameManager.getUUID(ownerName);
            return player.getUniqueId().equals(ownerId);
        };
        return false;

    }

    public boolean checkForSelectionRights(Player player, Block block) {

        Location location = block.getLocation();
        World world = location.getWorld();

        boolean wgPerm = false, residencePerm = false, lwcPerm = false, chestShopPerm = false;

        if (wg == null && !residence && lwc == null && !chestShop) {

            return plugin.hasPermissions(player, Permission.USE);

        }

        if (plugin.hasPermissions(player, Permission.FREE_USE)) {

            return true;

        }

        if (wg != null) {

            if (plugin.hasPermissions(player, Permission.WG_MEMBER)
                    && checkForWGMembership(player, location, world)) {

                wgPerm = true;

            } else if (plugin.hasPermissions(player, Permission.WG_OWNER)
                    && checkForWGOwnership(player, location, world)) {

                wgPerm = true;

            } else if (plugin.hasPermissions(player, Permission.WG_CAN_BUILD)
                    && checkForWGBuildPermissions(player, location, world)) {

                wgPerm = true;

            }
        }

        if (residence) {

            if (plugin.hasPermissions(player, Permission.RS_CAN_BUILD_FP)
                    && checkForResidencePerms(world, location, player, true)) {

                residencePerm = true;

            } else if (plugin.hasPermissions(player, Permission.RS_CAN_BUILD)
                    && checkForResidencePerms(world, location, player, false)) {

                residencePerm = true;

            }
        }

        if (lwc != null) {

            if (plugin.hasPermissions(player, Permission.LWC_CAN_ACCESS_FP)
                    && checkForLWCPerms(player, block, true)) {

                lwcPerm = true;

            } else if (plugin.hasPermissions(player, Permission.LWC_CAN_ACCESS)
                    && checkForLWCPerms(player, block, false)) {

                lwcPerm = true;

            }
        }

        if (chestShop) {

            if (QSUtil.checkForSign(block)) {

                if (plugin.hasPermissions(player, Permission.CHESTSHOP_EDIT)
                        && checkForChestShopPerms(player, (Sign) block.getState())) {

                    chestShopPerm = true;

                }
            }
        }

        return wgPerm && residencePerm && lwcPerm && chestShopPerm;

    }

    private Player getOwner(Sign sign) {

        for (Entry<Player, EditSession> entry : plugin.getSessions()) {

            if (entry.getValue().checkIfSelected(sign)) {

                return entry.getKey();

            }
        }

        return null;

    }
}
