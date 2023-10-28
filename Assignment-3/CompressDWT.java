import java.awt.image.BufferedImage;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

class ImageChannels {

    double[] red;
    double[] green;
    double[] blue;

    public ImageChannels(int width, int height) {

        this.red = new double[width * height];
        this.green = new double[width * height];
        this.blue = new double[width * height];
    }

}

class Image {
    File imageFile;
    ImageChannels imageChannels;
    ImageChannels channelsDWT;
    ImageChannels channelsIDWT;

    int height = 512;
    int width = 512;
    int frameLength = this.height * this.width * 3;

    public Image(String imagePath) {
        this.imageChannels = new ImageChannels(this.width, this.height);
        this.channelsDWT = new ImageChannels(this.width, this.height);
        this.channelsIDWT = new ImageChannels(this.width, this.height);
        this.imageFile = new File(imagePath);

        try {
            RandomAccessFile raf = new RandomAccessFile(this.imageFile, "r");
            raf.seek(0);

            byte[] bytes = new byte[this.frameLength];
            raf.read(bytes);
            raf.close();

            ByteBuffer redBuff = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, frameLength / 3));
            ByteBuffer greenBuff = ByteBuffer.wrap(Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2));
            ByteBuffer blueBuff = ByteBuffer.wrap(Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength));

            this.imageChannels.red = Stream.generate(() -> redBuff.get()).limit(redBuff.capacity())
                    .map(b -> b & 0xff)
                    .mapToDouble(i -> i)
                    .toArray();

            this.imageChannels.green = Stream.generate(() -> blueBuff.get()).limit(redBuff.capacity())
                    .map(b -> b & 0xff)
                    .mapToDouble(i -> i)
                    .toArray();

            this.imageChannels.green = Stream.generate(() -> greenBuff.get()).limit(redBuff.capacity())
                    .map(b -> b & 0xff)
                    .mapToDouble(i -> i)
                    .toArray();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class CompressDWT {
    JFrame frame;
    JLabel lbIm1;
    BufferedImage frameImage;
    Image img;
    int lowpassLevel;
    int SLEEP_TIME = 300;

    public CompressDWT(String imagePath, int lowpassLevel) {
        this.img = new Image(imagePath);
        this.lowpassLevel = lowpassLevel;
        this.frameImage = new BufferedImage(this.img.width, this.img.height, BufferedImage.TYPE_INT_RGB);
    }

    public void renderImage(ImageChannels channels, boolean repaint) {
        if (!repaint) {
            this.frameImage = new BufferedImage(this.img.width, this.img.height, BufferedImage.TYPE_INT_RGB);
        }
        int ind;
        for (int x = 0; x < this.img.width; x++) {
            for (int y = 0; y < this.img.height; y++) {
                ind = (int) x + (int) y * this.img.width;

                int r = (int) channels.red[ind];
                int g = (int) channels.green[ind];
                int b = (int) channels.blue[ind];

                int pix = 0xff000000 | ((r  << 16) | (g << 8) | (b));
                
                // TODO remove try catch
                try {
                    this.frameImage.setRGB(x, y, pix);

                } catch (Exception e) {
                    break;
                }
            }
        }

        StringBuilder title = new StringBuilder();
        String spacing = "      ";

        title.append(this.img.imageFile.getName());
        title.append(spacing);
        title.append(this.lowpassLevel);

        if (!repaint) {
            // Use label to display the image
            this.frame = new JFrame();
            GridBagLayout gLayout = new GridBagLayout();
            this.frame.getContentPane().setLayout(gLayout);
            lbIm1 = new JLabel(new ImageIcon(this.frameImage));

            this.frame.setResizable(false);

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 0;

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            frame.getContentPane().add(lbIm1, c);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);

        } else {
            title.append(spacing);
            title.append("Progressive | Delay: " + SLEEP_TIME + "ms");
            this.frame.repaint();
        }

        this.frame.setTitle(title.toString());
    }

    public void performDWT(int dwtLevel) {

        int ind1, ind2, indK;

        double lpRed, lpGreen, lpBlue;
        double hpRed, hpGreen, hpBlue;

        for (int lv = 1; lv <= dwtLevel; lv++) {

            int lp = 0;
            int hp = this.img.width / (int) Math.pow(2, lv);

            // ROW wise DWT
            for (int y = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y++) {
                for (int x = 0, k = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x = x + 2, k++) {

                    ind1 = (x) + y * this.img.width;
                    ind2 = (x + 1) + y * this.img.width;

                    // Low Pass
                    indK = (lp + k) + y * this.img.width;
                    lpRed = (this.img.imageChannels.red[ind1] + this.img.imageChannels.red[ind2]) / 2.0;
                    lpGreen = (this.img.imageChannels.green[ind1] + this.img.imageChannels.green[ind2]) / 2.0;
                    lpBlue = (this.img.imageChannels.blue[ind1] + this.img.imageChannels.blue[ind2]) / 2.0;
                    this.img.channelsDWT.red[indK] = lpRed;
                    this.img.channelsDWT.green[indK] = lpGreen;
                    this.img.channelsDWT.blue[indK] = lpBlue;

                    // High Pass
                    indK = (hp + k) + y * this.img.width;
                    hpRed = (this.img.imageChannels.red[ind1] - this.img.imageChannels.red[ind2]) / 2.0;
                    hpGreen = (this.img.imageChannels.green[ind1] - this.img.imageChannels.green[ind2]) / 2.0;
                    hpBlue = (this.img.imageChannels.blue[ind1] - this.img.imageChannels.blue[ind2]) / 2.0;
                    this.img.channelsDWT.red[indK] = hpRed;
                    this.img.channelsDWT.green[indK] = hpGreen;
                    this.img.channelsDWT.blue[indK] = hpBlue;
                }
            }
            this.img.imageChannels.red = Arrays.copyOf(this.img.channelsDWT.red, this.img.imageChannels.red.length);
            this.img.imageChannels.green = Arrays.copyOf(this.img.channelsDWT.green,
                    this.img.imageChannels.green.length);
            this.img.imageChannels.blue = Arrays.copyOf(this.img.channelsDWT.blue, this.img.imageChannels.blue.length);

            // COLUMN wise DWT
            for (int x = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x++) {
                for (int y = 0, k = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y = y + 2, k++) {

                    ind1 = x + (y) * this.img.width;
                    ind2 = x + (y + 1) * this.img.width;

                    // Low Pass
                    indK = x + (lp + k) * this.img.width;
                    lpRed = (this.img.imageChannels.red[ind1] + this.img.imageChannels.red[ind2]) / 2.0;
                    lpGreen = (this.img.imageChannels.green[ind1] + this.img.imageChannels.green[ind2]) / 2.0;
                    lpBlue = (this.img.imageChannels.blue[ind1] + this.img.imageChannels.blue[ind2]) / 2.0;
                    this.img.channelsDWT.red[indK] = lpRed;
                    this.img.channelsDWT.green[indK] = lpGreen;
                    this.img.channelsDWT.blue[indK] = lpBlue;

                    // High Pass
                    indK = x + (hp + k) * this.img.width;
                    hpRed = (this.img.imageChannels.red[ind1] - this.img.imageChannels.red[ind2]) / 2.0;
                    hpGreen = (this.img.imageChannels.green[ind1] - this.img.imageChannels.green[ind2]) / 2.0;
                    hpBlue = (this.img.imageChannels.blue[ind1] - this.img.imageChannels.blue[ind2]) / 2.0;
                    this.img.channelsDWT.red[indK] = hpRed;
                    this.img.channelsDWT.green[indK] = hpGreen;
                    this.img.channelsDWT.blue[indK] = hpBlue;
                }
            }
            this.img.imageChannels.red = Arrays.copyOf(this.img.channelsDWT.red, this.img.imageChannels.red.length);
            this.img.imageChannels.green = Arrays.copyOf(this.img.channelsDWT.green,
                    this.img.imageChannels.green.length);
            this.img.imageChannels.blue = Arrays.copyOf(this.img.channelsDWT.blue, this.img.imageChannels.blue.length);

        }
    }

    public void inverseDWT(int dwtLevel) {
        int ind1, ind2, indLP, indHP;

        int lpRed, lpGreen, lpBlue;
        int hpRed, hpGreen, hpBlue;

        for (int lv = dwtLevel; lv >= 1; lv--) {

            int lp = 0;
            int hp = this.img.width / (int) Math.pow(2, lv);

            // COLUMN wise IDWT
            for (int x = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x++) {
                for (int y = 0, k = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y = y + 2, k++) {

                    ind1 = x + (y) * this.img.width;
                    ind2 = x + (y + 1) * this.img.width;
                    indLP = x + (lp + k) * this.img.width;
                    indHP = x + (hp + k) * this.img.width;

                    // Red
                    this.img.channelsIDWT.red[ind1] = this.img.imageChannels.red[indLP]
                            + this.img.imageChannels.red[indHP];
                    this.img.channelsIDWT.red[ind2] = this.img.imageChannels.red[indLP]
                            - this.img.imageChannels.red[indHP];

                    // Green
                    this.img.channelsIDWT.green[ind1] = this.img.imageChannels.green[indLP]
                            + this.img.imageChannels.green[indHP];
                    this.img.channelsIDWT.green[ind2] = this.img.imageChannels.green[indLP]
                            - this.img.imageChannels.green[indHP];

                    // Blue
                    this.img.channelsIDWT.blue[ind1] = this.img.imageChannels.blue[indLP]
                            + this.img.imageChannels.blue[indHP];
                    this.img.channelsIDWT.blue[ind2] = this.img.imageChannels.blue[indLP]
                            - this.img.imageChannels.blue[indHP];
                }
            }
            for (int x = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x++) {
                for (int y = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y++) {

                    ind1 = x + (y) * this.img.width;

                    this.img.imageChannels.red[ind1] = this.img.channelsIDWT.red[ind1];
                    this.img.imageChannels.green[ind1] = this.img.channelsIDWT.green[ind1];
                    this.img.imageChannels.blue[ind1] = this.img.channelsIDWT.blue[ind1];
                }
            }

            // ROW wise IDWT
            for (int y = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y++) {
                for (int x = 0, k = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x = x + 2, k++) {

                    ind1 = (x) + y * this.img.width;
                    ind2 = (x + 1) + y * this.img.width;
                    indLP = (lp + k) + y * this.img.width;
                    indHP = (hp + k) + y * this.img.width;

                    // Red
                    this.img.channelsIDWT.red[ind1] = this.img.imageChannels.red[indLP] +
                            this.img.imageChannels.red[indHP];
                    this.img.channelsIDWT.red[ind2] = this.img.imageChannels.red[indLP] -
                            this.img.imageChannels.red[indHP];

                    // Green
                    this.img.channelsIDWT.green[ind1] = this.img.imageChannels.green[indLP]
                            + this.img.imageChannels.green[indHP];
                    this.img.channelsIDWT.green[ind2] = this.img.imageChannels.green[indLP]
                            - this.img.imageChannels.green[indHP];

                    // Blue
                    this.img.channelsIDWT.blue[ind1] = this.img.imageChannels.blue[indLP]
                            + this.img.imageChannels.blue[indHP];
                    this.img.channelsIDWT.blue[ind2] = this.img.imageChannels.blue[indLP]
                            - this.img.imageChannels.blue[indHP];
                }
            }
            for (int x = 0; x < this.img.width / (int) Math.pow(2, lv - 1); x++) {
                for (int y = 0; y < this.img.width / (int) Math.pow(2, lv - 1); y++) {

                    ind1 = x + (y) * this.img.width;

                    this.img.imageChannels.red[ind1] = this.img.channelsIDWT.red[ind1];
                    this.img.imageChannels.green[ind1] = this.img.channelsIDWT.green[ind1];
                    this.img.imageChannels.blue[ind1] = this.img.channelsIDWT.blue[ind1];
                }
            }

        }

    }

    public void extractCoefficients(int lowpassLevel) {

        int coeffDim = (int) Math.pow(2, lowpassLevel);
        int ind;

        for (int x = 0; x < this.img.width; x++) {
            for (int y = 0; y < this.img.height; y++) {

                ind = x + y * this.img.width;

                if (x >= coeffDim || y >= coeffDim) {
                    this.img.imageChannels.red[ind] = 0;
                    this.img.imageChannels.green[ind] = 0;
                    this.img.imageChannels.blue[ind] = 0;
                } else {
                    this.img.imageChannels.red[ind] = this.img.channelsDWT.red[ind];
                    this.img.imageChannels.green[ind] = this.img.channelsDWT.green[ind];
                    this.img.imageChannels.blue[ind] = this.img.channelsDWT.blue[ind];
                }
            }
        }
    }

    public void compress() {

        if (this.lowpassLevel == -1) {
            // Progressive
            this.performDWT(9);
            for (int x = 0; x <= 9; x++) {

                this.extractCoefficients(x);
                this.inverseDWT(9);
                this.renderImage(this.img.channelsIDWT, x > 0);

                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    System.err.println("Some error occurred - Cannot put thread to Sleep");
                    System.exit(0);
                }
            }
        } else {
            // this.renderImage(this.img.imageChannels, false);
            this.performDWT(9);
            this.extractCoefficients(lowpassLevel);
            this.inverseDWT(9);
            this.renderImage(this.img.channelsIDWT, false);
        }
    }

    public static void main(String[] args) {
        String imagePath = args[0];
        int lowpassLevel = Integer.parseInt(args[1]);
        System.out.println("imagePath " + imagePath);
        System.out.println("lowpassLevel " + lowpassLevel);

        CompressDWT obj = new CompressDWT(imagePath, lowpassLevel);
        obj.compress();
    }
}
