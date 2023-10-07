import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

enum CONSTANTS {
    WIDTH(640),
    HEIGHT(480),
    IMAGE(1),
    OBJECT(2),
    TOTAL_BINS(36),
    COLOR_HSV(21),
    COLOR_YUV(22);

    public final int val;

    CONSTANTS(int val) {
        this.val = val;
    }
}

class Node {
    double val;
    int x;
    int y;

    public Node(double val, int x, int y) {
        this.val = val;
        this.x = x;
        this.y = y;
    }
}

class Histogram {
    String name;
    char colourDim;
    int count;
    int binCap;
    int min;
    int max;
    HashMap<Integer, ArrayList<Node>> map;
    ArrayList<Node> rawData;

    public Histogram(String name, char colorDim, ArrayList<Node> data) {
        this.name = name;
        this.colourDim = colorDim;
        this.count = 0;
        this.map = new HashMap<>();

        this.rawData = new ArrayList<Node>(data);

        switch (colorDim) {
            case 'H':
                this.min = 0;
                this.max = 360;
                break;
            case 'S':
            case 'V':
                this.min = 0;
                this.max = 100;
                break;
            default:
        }
        this.binCap = (this.max - this.min) / CONSTANTS.TOTAL_BINS.val;
    }

    public void processData() {

        for (int i = 0; i < this.rawData.size(); i++) {
            Node currNode = this.rawData.get(i);
            int binNumber = (int) Math.floor(currNode.val / binCap);
            if (this.map.containsKey(binNumber)) {
                this.map.get(binNumber).add(currNode);
            } else {
                ArrayList<Node> list = new ArrayList<>();
                list.add(currNode);
                this.map.put(binNumber, list);
            }

            this.count++;
        }
    }

}

class Image {
    String name;
    int type;
    ArrayList<Node> red;
    ArrayList<Node> green;
    ArrayList<Node> blue;
    ArrayList<Histogram> hists;
    HashMap<Integer, ArrayList<ArrayList<Node>>> colourSpace; // hsv : {val,x,y}[][3]

    final int frameLength = CONSTANTS.HEIGHT.val * CONSTANTS.WIDTH.val * 3;

    public Image(String pathName, int type) {
        File image = new File(pathName);

        this.name = image.getName();
        this.type = type;

        try {
            RandomAccessFile raf = new RandomAccessFile(pathName, "r");
            raf.seek(0);

            byte[] bytes = new byte[frameLength];
            raf.read(bytes);
            raf.close();

            byte[] redStream = Arrays.copyOfRange(bytes, 0, frameLength / 3);
            byte[] greenStream = Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2);
            byte[] blueStream = Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength);

            int ind;

            this.red = new ArrayList<>();
            this.green = new ArrayList<>();
            this.blue = new ArrayList<>();

            for (int x = 0; x < CONSTANTS.WIDTH.val; x++) {
                for (int y = 0; y < CONSTANTS.HEIGHT.val; y++) {

                    ind = x + y * CONSTANTS.WIDTH.val;

                    Node r = new Node(redStream[ind] & 0xff, x, y);
                    Node g = new Node(greenStream[ind] & 0xff, x, y);
                    Node b = new Node(blueStream[ind] & 0xff, x, y);

                    if (g.val == 255.0 && b.val == 0.0 && r.val == 0.0 && type == CONSTANTS.OBJECT.val) {
                        continue;
                    }

                    this.red.add(r);
                    this.green.add(g);
                    this.blue.add(b);
                }
            }

            // this.red = redFinal.stream().mapToInt(i -> i).toArray();
            // this.green = greenFinal.stream().mapToInt(i -> i).toArray();
            // this.blue = blueFinal.stream().mapToInt(i -> i).toArray();

            System.out.println("-------------------------");
            System.out.println(this.name);
            System.out.println(this.red.size());
            System.out.println("-------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.colourSpace = new HashMap<>();
        this.colourSpace.put(CONSTANTS.COLOR_HSV.val, new ArrayList<>());
        this.colourSpace.put(CONSTANTS.COLOR_YUV.val, new ArrayList<>());

        generateColorSpaceData();

        generateHist(CONSTANTS.COLOR_HSV.toString(), 'H', colourSpace.get(CONSTANTS.COLOR_HSV.val).get(0));

    }

