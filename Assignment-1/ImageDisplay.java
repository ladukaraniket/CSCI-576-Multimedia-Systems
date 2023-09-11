
import java.awt.*;
import java.awt.image.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.Arrays;
import java.lang.*;
import javax.swing.*;

class ImageParams {
	private int imageWidth;
	private int imageHeight;
	private int frameLength;
	byte[] red;
	byte[] green;
	byte[] blue;
	byte[] redOG;
	byte[] greenOG;
	byte[] blueOG;

	boolean is1080;
	int imageScale;

	public ImageParams(String imgPath) {

		File file = new File(imgPath);

		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			this.imageScale = (int) Math.sqrt((int) raf.length() / (1920 * 1080 * 3));
			System.out.println("imageScale " + imageScale);
			this.setFrameLength((int) raf.length());

			byte[] bytes = new byte[this.frameLength];
			raf.read(bytes);
			raf.close();

			this.imageWidth = 1920 * imageScale;
			System.out.println("imageWidth " + imageWidth);
			this.imageHeight = 1080 * imageScale;
			System.out.println("imageHeight " + imageHeight);

			if (this.imageScale == 1) { // Image is a 1920 x 1080 image
				this.is1080 = true;

				this.red = Arrays.copyOfRange(bytes, 0, frameLength / 3);
				this.green = Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2);
				this.blue = Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength);

			} else {
				this.is1080 = false;

				this.redOG = Arrays.copyOfRange(bytes, 0, frameLength / 3);
				this.greenOG = Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2);
				this.blueOG = Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength);

				sampleTo1080(); // this will sample the large image to 1920 x 1080
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void sampleTo1080() {

		int pixInd;
		int pushInd = 0;

		this.red = new byte[2073600];
		this.green = new byte[2073600];
		this.blue = new byte[2073600];

		for (float row = 0; row < this.imageWidth; row += this.imageScale) {
			for (float col = 0; col < this.imageHeight; col += this.imageScale) {
				pixInd = (int) row + (int) col * this.imageWidth;

				this.red[pushInd] = this.redOG[pixInd];
				this.green[pushInd] = this.greenOG[pixInd];
				this.blue[pushInd] = this.blueOG[pixInd];

				pushInd++;
			}
		}
		System.out.println("pushInd " + pushInd);

	}

	public int getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(int imageWidth) {
		this.imageWidth = imageWidth;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(int imageHeight) {
		this.imageHeight = imageHeight;
	}

	public int getFrameLength() {
		return frameLength;
	}

	public void setFrameLength(int frameLength) {
		this.frameLength = frameLength;
	}

}

class WindowParams {
	int windowWidth;
	int windowHeight;
	int overlayDim;
	float scalingFactor;
	boolean antiAliasingEnabled;

	public int getOverlayDim() {
		return overlayDim;
	}

	public void setOverlayDim(int overlayDim) {
		this.overlayDim = overlayDim;
	}

	public WindowParams(int windowWidth, int windowHeight) {
		this.windowWidth = windowWidth;
		this.windowHeight = windowHeight;
	}

	public WindowParams(int windowWidth, int windowHeight, int scalingFactor, boolean antiAliasingEnabled) {
		this.windowWidth = windowWidth;
		this.windowHeight = windowHeight;
		this.scalingFactor = scalingFactor;
		this.antiAliasingEnabled = antiAliasingEnabled;
	}

	public int getWindowWidth() {
		return windowWidth;
	}

	public void setWindowWidth(int windowWidth) {
		this.windowWidth = windowWidth;
	}

	public int getWindowHeight() {
		return windowHeight;
	}

	public void setWindowHeight(int windowHeight) {
		this.windowHeight = windowHeight;
	}

	public float getScalingFactor() {
		return scalingFactor;
	}

	public void setScalingFactor(float scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public boolean isAntiAliasingEnabled() {
		return antiAliasingEnabled;
	}

	public void setAntiAliasingEnabled(boolean antiAliasingEnabled) {
		this.antiAliasingEnabled = antiAliasingEnabled;
	}

}

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;

	WindowParams window;
	ImageParams image;

