package com.skyblockexp.ezrtp.teleport.heatmap;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeatmapImageGeneratorOverlayTest {

    @Test
    void generateWithClaimOverlayProducesImage() {
        HeatmapGenerator.GridCell cell = new HeatmapGenerator.GridCell(0, 0);
        HeatmapGenerator.HeatmapData data = new HeatmapGenerator.HeatmapData(16, Map.of(cell, 10));
        HeatmapImageGenerator generator = new HeatmapImageGenerator(128);
        BufferedImage image = generator.generate(
                data,
                0,
                0,
                256,
                List.of(new ClaimChunkOverlay("world", 0, 0)),
                new ClaimOverlaySettings(true, ClaimOverlayStyle.BORDER, java.awt.Color.CYAN, 1));
        assertNotNull(image);
        assertEquals(128, image.getWidth());
        assertEquals(128, image.getHeight());
    }
}