    public static double[] RGB2HSV(double R, double G, double B) {

        R /= 255.0;
        G /= 255.0;
        B /= 255.0;

        double min = Math.min(Math.min(R, G), B);
        double max = Math.max(Math.max(R, G), B);
        double delta = max - min;

        double H = max;
        double S = max;
        double V = max;

        if (delta == 0) {
            H = 0;
            S = 0;
        } else {

            S = delta / max;

            double delR = (((max - R) / 6) + (delta / 2)) / delta;
            double delG = (((max - G) / 6) + (delta / 2)) / delta;
            double delB = (((max - B) / 6) + (delta / 2)) / delta;

            if (R == max) {
                H = delB - delG;
            } else if (G == max) {
                H = (1.0 / 3.0) + delR - delB;
            } else if (B == max) {
                H = (2.0 / 3.0) + delG - delR;
            }

            if (H < 0)
                H += 1;
            if (H > 1)
                H -= 1;
        }

        double[] hsv = new double[3];
        hsv[0] = H * 360;
        hsv[1] = S * 100;
        hsv[2] = V * 100;
        return hsv;
    }

    public void generateColorSpaceData() {

        ArrayList<Node> H = new ArrayList<>();
        ArrayList<Node> S = new ArrayList<>();
        ArrayList<Node> V = new ArrayList<>();

        for (int i = 0; i < this.red.size(); i++) {
            int x = this.red.get(i).x;
            int y = this.red.get(i).y;

            double[] hsv = RGB2HSV(this.red.get(i).val, this.green.get(i).val, this.blue.get(i).val);

            H.add(new Node(hsv[0], x, y));
            S.add(new Node(hsv[1], x, y));
            V.add(new Node(hsv[2], x, y));
        }

        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(H);
        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(S);
        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(V);
    }

    public void generateHist(String colorSpace, char colorDim, ArrayList<Node> vals) {

        Histogram hist = new Histogram(colorSpace + colorDim, colorDim, vals);
        hist.processData();

        System.out.println("SIZE - "+hist.map.entrySet().size());
        for (Entry<Integer, ArrayList<Node>> entry : hist.map.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue().size());
        }
    }
}

public class DetectObject {

    String imgPath;
    String[] objectPath;

    DetectObject(String[] args) {
        this.imgPath = args[0];
        this.objectPath = Arrays.copyOfRange(args, 1, args.length);
    }

    public void renderImage(String renderImagePath) {
        int frameLength = CONSTANTS.HEIGHT.val * CONSTANTS.WIDTH.val * 3;
        byte[] red;
        byte[] green;
        byte[] blue;

        File file = new File(renderImagePath);

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            byte[] bytes = new byte[frameLength];
            raf.read(bytes);
            raf.close();

            red = Arrays.copyOfRange(bytes, 0, frameLength / 3);
            green = Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2);
            blue = Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength);

            JFrame frame;
            JLabel lbIm1;
            BufferedImage imgOne = new BufferedImage(CONSTANTS.WIDTH.val, CONSTANTS.HEIGHT.val,
                    BufferedImage.TYPE_INT_RGB);

            // Set image value
            int ind;
            for (int x = 0; x < CONSTANTS.WIDTH.val; x++) {
                for (int y = 0; y < CONSTANTS.HEIGHT.val; y++) {
                    ind = x + y * CONSTANTS.WIDTH.val;

                    int r = red[ind] & 0xff;
                    int g = green[ind] & 0xff;
                    int b = blue[ind] & 0xff;

                    if (g == 255 && b == 0 && r == 0) {
                        continue;
                    }

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

                    imgOne.setRGB(x, y, pix);
                }
            }

            // Use label to display the image
            frame = new JFrame();
            GridBagLayout gLayout = new GridBagLayout();
            frame.getContentPane().setLayout(gLayout);
            lbIm1 = new JLabel(new ImageIcon(imgOne));

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 0;

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.getContentPane().add(lbIm1, c);
            frame.setResizable(false);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void detectAndDisplay() {

        new Image(this.imgPath, CONSTANTS.IMAGE.val);

        for (String image : this.objectPath)
            new Image(image, CONSTANTS.OBJECT.val);
        // renderImage(image);

    }

    public static void main(String[] args) {
        DetectObject obj = new DetectObject(args);
        obj.detectAndDisplay();
    }
}