	int offsetX;
	int offsetY;
	int newWidth;
	int newHeight;

	boolean ctrlKeyPressed = false;

	/**
	 * Read Image RGB
	 * Reads the image of given windowWidth and windowHeight at the given imgPath
	 * into the
	 * provided BufferedImage.
	 */
	private void readImageRGB(int windowWidth, int windowHeight, String imgPath, BufferedImage img) {
		try {
			int frameLength = windowWidth * windowHeight * 3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int some = 0;
			int ind = 0;

			byte[] red = Arrays.copyOfRange(bytes, 0, windowWidth * windowHeight);
			byte[] green = Arrays.copyOfRange(bytes, windowWidth * windowHeight, windowWidth * windowHeight * 2);
			byte[] blue = Arrays.copyOfRange(bytes, windowWidth * windowHeight * 2, bytes.length);

			for (int y = 0; y < windowHeight; y++) {
				for (int x = 0; x < windowWidth; x++) {
					// byte r = bytes[ind];
					// byte g = bytes[ind + windowHeight * windowWidth];
					// byte b = bytes[ind + windowHeight * windowWidth * 2];

					byte r = red[ind];
					byte g = green[ind];
					byte b = blue[ind];

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

	private void readScaledImageRGB() {
		try {
			int frameLength = this.window.getWindowWidth() * this.window.getWindowHeight() * 3;
			float samplingFactor = (float) (1.0 / this.window.getScalingFactor());
			this.offsetX = (int) ((1.0 - 1.0 / samplingFactor) / 2.0 * this.window.getWindowWidth());
			this.offsetY = (int) ((1.0 - 1.0 / samplingFactor) / 2.0 * this.window.getWindowHeight());
			this.newWidth = (int) (1.0 / samplingFactor * this.window.getWindowWidth());
			this.newHeight = (int) (1.0 / samplingFactor * this.window.getWindowHeight());

			System.out.println("frameLength " + frameLength);
			System.out.println("samplingFactor " + samplingFactor);
			System.out.println("offsetX " + offsetX);
			System.out.println("offsetY " + offsetY);
			System.out.println("newWidth " + newWidth);
			System.out.println("newHeight " + newHeight);

			int ind = 0;

			// Render the image as per scale
			int renderStartX = this.offsetX;
			int renderStartY = this.offsetY;
			for (float y = 0; y < this.window.getWindowHeight(); y += samplingFactor) {
				int xcoord = renderStartX;
				for (float x = 0; x < this.window.getWindowWidth(); x += samplingFactor, xcoord++) {

					ind = (int) x + (int) y * this.window.getWindowWidth();

					byte r = this.image.red[ind];
					byte g = this.image.green[ind];
					byte b = this.image.blue[ind];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

					// TODO remove try catch
					try {
						this.imgOne.setRGB(xcoord, renderStartY, pix);

					} catch (Exception e) {
						System.out.println("xcoord " + xcoord);
						break;
					}
				}
				renderStartY++;
			}

			// Apply Anti-Aliasing
			if (this.window.antiAliasingEnabled) {
				applyAntiAliasing();
			}

			// Display grid
			int gridSize = this.window.getOverlayDim();

			int pointerCoordX = offsetX + newWidth - 50;
			int pointerCoordY = offsetY + newHeight - 50;
			System.out.println("pointerCoordX " + pointerCoordX);
			System.out.println("pointerCoordY " + pointerCoordY);

			int OGcoordX, OGcoordY;

			OGcoordX = pointerCoordX
					+ (int) (Math.signum(pointerCoordX - (this.window.getWindowWidth() / 2))
							* (1.0 - this.window.getScalingFactor()) * this.window.getWindowWidth() / 2.0);
			OGcoordY = pointerCoordY
					+ (int) (Math.signum(pointerCoordY - (this.window.getWindowHeight() / 2))
							* (1.0 - this.window.getScalingFactor()) * this.window.getWindowHeight()
							/ 2.0);

			System.out.println("(1.0 - scaleFactor) * windowWidth / 2.0 "
					+ ((1.0 - this.window.getScalingFactor()) * this.window.getWindowWidth() / 2.0));
			System.out.println("(1.0 - scaleFactor) * windowHeight / 2.0 "
					+ (1.0 - this.window.getScalingFactor()) * this.window.getWindowHeight() / 2.0);
			System.out.println("OGcoordX " + OGcoordX);
			System.out.println("OGcoordY " + OGcoordY);

			for (int boxRow = pointerCoordX - gridSize; boxRow < pointerCoordX + gridSize; boxRow++) {
				for (int boxCol = pointerCoordY - gridSize,
						yCoord = OGcoordY; boxCol < pointerCoordY + gridSize; boxCol++) {

					if (boxRow >= offsetX && boxRow <= offsetX + newWidth && boxCol >= offsetY
							&& boxCol <= offsetY + newHeight && OGcoordX > 0 && OGcoordX < this.window.getWindowWidth()
							&& yCoord > 0
							&& yCoord < this.window.getWindowHeight()) {

						ind = (int) OGcoordX + (int) yCoord * this.window.getWindowWidth();
						int boxPix = 0xff000000 | ((image.red[ind] & 0xff) << 16) | ((image.green[ind] & 0xff) << 8)
								| (image.blue[ind] & 0xff);
						this.imgOne.setRGB(boxRow, boxCol, boxPix);
					}

					yCoord++;
				}
				OGcoordX++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void applyAntiAliasing() {
		int AAGridSize = 1; // creates a W x W filter where W = 2*AAGridSize + 1
		int pixVal;
		int aaPixRed, aaPixGreen, aaPixBlue;
		int count;

		for (int row = this.offsetX; row < this.offsetX + this.newWidth; row++) {
			for (int col = this.offsetY; col < this.offsetY + this.newHeight; col++) {
				aaPixRed = 0;
				aaPixGreen = 0;
				aaPixBlue = 0;
				count = 0;

				for (int aaRow = row - AAGridSize; aaRow <= row + AAGridSize; aaRow++) {
					for (int aaCol = col - AAGridSize; aaCol <= col + AAGridSize; aaCol++) {

						if (aaRow >= this.offsetX && aaRow <= this.offsetX + this.newWidth && aaCol >= this.offsetY
								&& aaCol <= this.offsetY + this.newHeight) {

							pixVal = this.imgOne.getRGB(aaRow, aaCol);

							aaPixRed += (pixVal >> 16) & 0xff;
							aaPixGreen += (pixVal >> 8) & 0xff;
							aaPixBlue += pixVal & 0xff;

							count++;
						}

					}
				}

				int aaPix = 0xff000000 | ((aaPixRed / count & 0xff) << 16) | ((aaPixGreen / count & 0xff) << 8)
						| (aaPixBlue / count & 0xff);

				try {
					this.imgOne.setRGB(row, col, aaPix);

				} catch (Exception e) {
					System.out.println("col " + col);
					System.out.println("row " + row);

					break;
				}
			}
		}
	}

	public void showIms(String[] args) {

		this.window = new WindowParams(1920, 1080);

		this.window.setScalingFactor(Float.parseFloat(args[1]));
		this.window.setAntiAliasingEnabled(Integer.parseInt(args[2]) == 1);
		this.window.setOverlayDim(Integer.parseInt(args[3]));

		this.image = new ImageParams(args[0]);

		// Read in the specified image
		this.imgOne = new BufferedImage(this.window.getWindowWidth(), this.window.getWindowHeight(),
				BufferedImage.TYPE_INT_RGB);
				
		readScaledImageRGB();

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		lbIm1 = new JLabel(new ImageIcon(imgOne));

		frame.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				if (!ctrlKeyPressed && e.getKeyCode() == 17) {
					ctrlKeyPressed = true;
					display();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				if (ctrlKeyPressed && e.getKeyCode() == 17) {
					ctrlKeyPressed = false;
					display();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {

			}
		});
		frame.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseMoved(MouseEvent e) {
				// TODO Auto-generated method stub
				System.out.println("mouse X,Y - (" + e.getX() + "," + e.getY() + ")");
			}

		});

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

	void display(){
		System.out.println(this.ctrlKeyPressed);
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
