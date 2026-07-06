package com.leechfinfishing;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("leechfinfishing")
public interface LeechfinFishingConfig extends Config
{
	@ConfigSection(
		name = "Active tile",
		description = "Active leechfin tile to be clicked.",
		position = 0
	)
	String activeTile = "activeTile";

	@ConfigSection(
		name = "Next tile",
		description = "The next leechfin tile to be clicked after the active tile.",
		position = 1
	)
	String nextTile = "nextTile";

	@ConfigItem(
		position = 1,
		keyName = "highlightActiveTile",
		name = "Highlight active tile",
		description = "Highlights the active tile for leechfin fishing.",
		section = "activeTile"
	)
	default boolean highlightActiveTile()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "activeHighlightColor",
		name = "Highlight color",
		description = "Highlight color for the active tile.",
		section = "activeTile"
	)
	default Color activeHighlightColor()
	{
		return Color.GREEN;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "activeFillColor",
		name = "Fill color",
		description = "Fill color for the active tile.",
		section = "activeTile"
	)
	default Color activeFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@ConfigItem(
		position = 1,
		keyName = "highlightNextTile",
		name = "Highlight next tile",
		description = "Highlights the next tile for leechfin fishing.",
		section = "nextTile"
	)
	default boolean highlightNextTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "nextHighlightColor",
		name = "Highlight color",
		description = "Highlight color for the active tile.",
		section = "nextTile"
	)
	default Color nextHighlightColor()
	{
		return Color.YELLOW;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "nextFillColor",
		name = "Fill color",
		description = "Fill color for the active tile.",
		section = "nextTile"
	)
	default Color nextFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "leechfinSpotColor",
		name = "Leechfin spot color",
		description = "Color to highlight leechfin fishing spot when not actively fishing."
	)
	default Color leechfinSpotColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "fullInventoryColor",
		name = "Full Inventory Color",
		description = "Color to highlight leechfin fishing spot when inventory is full."
	)
	default Color fullInventoryColor()
	{
		return Color.RED;
	}
}
