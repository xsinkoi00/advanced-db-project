package dbs.oracle_project.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Byte-array ↔ BufferedImage conversions and in-memory 90° rotation. */
public final class ImageTools {

  private ImageTools() {}

  public static BufferedImage read(byte[] bytes) throws IOException {
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      BufferedImage img = ImageIO.read(in);
      if (img == null) throw new IOException("Unsupported or corrupt image bytes");
      return img;
    }
  }

  /** Write to PNG (lossless, no palette issues, Oracle Multimedia reads it). */
  public static byte[] writePng(BufferedImage img) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(img, "png", out);
      return out.toByteArray();
    }
  }

  /** Rotate 90° clockwise or counter-clockwise, as PNG bytes. */
  public static byte[] rotate90(byte[] bytes, boolean clockwise) throws IOException {
    BufferedImage src = read(bytes);
    int w = src.getWidth(), h = src.getHeight();
    BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = dst.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    AffineTransform tx = new AffineTransform();
    if (clockwise) {
      tx.translate(h, 0);
      tx.rotate(Math.PI / 2);
    } else {
      tx.translate(0, w);
      tx.rotate(-Math.PI / 2);
    }
    g.drawImage(src, tx, null);
    g.dispose();
    return writePng(dst);
  }
}
