package com.pechatnov;

import com.vaadin.server.StreamResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.color.ColorSpace;
import java.awt.image.WritableRaster;
import java.io.*;
import java.awt.image.ColorConvertOp;

/**
 * Created by ura on 10.08.16.
 */
public class MImage {
    public final Integer H, W;
    public BufferedImage image, grayImage, blackAndWhiteImage;
    public enum IntenseType {BINARY_INTENSE, GRADUAL_INTENSE};
    IntenseType intenseType = IntenseType.BINARY_INTENSE;

    public static class MImageSource
            implements StreamResource.StreamSource {
        ByteArrayOutputStream imagebuffer = null;
        BufferedImage image = null;

        public InputStream getStream () {
            try {
                imagebuffer = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imagebuffer);

                return new ByteArrayInputStream(
                        imagebuffer.toByteArray());
            } catch (IOException e) {
                return null;
            }
        }
        MImageSource(BufferedImage image_) {
            image = image_;
        }
    }
    /*
    static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    */
    public void setIntenseType(IntenseType intenseType_) {
        intenseType = intenseType_;
    }

    public boolean getPixelBinary(int x, int y) {
        return (image.getRGB(x, y) != -1);
    }
    public int getPixelPercents(int x, int y) {
        int ret = (grayImage.getRGB(x, y) % 256) * 100 / 255;
        if (ret == 0 && getPixelBinary(x, y))
            ret = 1;
        return ret;
    }

    public StreamResource getBlackAndWhiteSource() {
        StreamResource.StreamSource imageSource = new MImageSource(blackAndWhiteImage);
        return new StreamResource(imageSource, "bmp");
    }

    public StreamResource getGraySource() {
        StreamResource.StreamSource imageSource = new MImageSource(grayImage);
        return new StreamResource(imageSource, "bmp");
    }

    public StreamResource getResource() {
        StreamResource.StreamSource imageSource = new MImageSource(image);
        return new StreamResource(imageSource, "bmp");
    }



    public static MImage openImage(String fname) {
        System.err.println("Open image :" + fname);
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fname));
        } catch (Exception e) {
            System.err.println("Problem with opening picture");
            UsefulMethods.notifyException(e);
            return null;
        }
        return new MImage(image);
    }

    MImage(BufferedImage image_) {
        image = image_;
        if (image == null) {
            System.err.println("MImage(BufferedImage image_): null image error!");
            UsefulMethods.printCurrentStackTrace();
            H = 0;
            W = 0;
            return;
        }
        H = image.getHeight();
        W = image.getWidth();
        /*try {
            ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
            op.filter(image, grayImage);
            System.err.println("grayImageSize: " + grayImage.getWidth() + "x" + grayImage.getHeight());
        } catch (Exception e) {
            UsefulMethods.notifyException(e);
        }*/
        try {
            blackAndWhiteImage = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_BINARY);
            grayImage = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
            for (int x = 0; x < W; x++)
                for (int y = 0; y < H; y++) {
                    blackAndWhiteImage.setRGB(x, y, getPixelBinary(x, y) ? 0 : 0xffffff);
                    grayImage.setRGB(x, y, image.getRGB(x, y));
                }
        } catch (Exception e) {
            UsefulMethods.notifyException(e);
        }
    }
}
