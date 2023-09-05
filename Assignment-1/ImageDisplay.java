
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;

import javax.swing.*;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 1920; // default image width and height
	int height = 1080;

	/**
	 * Read Image RGB
	 * Reads the image of given width and height at the given imgPath into the
	 * provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			StringBuilder str = new StringBuilder();

			// for (int i = 0; i < 100; i++) {
			// 	str.append(bytes[i]);
			// 	str.append(" ");
			// }

			// System.out.println(str);

			int some =0;
			int ind = 0;
			// System.out.println("len " +bytes.length);
			// System.out.println("len " +bytes.length/3);
			// System.out.println("len " +bytes.length/3*2);

			byte[] red = Arrays.copyOfRange(bytes, 0, width*height);
			byte[] green = Arrays.copyOfRange(bytes, width*height, width*height*2);
			byte[] blue = Arrays.copyOfRange(bytes, width*height*2, bytes.length);
			
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					byte a = 0;
					// byte r = bytes[ind];
					// byte g = bytes[ind + height * width];
					// byte b = bytes[ind + height * width * 2];

					byte r = red[ind];
					byte g = green[ind];
					byte b = blue[ind];

					// while (some < 2) {
					// 	// str1.append(r);
					// 	// str1.append(" ");
					// 	// str1.append(g);
					// 	// str1.append(" ");
					// 	// str1.append(b);
					// 	// str1.append(" : ");
					// 	System.out.println(ind+" "+ind + height * width+" "+ind + height * width * 2);
					// 	System.out.println(r+" "+g+" "+b);
					// 	some++;
					// }

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void showIms(String[] args) {

		// Read a parameter from command line
		String param1 = args[1];
		System.out.println("The second parameter was: " + param1);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

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
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
