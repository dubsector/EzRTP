package com.skyblockexp.ezrtp.teleport.heatmap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generates heatmap images from heatmap data.
 * Converts grid-based density data into colored pixel images suitable for Minecraft maps.
 */
public final class HeatmapImageGenerator {
    
    // Default Minecraft map size is 128x128 pixels
    private static final int DEFAULT_MAP_SIZE = 128;
    
    // Yellow to Red gradient (easier to see)
    private static final Color[] HEATMAP_COLORS = {
        new Color(253, 231, 37),   // Yellow (low)
        new Color(255, 153, 51),   // Orange
        new Color(255, 69, 0),     // Orange-Red
        new Color(220, 0, 0),      // Red
        new Color(128, 0, 0)       // Dark Red (high)
    };
    
    private final int mapSize;
    
    /**
     * Creates a heatmap image generator with the default map size (128x128).
     */
    public HeatmapImageGenerator() {
        this(DEFAULT_MAP_SIZE);
    }
    
    /**
     * Creates a heatmap image generator with a custom map size.
     * 
     * @param mapSize the size of the map in pixels (width and height)
     */
    public HeatmapImageGenerator(int mapSize) {
        if (mapSize <= 0 || mapSize > 1024) {
            throw new IllegalArgumentException("Map size must be between 1 and 1024 pixels");
        }
        this.mapSize = mapSize;
    }
    
    /**
     * Generates a heatmap image from the given heatmap data.
     * 
     * @param heatmapData the heatmap data to visualize
     * @return a BufferedImage containing the heatmap visualization
     */
    /**
     * Generates a heatmap image from the given heatmap data, centered on the given world coordinates and radius.
     * @param heatmapData the heatmap data to visualize
     * @param centerX the world X coordinate to center the image on
     * @param centerZ the world Z coordinate to center the image on
     * @param radius the world radius to display (half-width/height of the image in blocks)
     * @return a BufferedImage containing the heatmap visualization
     */
    public BufferedImage generate(HeatmapGenerator.HeatmapData heatmapData, int centerX, int centerZ, int radius) {
        return generate(heatmapData, centerX, centerZ, radius, Collections.emptyList(), ClaimOverlaySettings.defaults());
    }

