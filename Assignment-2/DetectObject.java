/*
 * Author   : Aniket Ladukar
 * Subject  : CSCI 576 : Multimedia Systems Design
 */



import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    public String toString() {
        return "(" + this.x + "," + this.y + ") : " + this.val;
    }
}

class Cluster {
    int minX;
    int maxX;
    int minY;
    int maxY;
    float scale;
    ArrayList<Node> cNodes;

    public Cluster() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.scale = 0.0f;
        this.cNodes = new ArrayList<>();
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
        this.processData();
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
    HashMap<String, ArrayList<Integer>> coordToRGB;
    HashMap<String, Histogram> hists;
    HashMap<Integer, ArrayList<ArrayList<Node>>> colourSpace; // hsv : {val,x,y}[][3]
    int maxX;
    int minX;
    int maxY;
    int minY;
    float scale;

    final int frameLength = CONSTANTS.HEIGHT.val * CONSTANTS.WIDTH.val * 3;

    public String toString() {
        String string = this.name + " " + this.type + " " + hists.size() + " "
                + colourSpace.get(CONSTANTS.COLOR_HSV.val).size();
        return string;
    }

    public Image(String pathName, int type) {
        File image = new File(pathName);

        this.name = image.getName();
        this.type = type;
        this.coordToRGB = new HashMap<>();
        this.maxX = Integer.MIN_VALUE;
        this.minX = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.scale = 0.0f;

        // Parse RGB file and extra pixels
        // Ignore green background on Object Images
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

                    if (type == CONSTANTS.OBJECT.val) {
                        this.minX = this.minX > x ? x : this.minX;
                        this.minY = this.minY > y ? y : this.minY;
                        this.maxX = this.maxX < x ? x : this.maxX;
                        this.maxY = this.maxY < y ? y : this.maxY;
                    }

                    this.coordToRGB.put("(" + x + "," + y + ")",
                            new ArrayList<Integer>(Arrays.asList((int) r.val, (int) g.val, (int) b.val)));
                    this.red.add(r);
                    this.green.add(g);
                    this.blue.add(b);
                }
            }

            this.scale = (float) (this.maxX - this.minX) / (float) (this.maxY - this.minY);
            System.out.println("calc scale " + this.scale);
            // this.red = redFinal.stream().mapToInt(i -> i).toArray();
            // this.green = greenFinal.stream().mapToInt(i -> i).toArray();
            // this.blue = blueFinal.stream().mapToInt(i -> i).toArray();

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.colourSpace = new HashMap<>();
        this.colourSpace.put(CONSTANTS.COLOR_HSV.val, new ArrayList<>());
        this.colourSpace.put(CONSTANTS.COLOR_YUV.val, new ArrayList<>());
        this.hists = new HashMap<>();

        this.generateColorSpaceData();
        this.generateHist();

    }

    public double[] convRGBtoYUV(double R, double G, double B) {

        int y = (int) Math.round(R * .299000 + G * .587000 + B * .114000);
        int u = (int) Math.round(R * -.168736 + G * -.331264 + B * .500000
                + 128);
        int v = (int) Math.round(R * .500000 + G * -.418688 + B * -.081312
                + 128);

        double yuv[] = new double[3];
        yuv[0] = y;
        yuv[1] = u;
        yuv[2] = v;
        return yuv;
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

        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (int i = 0; i < this.red.size(); i++) {
            int x = this.red.get(i).x;
            int y = this.red.get(i).y;

            double[] hsv = RGB2HSV(this.red.get(i).val, this.green.get(i).val, this.blue.get(i).val);
            double[] yuv = convRGBtoYUV(this.red.get(i).val, this.green.get(i).val, this.blue.get(i).val);

            if (max < yuv[2]) {
                max = yuv[2];
            }

            H.add(new Node(hsv[0], x, y));
            S.add(new Node(hsv[1], x, y));
            V.add(new Node(hsv[2], x, y));
        }

        System.out.println("MAX V in YUV - " + max);

        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(H);
        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(S);
        this.colourSpace.get(CONSTANTS.COLOR_HSV.val).add(V);
    }

    public void generateHist() {
        ArrayList<Node> H = colourSpace.get(CONSTANTS.COLOR_HSV.val).get(0);
        ArrayList<Node> S = colourSpace.get(CONSTANTS.COLOR_HSV.val).get(1);
        ArrayList<Node> V = colourSpace.get(CONSTANTS.COLOR_HSV.val).get(2);

        hists.put(CONSTANTS.COLOR_HSV.toString() + "_H", new Histogram(CONSTANTS.COLOR_HSV.toString() + "_H", 'H', H));
        hists.put(CONSTANTS.COLOR_HSV.toString() + "_S", new Histogram(CONSTANTS.COLOR_HSV.toString() + "_S", 'S', S));
        hists.put(CONSTANTS.COLOR_HSV.toString() + "_V", new Histogram(CONSTANTS.COLOR_HSV.toString() + "_V", 'V', V));

    }
}

