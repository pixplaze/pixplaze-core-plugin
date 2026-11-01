package com.pixplaze.api.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkinHeadsTest {

    private static final int FACE = 0xFFFF0000;
    private static final int HAT = 0xFF0000FF;

    @Test
    @DisplayName("Head is the 8×8 face with the hat layer drawn over it")
    void cropsFaceWithHat() throws IOException {
        final var skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int x = 8; x < 16; x++) {
            for (int y = 8; y < 16; y++) {
                skin.setRGB(x, y, FACE);
            }
        }
        // One opaque hat pixel over the top-left corner of the face; the rest of the hat is transparent
        skin.setRGB(40, 8, HAT);

        final var head = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(SkinHeads.crop(png(skin)))));

        assertEquals(8, head.getWidth());
        assertEquals(8, head.getHeight());
        assertEquals(HAT, head.getRGB(0, 0));
        assertEquals(FACE, head.getRGB(7, 7));
    }

    @Test
    @DisplayName("An image smaller than a skin texture is rejected")
    void rejectsNonSkin() throws IOException {
        final var tiny = png(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

        assertThrows(IllegalStateException.class, () -> SkinHeads.crop(tiny));
    }

    private static byte[] png(BufferedImage image) throws IOException {
        final var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
