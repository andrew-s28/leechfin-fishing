package com.leechfinfishing;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.Projectile;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
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

	public static final int ACTIVE_VARBIT_ID = 15454;
	// radius within which to consider a valid leechfin fishing spot
	public static final int SEARCH_RADIUS = 5;
	public static final WorldArea LEECHFIN_WEST = new WorldArea(2707, 7817, 6, 6, 0);
	public static final WorldArea LEECHFIN_EAST = new WorldArea(2718, 7823, 6, 6, 0);
	public static final int LeechfinID = 3992;
	public static final int LeechfinFishingSpotID = 62193;
	public static Projectile activeLeechfin;
	public static Projectile nextLeechfin;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private LeechfinFishingConfig config;

	@Inject
	private LeechfinFishingOverlay overlay;

	@Override
	protected void startUp()
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
		List<Projectile> leechfin = getSortedLeechfin(client.getTopLevelWorldView());
		if (leechfin == null)
		{
			return;
		}

		if (leechfin.isEmpty())
		{
			activeLeechfin = null;
			nextLeechfin = null;
			return;
		}

		// ensure active is still valid
		if (activeLeechfin == null || !leechfin.contains(activeLeechfin))
		{
			activeLeechfin = leechfin.get(0);
		}

		nextLeechfin = findNextLeechfin(activeLeechfin, leechfin);
	}

	private List<Projectile> getSortedLeechfin(WorldView worldView)
	{
		List<Projectile> list = new ArrayList<>();
		GameObject leechfinFishingSpot = getLeechfinFishingSpot(client, worldView);
		if (leechfinFishingSpot == null)
		{
			return null;
		}
		LocalPoint leechfinFishingSpotPoint = leechfinFishingSpot.getLocalLocation();

		for (Projectile p : client.getProjectiles())
		{
			if (p.getId() != LeechfinFishingPlugin.LeechfinID)
			{
				continue;
			}
			LocalPoint pLocalPoint = LocalPoint.fromWorld(client, p.getSourcePoint());
			if (pLocalPoint == null)
			{
				continue;
			}
			if (pLocalPoint.getY() >= leechfinFishingSpotPoint.dy(128 * 5 + 1).getY())
			{
				continue;
			}
			if (Math.abs(pLocalPoint.getX() - leechfinFishingSpotPoint.getX()) > 2 * 128)
			{
				continue;
			}
			// 30 game cycles to a game tick
			if (p.getRemainingCycles() <= 30)
			{
				continue;
			}

			list.add(p);
		}

		list.sort(Comparator.comparingDouble(Projectile::getRemainingCycles));
		return list;
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

		if (list.get(idx).getSourcePoint().getX() == list.get(idx + 1).getSourcePoint().getX())
		{
			return findNextLeechfin(list.get(idx + 1), list);
		}

		return list.get(idx + 1);
	}

	public static boolean isInventoryFull(Client client)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		boolean inventoryFull = false;
		if (inventory != null)
		{
			int freeSlots = inventory.size() - inventory.count();
			if (freeSlots == 0) {
				inventoryFull = true;
			}
		}
		return inventoryFull;
	}

	public static boolean isLeechfinFishing(Client client)
	{
		return client.getVarbitValue(LeechfinFishingPlugin.ACTIVE_VARBIT_ID) == 1;
	}

	public static LocalPoint getLeechfinPoint(WorldView worldView, Projectile projectile)
	{
		int projectileId = projectile.getId();
		if (projectileId != LeechfinFishingPlugin.LeechfinID)
		{
			return null;
		}

		int x = (int) projectile.getX();
		int y = (int) projectile.getY();

		return new LocalPoint(x, y, worldView);
	}

	public static GameObject getLeechfinFishingSpot(Client client, WorldView worldview)
	{
		Scene scene = worldview.getScene();
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		Tile[][][] tiles = scene.getTiles();
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
				if (tile.getWorldLocation().distanceTo(playerLocation) >= LeechfinFishingPlugin.SEARCH_RADIUS)
				{
					continue;
				}

				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject gameObject : gameObjects)
					{
						if (gameObject != null)
						{
							if (gameObject.getId() == LeechfinFishingPlugin.LeechfinFishingSpotID)
							{
								return gameObject;
							}
						}
					}
				}
			}
		}
		return null;
	}

}