    public BufferedImage generate(HeatmapGenerator.HeatmapData heatmapData,
                                  int centerX,
                                  int centerZ,
                                  int radius,
                                  List<ClaimChunkOverlay> claimChunks,
                                  ClaimOverlaySettings overlaySettings) {
        if (heatmapData == null || heatmapData.getTotalLocations() == 0) {
            return createEmptyMap();
        }
        Map<HeatmapGenerator.GridCell, Integer> data = heatmapData.getData();
        if (data.isEmpty()) {
            return createEmptyMap();
        }
        int gridSize = heatmapData.getGridSize();
        // Compute bounds of all data
        int minCellX = Integer.MAX_VALUE, maxCellX = Integer.MIN_VALUE;
        int minCellZ = Integer.MAX_VALUE, maxCellZ = Integer.MIN_VALUE;
        for (HeatmapGenerator.GridCell cell : data.keySet()) {
            if (cell.x() < minCellX) minCellX = cell.x();
            if (cell.x() > maxCellX) maxCellX = cell.x();
            if (cell.z() < minCellZ) minCellZ = cell.z();
            if (cell.z() > maxCellZ) maxCellZ = cell.z();
        }
        int dataMinX = minCellX * gridSize;
        int dataMaxX = (maxCellX + 1) * gridSize - 1;
        int dataMinZ = minCellZ * gridSize;
        int dataMaxZ = (maxCellZ + 1) * gridSize - 1;
        // If all data is outside the requested bounds, or if radius is 0, auto-fit to data
        boolean autoFit = radius <= 0
            || dataMinX < (centerX - radius) || dataMaxX > (centerX + radius)
            || dataMinZ < (centerZ - radius) || dataMaxZ > (centerZ + radius);
        int minWorldX, maxWorldX, minWorldZ, maxWorldZ;
        if (autoFit) {
            minWorldX = dataMinX;
            maxWorldX = dataMaxX;
            minWorldZ = dataMinZ;
            maxWorldZ = dataMaxZ;
        } else {
            minWorldX = centerX - radius;
            maxWorldX = centerX + radius;
            minWorldZ = centerZ - radius;
            maxWorldZ = centerZ + radius;
        }
        // Create the image
        BufferedImage image = new BufferedImage(mapSize, mapSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(0, 0, mapSize, mapSize);
        int maxCount = heatmapData.getMaxCount();
        boolean allSingle = maxCount == 1;
        for (Map.Entry<HeatmapGenerator.GridCell, Integer> entry : data.entrySet()) {
            HeatmapGenerator.GridCell cell = entry.getKey();
            int count = entry.getValue();
            // Convert cell to world coordinates (center of cell)
            int cellWorldX = cell.x() * gridSize + gridSize / 2;
            int cellWorldZ = cell.z() * gridSize + gridSize / 2;
            // Map world coordinates to image pixels
            double relX = (double) (cellWorldX - minWorldX) / Math.max(1, (maxWorldX - minWorldX));
            double relZ = (double) (cellWorldZ - minWorldZ) / Math.max(1, (maxWorldZ - minWorldZ));
            int pixelX = (int) (relX * mapSize);
            int pixelZ = (int) (relZ * mapSize);
            int pixelWidth = Math.max(1, (int) Math.ceil((double) mapSize * gridSize / Math.max(1, (maxWorldX - minWorldX))));
            int pixelHeight = Math.max(1, (int) Math.ceil((double) mapSize * gridSize / Math.max(1, (maxWorldZ - minWorldZ))));
            pixelWidth = Math.min(pixelWidth, mapSize - pixelX);
            pixelHeight = Math.min(pixelHeight, mapSize - pixelZ);
            double intensity = allSingle ? 0.0 : (double) (count - 1) / (maxCount - 1);
            Color color = getColorForIntensity(intensity);
            if (pixelWidth > 0 && pixelHeight > 0) {
                int alpha = (int) (50 + 205 * intensity);
                Color alphaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                g2d.setColor(alphaColor);
                g2d.fillRect(pixelX, pixelZ, pixelWidth, pixelHeight);
            }
        }
        drawLegend(g2d, mapSize);
        drawClaimOverlays(g2d, minWorldX, maxWorldX, minWorldZ, maxWorldZ,
                claimChunks != null ? claimChunks : Collections.emptyList(),
                overlaySettings != null ? overlaySettings : ClaimOverlaySettings.defaults());
        g2d.dispose();
        return image;
    }

    private void drawClaimOverlays(Graphics2D g2d,
                                   int minWorldX,
                                   int maxWorldX,
                                   int minWorldZ,
                                   int maxWorldZ,
                                   List<ClaimChunkOverlay> claimChunks,
                                   ClaimOverlaySettings settings) {
        if (claimChunks.isEmpty() || !settings.enabled() || settings.style() != ClaimOverlayStyle.BORDER) {
            return;
        }

        g2d.setColor(settings.color());
        g2d.setStroke(new BasicStroke(settings.lineWidth()));
        int worldWidth = Math.max(1, maxWorldX - minWorldX);
        int worldHeight = Math.max(1, maxWorldZ - minWorldZ);

        for (ClaimChunkOverlay chunk : claimChunks) {
            int chunkMinX = chunk.chunkX() * 16;
            int chunkMaxX = chunkMinX + 16;
            int chunkMinZ = chunk.chunkZ() * 16;
            int chunkMaxZ = chunkMinZ + 16;

            double relMinX = (double) (chunkMinX - minWorldX) / worldWidth;
            double relMaxX = (double) (chunkMaxX - minWorldX) / worldWidth;
            double relMinZ = (double) (chunkMinZ - minWorldZ) / worldHeight;
            double relMaxZ = (double) (chunkMaxZ - minWorldZ) / worldHeight;

            int px = Math.max(0, Math.min(mapSize - 1, (int) Math.round(relMinX * mapSize)));
            int pz = Math.max(0, Math.min(mapSize - 1, (int) Math.round(relMinZ * mapSize)));
            int pw = Math.max(1, (int) Math.round((relMaxX - relMinX) * mapSize));
            int ph = Math.max(1, (int) Math.round((relMaxZ - relMinZ) * mapSize));
            if (px < mapSize && pz < mapSize) {
                int maxWidth = mapSize - px - 1;
                int maxHeight = mapSize - pz - 1;
                g2d.drawRect(px, pz, Math.max(1, Math.min(pw, maxWidth)), Math.max(1, Math.min(ph, maxHeight)));
            }
        }
    }
    
    /**
     * Creates an empty/blank map image.
     */
    private BufferedImage createEmptyMap() {
        BufferedImage image = new BufferedImage(mapSize, mapSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(20, 20, 30));
        g2d.fillRect(0, 0, mapSize, mapSize);
        g2d.dispose();
        return image;
    }
    
    /**
     * Gets the color for a given intensity value (0.0 to 1.0).
     * Uses linear interpolation between predefined color stops.
     */
    private Color getColorForIntensity(double intensity) {
        if (intensity <= 0.0) {
            return HEATMAP_COLORS[0];
        }
        if (intensity >= 1.0) {
            return HEATMAP_COLORS[HEATMAP_COLORS.length - 1];
        }
        
        // Find the two colors to interpolate between
        double scaledIntensity = intensity * (HEATMAP_COLORS.length - 1);
        int index = (int) Math.floor(scaledIntensity);
        double fraction = scaledIntensity - index;
        
        if (index >= HEATMAP_COLORS.length - 1) {
            return HEATMAP_COLORS[HEATMAP_COLORS.length - 1];
        }
        
        Color color1 = HEATMAP_COLORS[index];
        Color color2 = HEATMAP_COLORS[index + 1];
        
        // Linear interpolation between the two colors
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        
        return new Color(r, g, b);
    }
    
    /**
     * Returns the map size in pixels.
     */
    public int getMapSize() {
        return mapSize;
    }

    /**
     * Draws a color legend on the heatmap image.
     */
    private void drawLegend(Graphics2D g2d, int mapSize) {
        int legendWidth = 80;
        int legendHeight = 12;
        int x = mapSize - legendWidth - 8;
        int y = mapSize - legendHeight - 8;
        for (int i = 0; i < legendWidth; i++) {
            double t = (double) i / (legendWidth - 1);
            Color c = getColorForIntensity(t);
            g2d.setColor(c);
            g2d.fillRect(x + i, y, 1, legendHeight);
        }
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, legendWidth, legendHeight);
        g2d.setFont(g2d.getFont().deriveFont(9f));
        g2d.drawString("Low", x, y - 2);
        g2d.drawString("High", x + legendWidth - 24, y - 2);
    }
}
