package org.scourge.util;

import com.ardor3d.image.util.AWTImageLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * User: gabor
 * Date: 1/9/11
 * Time: 1:55 PM
 */
public class ImageUtil {
    public static BufferedImage getBufferedImage(ImageIcon icon) {
        Image image = icon.getImage();
        BufferedImage buffImage =
          new BufferedImage(
              image.getWidth(null),
              image.getHeight(null),
              BufferedImage.TYPE_INT_ARGB);

        // Draw Image into BufferedImage
        Graphics g = buffImage.getGraphics();
        g.drawImage(image, 0, 0, null);
//        g.dispose();
        return buffImage;
    }

    public static com.ardor3d.image.Image getArdorImage(ImageIcon icon, boolean flip) {
        return AWTImageLoader.makeArdor3dImage(getBufferedImage(icon), flip);
    }
}
