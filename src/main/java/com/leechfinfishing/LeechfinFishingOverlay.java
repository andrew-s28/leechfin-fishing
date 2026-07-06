package com.leechfinfishing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class LeechfinFishingOverlay extends Overlay
{
	private final Client client;

	private final LeechfinFishingPlugin plugin;

	private final LeechfinFishingConfig config;

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

		if (plugin.isLeechfinFishing())
		{
			// render leechfin tiles only when actively fishing
			renderLeechfinTiles(graphics, worldView);
		}
		else
		{
			// render only the central tile when not actively fishing
			renderLeechfinFishingSpotTile(graphics);
		}

		return null;
	}

	private void renderLeechfinFishingSpotTile(Graphics2D graphics)
	{
		Color color = LeechfinFishingPlugin.isInventoryFull(client)
			? config.fullInventoryColor()
			: config.leechfinSpotColor();
		for (LocalPoint leechfinFishingSpot : plugin.getLeechfinFishingPoints())
		{
			renderTileOverlay(
				graphics,
				leechfinFishingSpot,
				color
			);
		}
	}

	private void renderLeechfinTiles(Graphics2D graphics, WorldView worldView)
	{
		if (plugin.getClosestLeechfinFishingPoint() == null)
		{
			return;
		}
		if (config.highlightActiveTile())
		{
			renderLeechfinTileOverlay(
				graphics,
				worldView,
				plugin.getActiveLeechfinPoint(),
				0,
				config.activeHighlightColor(),
				config.activeFillColor()
			);
		}

		if (config.highlightNextTile())
		{
			renderLeechfinTileOverlay(
				graphics,
				worldView,
				plugin.getNextLeechfinPoint(),
				1,
				config.nextHighlightColor(),
				config.nextFillColor()
			);
		}
	}

	private void renderLeechfinTileOverlay(
		Graphics2D graphics,
		WorldView worldView,
		LocalPoint leechfinPoint,
		int yOffset,
		Color highlightColor,
		Color fillColor
	)
	{
		if (leechfinPoint == null)
		{
			return;
		}
		LocalPoint tileToHighlight = new LocalPoint(
			leechfinPoint.getX(),
			plugin.getClosestLeechfinFishingPoint()
				.dy(yOffset * Perspective.LOCAL_TILE_SIZE).
				getY(),
			worldView
		);
		renderTileOverlay(graphics, tileToHighlight, highlightColor, fillColor);

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
