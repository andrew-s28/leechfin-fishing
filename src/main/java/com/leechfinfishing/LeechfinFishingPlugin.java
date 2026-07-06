package com.leechfinfishing;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Leechfin Fishing",
	description = "Highlighting for leechfin fishing.",
	tags = {"highlight", "fishing"}
)
public class LeechfinFishingPlugin extends Plugin
{
	public static final Logger log = LoggerFactory.getLogger(LeechfinFishingPlugin.class);

	// manually coded ids until api updated
	// varbit used to determine if actively fishing leechfin spot
	public static final int ACTIVE_VARBIT_ID = 15454;
	// leechfin projectile and leechfin fishing spot game object ids
	public static final int LEECHFIN_ID = 3992;
	public static final int LEECHFIN_FISHING_SPOT_ID = 62193;

	private static final int CLIENT_TICKS_PER_GAME_TICK = Constants.GAME_TICK_LENGTH / Constants.CLIENT_TICK_LENGTH;
	private static final int MAX_NORTH_DISTANCE = 5 * Perspective.LOCAL_TILE_SIZE;
	private static final int MAX_X_OFFSET = 2 * Perspective.LOCAL_TILE_SIZE;

	// areas to use for activating plugin, covers the bridges
	public static final WorldArea LEECHFIN_WEST = new WorldArea(2707, 7817, 6, 6, 0);
	public static final WorldArea LEECHFIN_EAST = new WorldArea(2718, 7823, 6, 6, 0);

	private @Getter List<LocalPoint> leechfinFishingPoints;
	private @Getter LocalPoint closestLeechfinFishingPoint;
	private @Getter Projectile activeLeechfin;
	private @Getter LocalPoint activeLeechfinPoint;
	private @Getter Projectile nextLeechfin;
	private @Getter LocalPoint nextLeechfinPoint;


	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private LeechfinFishingConfig config;

	@Inject
	private LeechfinFishingOverlay overlay;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Leechfin Fishing started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Leechfin Fishing stopped!");
		overlayManager.remove(overlay);
	}

	@Provides
	LeechfinFishingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LeechfinFishingConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		WorldView worldView = client.getTopLevelWorldView();

		leechfinFishingPoints = findLeechfinFishingPoints(worldView);
		// no fishing spots found
		if (leechfinFishingPoints.isEmpty())
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		closestLeechfinFishingPoint = findClosestLeechfinFishingPoint(client.getLocalPlayer());
		if (closestLeechfinFishingPoint == null)
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		List<Projectile> leechfin = getSortedLeechfin();
		// no leechfin on screen
		if (leechfin.isEmpty())
		{
			activeLeechfin = null;
			activeLeechfinPoint = null;
			nextLeechfin = null;
			nextLeechfinPoint = null;
			return;
		}

		// ensure active is still valid, set if not
		if (activeLeechfin == null || !leechfin.contains(activeLeechfin))
		{
			activeLeechfin = leechfin.get(0);
		}
		activeLeechfinPoint = findLeechfinPoint(worldView, activeLeechfin);

		// now find next leechfin with a different x position
		nextLeechfin = findNextLeechfin(activeLeechfin, leechfin);
		nextLeechfinPoint = findLeechfinPoint(worldView, nextLeechfin);
	}

	private List<Projectile> getSortedLeechfin()
	{
		List<Projectile> list = new ArrayList<>();
		for (Projectile leechfinProjectile : client.getProjectiles())
		{
			if (!isValidLeechfin(leechfinProjectile))
			{
				continue;
			}

			list.add(leechfinProjectile);
		}

		list.sort(Comparator.comparingDouble(Projectile::getRemainingCycles));
		return list;
	}

	private boolean isValidLeechfin(Projectile leechfinProjectile)
	{
		if (leechfinProjectile.getId() != LEECHFIN_ID)
		{
			return false;
		}
		LocalPoint pLocalSourcePoint = LocalPoint.fromWorld(client, leechfinProjectile.getSourcePoint());
		if (pLocalSourcePoint == null)
		{
			return false;
		}
		// if source is more than 5 tiles north, don't include
		if (pLocalSourcePoint.getY() > closestLeechfinFishingPoint.dy(MAX_NORTH_DISTANCE).getY())
		{
			return false;
		}
		// if source is more 2 tiles east or west, don't include
		if (Math.abs(pLocalSourcePoint.getX() - closestLeechfinFishingPoint.getX()) > MAX_X_OFFSET)
		{
			return false;
		}
		// if remaining cycles is less than a tick, don't include, it's too late to click
		if (leechfinProjectile.getRemainingCycles() <= CLIENT_TICKS_PER_GAME_TICK)
		{
			return false;
		}
		return true;
	}

	private Projectile findNextLeechfin(Projectile active, List<Projectile> list)
	{
		if (active == null)
		{
			return list.isEmpty() ? null : list.get(0);
		}

		int idx = list.indexOf(active);

		// if list does not contain active
		if (idx == -1)
		{
			return list.isEmpty() ? null : list.get(0);
		}
		// if active is last list index, next would be out of range, so just return active
		if (idx + 1 >= list.size())
		{
			return null;
		}
		// if next has the same x as active, skip to the next after that
		if (list.get(idx).getSourcePoint().getX() == list.get(idx + 1).getSourcePoint().getX())
		{
			return findNextLeechfin(list.get(idx + 1), list);
		}

		return list.get(idx + 1);
	}

	public static boolean isInventoryFull(Client client)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		return inventory != null && inventory.size() <= inventory.count();
	}

	public boolean isLeechfinFishing()
	{
		return client.getVarbitValue(LeechfinFishingPlugin.ACTIVE_VARBIT_ID) == 1;
	}

	private LocalPoint findLeechfinPoint(WorldView worldView, Projectile projectile)
	{
		if (projectile == null)
		{
			return null;
		}
		int projectileId = projectile.getId();
		if (projectileId != LEECHFIN_ID)
		{
			return null;
		}

		int x = (int) projectile.getX();
		int y = (int) projectile.getY();

		return new LocalPoint(x, y, worldView);
	}

	private LocalPoint findClosestLeechfinFishingPoint(Player player)
	{
		if (player == null || leechfinFishingPoints.isEmpty())
		{
			return null;
		}

		LocalPoint playerLocation = player.getLocalLocation();

		return leechfinFishingPoints.stream()
			.min(Comparator.comparingInt(
				spot -> spot.distanceTo(playerLocation)
			))
			.orElse(null);
	}

	private List<LocalPoint> findLeechfinFishingPoints(WorldView worldview)
	{
		List<LocalPoint> leechfinFishingSpots = new ArrayList<>();

		Tile[][][] tiles = worldview.getScene().getTiles();
		int z = worldview.getPlane();

		for (int x = 0; x < tiles[z].length; ++x)
		{
			for (int y = 0; y < tiles[z][x].length; ++y)
			{
				Tile tile = tiles[z][x][y];
				if (tile == null)
				{
					continue;
				}

				GameObject[] gameObjects = tile.getGameObjects();
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject != null && gameObject.getId() == LEECHFIN_FISHING_SPOT_ID)
					{
						leechfinFishingSpots.add(gameObject.getLocalLocation());
					}
				}
			}
		}
		return leechfinFishingSpots;
	}
}
