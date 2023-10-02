import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

class AllError {
    Channel channelError;
    double combinedError;
}

class Channel {
    double red;
    double green;
    double blue;
}

class ErrorData {
    int dropPercent;
    double errorRate;
    Channel channelError;

    public String toString() {
        return "dropPercent: " + dropPercent + "  errorRate: " + errorRate + "  RED: " + channelError.red + "  GREEN: "
                + channelError.green + "  BLUE: " + channelError.blue;
    }
}

public class ImageAnalysis {

    int width = 1920;
    int height = 1080;
    int singleChannel = width * height;
    int frameLength = singleChannel * 3;

    int gStart = singleChannel;
    int bStart = 2 * singleChannel;

    String destinationRoot = "./Interpolated";

    HashMap<String, ArrayList<ErrorData>> errorMap = new HashMap();

    public void analyzeFile(File file, int dropPercent) {

        try {
            RandomAccessFile readFile = new RandomAccessFile(file, "r");
            readFile.seek(0);

            byte[] bytes = new byte[this.frameLength];
            readFile.read(bytes);
            readFile.close();

            byte[] newBytes = new byte[this.frameLength];
            int ind;

            newBytes = Arrays.copyOf(bytes, bytes.length);

            ArrayList<Integer[]> list = new ArrayList<>();

            for (int i = 0; i < singleChannel * dropPercent / 100; i++) {

                int x = (int) (Math.random() * width);
                int y = (int) (Math.random() * height);

                ind = y * this.width + x;

                list.add(new Integer[] { x, y });
                newBytes[ind] = 0;
                newBytes[gStart + ind] = 0;
                newBytes[bStart + ind] = 0;

            }

            interpolate(newBytes, list);

            if (dropPercent % 15 == 0) {
                RandomAccessFile newFile = new RandomAccessFile(
                        destinationRoot + "/" +
                                file.getName() + "/" +
                                dropPercent + file.getName(),
                        "rw");
                newFile.write(newBytes);
                newFile.close();
            }

            // Calculate error Rate

            ErrorData error = new ErrorData();

            error.dropPercent = dropPercent;
            AllError allError = calculateError(bytes, newBytes);
            // error.errorRate = calculateError(bytes, newBytes);

            error.errorRate = allError.combinedError;
            error.channelError = allError.channelError;

            if (errorMap.containsKey(file.getName())) {
                errorMap.get(file.getName()).add(error);
            } else {
                ArrayList<ErrorData> errorlist = new ArrayList<>();
                errorlist.add(error);
                errorMap.put(file.getName(), errorlist);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AllError calculateError(byte[] bytes, byte[] newBytes) {
        int ind;
        double errorRed = 0;
        double errorGreen = 0;
        double errorBlue = 0;
        double combinedError = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ind = y * this.width + x;

                errorRed += Math.pow((newBytes[ind] & 0xff) - (bytes[ind] & 0xff), 2);
                errorGreen += Math.pow((newBytes[gStart + ind] & 0xff) - (bytes[gStart + ind] & 0xff), 2);
                errorBlue += Math.pow((newBytes[bStart + ind] & 0xff) - (bytes[bStart + ind] & 0xff), 2);
            }
        }

        errorRed /= singleChannel;
        errorGreen /= singleChannel;
        errorBlue /= singleChannel;

        // RMSE
        // combinedError = Math.sqrt((errorRed + errorGreen + errorBlue) / 3);
        // double redChannelError = Math.sqrt(errorRed);
        // double greenChannelError = Math.sqrt(errorGreen);
        // double blueChannelError = Math.sqrt(errorBlue);

        // MSE
        combinedError = (errorRed + errorGreen + errorBlue) / 3;
        double redChannelError = errorRed;
        double greenChannelError = errorGreen;
        double blueChannelError = errorBlue;

        Channel ch = new Channel();
        ch.red = redChannelError;
        ch.green = greenChannelError;
        ch.blue = blueChannelError;

        AllError allError = new AllError();
        allError.channelError = ch;
        allError.combinedError = combinedError;

        return allError;
    }

    public void interpolate(byte[] bytes, ArrayList<Integer[]> list) {

        int gridOffset = 5;

        list.forEach((point) -> {

            int x = point[0];
            int y = point[1];

            long averageRed = 0;
            long averageGreen = 0;
            long averageBlue = 0;
            int count = 0;
            int ind;

            for (int i = x - gridOffset; i <= x + gridOffset; i++) {
                for (int j = y - gridOffset; j <= y + gridOffset; j++) {

                    if (i >= 0 && i < this.width && j >= 0 && j < this.height && x != i && y != j) {
                        ind = j * this.width + i;

                        averageRed += bytes[ind] & 0xff;
                        averageGreen += bytes[gStart + ind] & 0xff;
                        averageBlue += bytes[bStart + ind] & 0xff;
                        count++;
                    }

                }
            }

            averageRed /= count;
            averageGreen /= count;
            averageBlue /= count;

            ind = y * this.width + x;
            bytes[ind] = (byte) (averageRed - 0xff - 1);
            bytes[gStart + ind] = (byte) (averageGreen - 0xff - 1);
            bytes[bStart + ind] = (byte) (averageBlue - 0xff - 1);
        });

    }

    public void processImages(ArrayList<File> analysisFiles) {
        File destination;
        destination = new File(destinationRoot);
        destination.mkdir();

        int count = 0;
        for (File f : analysisFiles) {
            destination = new File(destinationRoot + "/" + f.getName() + "/");
            destination.mkdir();

            for (int i = 0; i <= 50; i++) {
                analyzeFile(f, i);
                System.out.println(f.getName() + " " + i + "% Completed");
            }

            // if (++count == 3) {
            // break;
            // }
        }

        StringBuilder s = new StringBuilder();
        for (String key : errorMap.keySet()) {

            s.append(key + "~~~");
            errorMap.get(key).forEach(datum -> {
                s.append(datum.toString() + ":::");
            });
            s.append("\n");
            s.append("FILE END");

        }

        // Print Log file
        try {
            File log = new File("./log.txt");
            FileWriter fileWriter = new FileWriter(log);

            // Write the content of the StringBuffer to the file
            fileWriter.write(s.toString());

            // Close the FileWriter to flush and release resources
            fileWriter.close();
        } catch (Exception e) {

        }

        System.out.println(s);
    }

    public void stats(File file) {
        try {
            RandomAccessFile readFile = new RandomAccessFile(file, "r");
            readFile.seek(0);

            byte[] bytes = new byte[this.frameLength];
            readFile.read(bytes);
            readFile.close();

            int ind;
            float red = 0;
            int minRed = bytes[0];
            int maxRed = bytes[0];
            float green = 0;
            int minGreen = bytes[gStart];
            int maxGreen = bytes[gStart];
            float blue = 0;
            int minBlue = bytes[bStart];
            int maxBlue = bytes[bStart];

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    ind = j * this.width + i;

                    red += bytes[ind] & 0xff;
                    green += bytes[gStart + ind] & 0xff;
                    blue += bytes[bStart + ind] & 0xff;

                    minRed = minRed > (bytes[ind] & 0xff) ? (int) (bytes[ind] & 0xff) : minRed;
                    minGreen = minGreen > (bytes[gStart + ind] & 0xff) ? (int) (bytes[gStart + ind] & 0xff) : minGreen;
                    minBlue = minBlue > (bytes[bStart + ind] & 0xff) ? (int) (bytes[bStart + ind] & 0xff) : minBlue;

                    maxRed = maxRed < (bytes[ind] & 0xff) ? (int) (bytes[ind] & 0xff) : maxRed;
                    maxGreen = maxGreen < (bytes[gStart + ind] & 0xff) ? (int) (bytes[gStart + ind] & 0xff) : maxGreen;
                    maxBlue = maxBlue < (bytes[bStart + ind] & 0xff) ? (int) (bytes[bStart + ind] & 0xff) : maxBlue;
                }
            }

            red /= singleChannel;
            green /= singleChannel;
            blue /= singleChannel;
            System.out.println();
            System.out.println("---------");
            System.out.println(file.getName());
            System.out.println("---------");
            // System.out.println("minRed " + minRed);
            // System.out.println("maxRed " + maxRed);
            System.out.println("red " + red);
            // System.out.println("minGreen " + minGreen);
            // System.out.println("maxGreen " + maxGreen);
            System.out.println("green " + green);
            // System.out.println("minBlue " + minBlue);
            // System.out.println("maxBlue " + maxBlue);
            System.out.println("blue " + blue);

            float redE = 0;
            float greenE = 0;
            float blueE = 0;

            int drop = 50;

            for (int i = 0; i < singleChannel * drop / 100; i++) {

                int x = (int) (Math.random() * width);
                int y = (int) (Math.random() * height);

                ind = y * this.width + x;

                redE += Math.pow((bytes[ind] & 0xff) - red, 2);
                greenE += Math.pow((bytes[gStart + ind] & 0xff) - green, 2);
                blueE += Math.pow((bytes[bStart + ind] & 0xff) - blue, 2);
            }

            redE /= singleChannel;
            greenE /= singleChannel;
            blueE /= singleChannel;

            redE = (float) Math.sqrt(redE);
            greenE = (float) Math.sqrt(greenE);
            blueE = (float) Math.sqrt(blueE);
            System.out.println();
            // double a = ((255/red) + (255/green) +(255/blue))/3;
            // System.out.println("a "+ a);
            // double a = 0.00201;
            double a = 5.2 / Math.pow(50, 2);
            System.out.println("redE " + redE);
            System.out.println("greenE " + greenE);
            System.out.println("blueE " + blueE);
            System.out.println();
            System.out.println("red - " + (red + redE)/2 * drop * drop * a);
            System.out.println("green - " + (green + greenE)/2 * drop * drop * a);
            System.out.println("blue - " + (blue + blueE)/2 * drop * drop * a);
            System.out.println("all - " + (red+ redE + green+ greenE + blue+ blueE) / 6.0 * drop * drop * a);
            System.out.println("--");
        } catch (Exception e) {

        }
    }

    public static void main(String[] args) {

        File analysisFilePath = new File("./1920x1080_data_samples/");

        ArrayList<File> analysisFiles = new ArrayList<>();

        for (File file : analysisFilePath.listFiles()) {

            if (file.isFile() && file.getName().substring(file.getName().lastIndexOf('.')).equals(".rgb")) {
                analysisFiles.add(file);
            }

        }

        ImageAnalysis analyzer = new ImageAnalysis();
        // analyzer.processImages(analysisFiles);

        for (File f : analysisFiles)
            analyzer.stats(f);

    }
}