public class DetectObject {

    String imgPath;
    String[] objectPath;
    Image img;
    ArrayList<Image> obj_list;

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne = new BufferedImage(CONSTANTS.WIDTH.val, CONSTANTS.HEIGHT.val,
            BufferedImage.TYPE_INT_RGB);

    DetectObject(String[] args) {
        this.imgPath = args[0];
        this.objectPath = Arrays.copyOfRange(args, 1, args.length);
        obj_list = new ArrayList<>();
        this.frame = new JFrame();
        this.frame.setTitle("Processing Image. Please Wait !!!");
    }

    public void renderImage(Image image) {
        try {
            JFrame frame;
            JLabel lbIm1;
            BufferedImage imgOne = new BufferedImage(CONSTANTS.WIDTH.val, CONSTANTS.HEIGHT.val,
                    BufferedImage.TYPE_INT_RGB);

            // Set image value
            for (int i = 0; i < image.red.size(); i++) {

                int x = image.red.get(i).x;
                int y = image.red.get(i).y;

                int pix = 0xff000000 | (((int) image.red.get(i).val & 0xff) << 16)
                        | (((int) image.green.get(i).val & 0xff) << 8) | ((int) image.blue.get(i).val & 0xff);
                imgOne.setRGB(x, y, pix);
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

    public void displayMainImage() {
        try {

            // Set image value
            for (int x = 0; x < CONSTANTS.WIDTH.val; x++) {
                for (int y = 0; y < CONSTANTS.HEIGHT.val; y++) {

                    ArrayList<Integer> colors = this.img.coordToRGB.get("(" + x + "," + y + ")");

                    int red = colors.get(0);
                    int green = colors.get(1);
                    int blue = colors.get(2);

                    int pix = 0xff000000 | ((red & 0xff) << 16)
                            | ((green & 0xff) << 8) | (blue & 0xff);
                    this.imgOne.setRGB(x, y, pix);
                }
            }

            // Use label to display the image
            GridBagLayout gLayout = new GridBagLayout();
            this.frame.getContentPane().setLayout(gLayout);
            this.lbIm1 = new JLabel(new ImageIcon(imgOne));

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.5;
            c.gridx = 0;
            c.gridy = 0;

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 1;
            this.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.frame.getContentPane().add(lbIm1, c);
            this.frame.setResizable(false);
            this.frame.pack();
            this.frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void renderImageFromNodes(Image image, ArrayList<Node> nodes) {
        try {
            JFrame frame;
            JLabel lbIm1;
            BufferedImage imgOne = new BufferedImage(CONSTANTS.WIDTH.val, CONSTANTS.HEIGHT.val,
                    BufferedImage.TYPE_INT_RGB);

            // Set image value
            for (int i = 0; i < nodes.size(); i++) {

                int x = nodes.get(i).x;
                int y = nodes.get(i).y;

                ArrayList<Integer> colors = image.coordToRGB.get("(" + x + "," + y + ")");

                int red = colors.get(0);
                int green = colors.get(1);
                int blue = colors.get(2);

                int pix = 0xff000000 | ((red & 0xff) << 16)
                        | ((green & 0xff) << 8) | (blue & 0xff);
                imgOne.setRGB(x, y, pix);
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

    public void renderBoundingBoxFromCluster(Image image, Cluster cluster, String obj_name) {
        try {
            System.out.println("cluster scale - " + cluster.scale);
            System.out.println("Image scale - " + image.scale);

            int offset = 5;
            cluster.minX = cluster.minX - offset > 0 ? cluster.minX - offset : cluster.minX;
            cluster.minY = cluster.minY - offset > 0 ? cluster.minY - offset : cluster.minY;
            cluster.maxX = cluster.maxX + offset < 640 ? cluster.maxX + offset : cluster.maxX;
            cluster.maxY = cluster.maxY + offset < 480 ? cluster.maxY + offset : cluster.maxY;

            // Render Box
            int boxWidth = 5;
            for (int y = cluster.minY; y < cluster.maxY; y++) {
                for (int w = 0; w < boxWidth; w++) {
                    this.imgOne.setRGB(cluster.minX + w, y, Color.GREEN.getRGB());
                    this.imgOne.setRGB(cluster.maxX - w, y, Color.GREEN.getRGB());
                }
            }

            for (int x = cluster.minX; x < cluster.maxX; x++) {
                for (int w = 0; w < boxWidth; w++) {
                    this.imgOne.setRGB(x, cluster.minY + w, Color.GREEN.getRGB());
                }
                for (int w = 0; w < boxWidth + 7; w++) {
                    this.imgOne.setRGB(x, cluster.maxY - w, Color.GREEN.getRGB());

                }
            }

            Font font = new Font("Arial", Font.BOLD, 12);
            Graphics g = this.imgOne.getGraphics();
            g.setFont(font);
            g.setColor(Color.BLACK);
            g.drawString(obj_name, cluster.minX, cluster.maxY);

            // Use label to display the image
            this.frame.repaint();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Integer> findTopFreqBins(Image image, int k, char colorDim) {

        // Temp Logging dims
        System.out.println(image.name);

        Histogram obj = image.hists.get(CONSTANTS.COLOR_HSV.toString() + "_" + colorDim);

        HashMap<Integer, Integer> freqToBin = new HashMap<>();

        for (Entry<Integer, ArrayList<Node>> e : obj.map.entrySet()) {
            if (freqToBin.size() < k) {
                freqToBin.put(e.getValue().size(), e.getKey());
            } else {
                int min = Collections.min(freqToBin.entrySet(), Map.Entry.comparingByKey()).getKey();

                if (e.getValue().size() > min) {
                    freqToBin.remove(min);
                    freqToBin.put(e.getValue().size(), e.getKey());
                }

            }
        }

        return new ArrayList<>(freqToBin.values());
    }

    public void detectAndDisplay() {
        long start = System.currentTimeMillis();

        // Generate Images
        this.frame.setTitle("Creating Histograms. Please Wait !!!");
        System.out.println("Creating Histograms. Please Wait !!!");
        this.img = new Image(this.imgPath, CONSTANTS.IMAGE.val);

        for (String image : this.objectPath)
            this.obj_list.add(new Image(image, CONSTANTS.OBJECT.val));

        int topBinCount = 4;
        // 41015
        // Display Image
        displayMainImage();

        for (Image obj_image : this.obj_list) {
            // renderImage(obj_image);
            ArrayList<Node> img_pix = new ArrayList<>();
            ArrayList<Integer> topSBins = findTopFreqBins(obj_image, topBinCount + 20, 'S');
            ArrayList<Integer> topVBins = findTopFreqBins(obj_image, topBinCount + 9, 'V');

            // Filter image pixel having hues present in top k bins
            for (Integer bin : findTopFreqBins(obj_image, topBinCount, 'H')) {
                this.frame.setTitle("Filtering Pixels. Please Wait !!!");
                System.out.println("Filtering Pixels. Please Wait !!!");

                ArrayList<Node> matchingNodes = this.img.hists.get(CONSTANTS.COLOR_HSV.toString() + "_H").map.get(bin);

                if (matchingNodes != null) {

                    for (Node node : matchingNodes) {
                        ArrayList<Integer> rgb = this.img.coordToRGB.get("(" + node.x + "," + node.y + ")");
                        double[] hsv = Image.RGB2HSV(rgb.get(0), rgb.get(1), rgb.get(2));

                        int vBin = (int) Math
                                .floor(hsv[2] / this.img.hists.get(CONSTANTS.COLOR_HSV.toString() + "_V").binCap);

                        int sBin = (int) Math
                                .floor(hsv[1] / this.img.hists.get(CONSTANTS.COLOR_HSV.toString() + "_S").binCap);

                        // if (!(topVBins.contains(vBin) ^ topSBins.contains(sBin)) ) {
                        if ((topVBins.contains(vBin) && topSBins.contains(sBin))) {

                            img_pix.add(node);
                        }
                    }

                }

            }
            this.frame.setTitle("Creating Clusters. Please Wait !!!");
            System.out.println("Creating Clusters. Please Wait !!!");
            System.out.println(img_pix.size());

            for (Cluster c : DBScan(img, obj_image, img_pix)) {

                // renderImageFromNodes(img, c.cNodes);
                renderBoundingBoxFromCluster(img, c, obj_image.name);
            }

        }

        long end = System.currentTimeMillis();
        long elapsedTime = end - start;
        this.frame.setTitle("Detection Complete. Time taken to detect object(s) - " + elapsedTime / 1000.0 + " s");
        System.out.println("Detection Complete. Time taken to detect object(s) - " + elapsedTime / 1000.0 + " s");
    }

    public ArrayList<Cluster> DBScan(Image main_image, Image obj_image, ArrayList<Node> nodes) {

        this.frame.setTitle("Searching Object: " + obj_image.name + ". Please Wait !!!");
        System.out.println("Searching Object: " + obj_image.name + ". Please Wait !!!");

        int eps = 10;
        int minNodes = 15;

        ArrayList<Node> centreNodes = new ArrayList<>();

        // Identify Centre Nodes
        for (Node currNode : nodes) {
            int count = 0;

            for (Node node : nodes) {
                int distance = (int) Math.sqrt(Math.pow(currNode.x - node.x, 2) + Math.pow(currNode.y - node.y, 2));

                if (distance < eps) {
                    count++;
                }
            }

            if (count > minNodes) {
                centreNodes.add(currNode);
            }

            if (count % 300 == 0) {
                this.frame.setTitle("Searching Object: " + obj_image.name + ". Please Wait !");
                System.out.println("Searching Object: " + obj_image.name + ". Please Wait !");
            } else if (count % 200 == 0) {
                this.frame.setTitle("Searching Object: " + obj_image.name + ". Please Wait !!!");
                System.out.println("Searching Object: " + obj_image.name + ". Please Wait !!!");
            } else if (count % 100 == 0) {
                this.frame.setTitle("Searching Object: " + obj_image.name + ". Please Wait !!!!!!");
                System.out.println("Searching Object: " + obj_image.name + ". Please Wait !!!!!!");
            }
        }

        this.frame.setTitle("Creating clusters. This could take upto a minute");
        System.out.println("Creating clusters. This could take upto a minute");

        // Create Cluster
        ArrayList<Cluster> clusterList = new ArrayList<>();
        while (!centreNodes.isEmpty()) {
            Node currNode = centreNodes.remove(0);

            Cluster cluster = new Cluster();
            ArrayList<Node> neighbor = new ArrayList<>();

            int xmin = Integer.MAX_VALUE;
            int xmax = Integer.MIN_VALUE;
            int ymin = Integer.MAX_VALUE;
            int ymax = Integer.MIN_VALUE;

            cluster.cNodes.add(currNode);
            neighbor.add(currNode);

            while (!neighbor.isEmpty()) {
                Node currNeighbor = neighbor.remove(0);
                ArrayList<Node> toRemove = new ArrayList<>();

                for (Node node : centreNodes) {
                    int distance = (int) Math.sqrt(Math.pow(currNeighbor.x - node.x, 2) +
                            Math.pow(currNeighbor.y - node.y, 2));

                    if (distance < eps) {
                        cluster.cNodes.add(node);
                        neighbor.add(node);
                        toRemove.add(node);

                        xmin = xmin > node.x ? node.x : xmin;
                        ymin = ymin > node.y ? node.y : ymin;
                        xmax = xmax < node.x ? node.x : xmax;
                        ymax = ymax < node.y ? node.y : ymax;
                    }
                }

                centreNodes.removeAll(toRemove);

            }

            System.out.println("Scale " + clusterList.size() + " : " + (float) (xmax - xmin) / (float) (ymax - ymin));
            cluster.minX = xmin;
            cluster.minY = ymin;
            cluster.maxX = xmax;
            cluster.maxY = ymax;
            cluster.scale = (float) (xmax - xmin) / (float) (ymax - ymin);

            clusterList.add(cluster);
        }

        ArrayList<Integer> topBin = findTopFreqBins(obj_image, 1, 'H');
        ArrayList<Integer> topBinS = findTopFreqBins(obj_image, 12, 'S');
        ArrayList<Integer> topBinV = findTopFreqBins(obj_image, 12, 'V');

        int count = 0;
        int ind = 0;
        System.out.println(obj_image.scale);
        for (int i = 0; i < clusterList.size(); i++) {
            ArrayList<Node> curr = clusterList.get(i).cNodes;
            int sum = 0;
            for (int j = 0; j < curr.size(); j++) {
                ArrayList<Integer> rgb = main_image.coordToRGB.get("(" + curr.get(j).x + "," + curr.get(j).y + ")");
                double[] hsv = Image.RGB2HSV(rgb.get(0), rgb.get(1), rgb.get(2));

                int currBin = (int) hsv[0] / obj_image.hists.get(CONSTANTS.COLOR_HSV.toString() + "_H").binCap;
                int currBinS = (int) hsv[1] / obj_image.hists.get(CONSTANTS.COLOR_HSV.toString() + "_S").binCap;
                int currBinV = (int) hsv[2] / obj_image.hists.get(CONSTANTS.COLOR_HSV.toString() + "_V").binCap;

                if (topBin.contains(currBin)) {
                    sum++;
                }
            }

            if (count < sum) {
                count = sum;
                ind = i;
            }
        }

        System.out.println("MAX " + count);
        System.out.println("num clusters - " + clusterList.size());

        System.out.println(ind);
        ArrayList<Cluster> c = new ArrayList<>();

        System.out.println("count " + count);
        for (Cluster clust : clusterList) {

            System.out.println("clust.cNodes.size() " + clust.cNodes.size());
            if (clust.cNodes.size() > 0.9 * count && clust.cNodes.size() > 1000) {

                System.out.println("added");
                c.add(clust);

            }
        }

        if (c.size() > 1) {
            c.removeIf((clust) -> (Math.abs(clust.scale - obj_image.scale) > 0.45));
        }

        System.out.println("total c size " + c.size());
        return c;

    }

    public static void main(String[] args) {
        DetectObject obj = new DetectObject(args);
        obj.detectAndDisplay();
    }
}
