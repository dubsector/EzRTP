package com.skyblockexp.ezrtp.teleport.heatmap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import com.skyblockexp.ezrtp.util.ItemFlagUtil;
import com.skyblockexp.ezrtp.util.MessageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.List;

/**
 * Service for creating and managing heatmap maps.
 * Handles the conversion of heatmap data into Minecraft map items.
 */
public final class HeatmapMapService {
    
    private static final int MIN_DATA_THRESHOLD = 5;
    private final Logger logger;
    
    /**
     * Creates a new heatmap map service.
     * 
     * @param logger the logger for logging messages
     */
    public HeatmapMapService(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Creates a map item with the heatmap visualization.
     * 
     * @param heatmapData the heatmap data to visualize
     * @param player the player who will receive the map
     * @return the map item, or null if creation failed
     */
    public ItemStack createHeatmapMap(HeatmapGenerator.HeatmapData heatmapData, Player player, int centerX, int centerZ, int radius) {
        return createHeatmapMap(heatmapData, player, centerX, centerZ, radius, Collections.emptyList(), ClaimOverlaySettings.defaults());
    }

    public ItemStack createHeatmapMap(HeatmapGenerator.HeatmapData heatmapData,
                                      Player player,
                                      int centerX,
                                      int centerZ,
                                      int radius,
                                      List<ClaimChunkOverlay> claimOverlays,
                                      ClaimOverlaySettings overlaySettings) {
        if (heatmapData == null || heatmapData.getTotalLocations() < MIN_DATA_THRESHOLD) {
            return null;
        }
        try {
            HeatmapImageGenerator imageGenerator = new HeatmapImageGenerator();
            BufferedImage heatmapImage = imageGenerator.generate(heatmapData, centerX, centerZ, radius, claimOverlays, overlaySettings);
            MapView mapView = Bukkit.createMap(player.getWorld());
            for (MapRenderer renderer : mapView.getRenderers()) {
                mapView.removeRenderer(renderer);
            }
            HeatmapMapRenderer heatmapRenderer = new HeatmapMapRenderer(heatmapImage);
            mapView.addRenderer(heatmapRenderer);
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
            if (mapMeta != null) {
                mapMeta.setMapView(mapView);
                com.skyblockexp.ezrtp.util.compat.ItemMetaCompat.setDisplayName(mapMeta,
                    MessageUtil.legacyToComponent("&6&lRTP Heatmap"));
                java.util.List<net.kyori.adventure.text.Component> lore = java.util.Arrays.asList(
                    MessageUtil.legacyToComponent("&7Total Locations: &f" + heatmapData.getTotalLocations()),
                    MessageUtil.legacyToComponent("&7Grid Cells: &f" + heatmapData.getOccupiedCells()),
                    MessageUtil.legacyToComponent("&7Grid Size: &f" + heatmapData.getGridSize() + " blocks")
                );
                com.skyblockexp.ezrtp.util.compat.ItemMetaCompat.setLore(mapMeta, lore);
                ItemFlagUtil.setItemMetaCompatibly(mapItem, mapMeta);
            }
            return mapItem;
        } catch (Exception e) {
            logger.warning("Failed to create heatmap map: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Saves a heatmap as a PNG file.
     * 
     * @param heatmapData the heatmap data to save
     * @param outputFile the file to save to
     * @return true if successful, false otherwise
     */
    public boolean saveHeatmapAsPng(HeatmapGenerator.HeatmapData heatmapData, File outputFile, int centerX, int centerZ, int radius) {
        return saveHeatmapAsPng(heatmapData, outputFile, centerX, centerZ, radius, Collections.emptyList(), ClaimOverlaySettings.defaults());
    }

    public boolean saveHeatmapAsPng(HeatmapGenerator.HeatmapData heatmapData,
                                    File outputFile,
                                    int centerX,
                                    int centerZ,
                                    int radius,
                                    List<ClaimChunkOverlay> claimOverlays,
                                    ClaimOverlaySettings overlaySettings) {
        if (heatmapData == null || heatmapData.getTotalLocations() < MIN_DATA_THRESHOLD) {
            return false;
        }
        try {
            HeatmapImageGenerator imageGenerator = new HeatmapImageGenerator();
            BufferedImage heatmapImage = imageGenerator.generate(heatmapData, centerX, centerZ, radius, claimOverlays, overlaySettings);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            ImageIO.write(heatmapImage, "PNG", outputFile);
            logger.info("Heatmap saved to: " + outputFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.warning("Failed to save heatmap PNG: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if there is enough data to generate a meaningful heatmap.
     * 
     * @param heatmapData the heatmap data to check
     * @return true if there is enough data, false otherwise
     */
    public boolean hasEnoughData(HeatmapGenerator.HeatmapData heatmapData) {
        return heatmapData != null && heatmapData.getTotalLocations() >= MIN_DATA_THRESHOLD;
    }
    
    /**
     * Returns the minimum data threshold for generating heatmaps.
     */
    public int getMinDataThreshold() {
        return MIN_DATA_THRESHOLD;
    }
}
