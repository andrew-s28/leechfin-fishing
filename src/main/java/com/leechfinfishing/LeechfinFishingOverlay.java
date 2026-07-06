package com.leechfinfishing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class LeechfinFishingOverlay extends Overlay
{
	Client client;

	LeechfinFishingPlugin plugin;

	LeechfinFishingConfig config;

	@Inject
	public LeechfinFishingOverlay(Client client, LeechfinFishingPlugin plugin, LeechfinFishingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}

		// only run renderer if player is on one of the leechfin bridges
		if (
			!playerLocation.isInArea(LeechfinFishingPlugin.LEECHFIN_WEST)
			&& !playerLocation.isInArea(LeechfinFishingPlugin.LEECHFIN_EAST)
		)
		{
			return null;
		}

		// render only the central tile when not actively fishing
		if (!LeechfinFishingPlugin.isLeechfinFishing(client))
		{
			renderLeechfinFishingSpotTile(graphics, worldView);
		}

		// render leechfin tiles only when actively fishing
		if (LeechfinFishingPlugin.isLeechfinFishing(client))
		{
			renderLeechfinTiles(graphics, worldView);
		}

		return null;
	}

	private void renderLeechfinFishingSpotTile(Graphics2D graphics, WorldView worldView)
	{
		GameObject leechfinFishingSpot = LeechfinFishingPlugin.getLeechfinFishingSpot(client, worldView);
		if (leechfinFishingSpot == null)
		{
			return;
		}
		LocalPoint tileToHighlight = new LocalPoint(leechfinFishingSpot.getX(), leechfinFishingSpot.getY(), worldView);

		if (LeechfinFishingPlugin.isInventoryFull(client))
		{
			renderTileOverlay(graphics, tileToHighlight, config.fullInventoryColor());
		} else {
			renderTileOverlay(graphics, tileToHighlight, config.leechfinSpotColor());
		}
	}

	private void renderLeechfinTiles(Graphics2D graphics, WorldView worldView)
	{
		GameObject leechfinFishingSpot = LeechfinFishingPlugin.getLeechfinFishingSpot(client, worldView);
		if (leechfinFishingSpot == null)
		{
			return;
		}
		LocalPoint leechfinFishingSpotPoint = leechfinFishingSpot.getLocalLocation();

		if (config.highlightActiveTile() && LeechfinFishingPlugin.activeLeechfin != null)
		{
			LocalPoint activeLeechfinPoint = LeechfinFishingPlugin.getLeechfinPoint(worldView, LeechfinFishingPlugin.activeLeechfin);
			if (activeLeechfinPoint != null){
				int dy = Math.abs(activeLeechfinPoint.getY() - leechfinFishingSpotPoint.getY());
				if (dy < 5 * 128)
				{
					LocalPoint tileToHighlight = new LocalPoint(activeLeechfinPoint.getX(), leechfinFishingSpotPoint.getY(), worldView);
					renderTileOverlay(graphics, tileToHighlight, config.activeHighlightColor(), config.activeFillColor());
				}
			}
		}

		if (config.highlightNextTile() && LeechfinFishingPlugin.nextLeechfin != null)
		{
			LocalPoint nextLeechfinPoint = LeechfinFishingPlugin.getLeechfinPoint(worldView, LeechfinFishingPlugin.nextLeechfin);
			if (nextLeechfinPoint != null)
			{
				int dy = Math.abs(nextLeechfinPoint.getY() - leechfinFishingSpotPoint.getY());
				if (dy < 5 * 128)
				{
					LocalPoint tileToHighlight = new LocalPoint(nextLeechfinPoint.getX(), leechfinFishingSpotPoint.dy(128).getY(), worldView);
					renderTileOverlay(graphics, tileToHighlight, config.nextHighlightColor(), config.nextFillColor());
				}
			}
		}
	}

	private void renderTileOverlay(Graphics2D graphics, LocalPoint localPoint, Color color)
	{
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null)
		{
			return;
		}

		OverlayUtil.renderPolygon(graphics, tilePoly, color);
	}

	private void renderTileOverlay(Graphics2D graphics, LocalPoint localPoint, Color color, Color fillColor)
	{
		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly == null)
		{
			return;
		}

		OverlayUtil.renderPolygon(graphics, tilePoly, color, fillColor, new BasicStroke(2));
	}
}
