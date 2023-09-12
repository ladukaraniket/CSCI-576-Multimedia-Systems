
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

class ImageParams {
	private int imageWidth;
	private int imageHeight;
	private int frameLength;
	byte[] red;
	byte[] green;
	byte[] blue;

	int imageScale;

	public ImageParams(String imgPath) {

		File file = new File(imgPath);

		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			this.imageScale = (int) Math.sqrt((int) raf.length() / (1920 * 1080 * 3));
			this.setFrameLength((int) raf.length());

			byte[] bytes = new byte[this.frameLength];
			raf.read(bytes);
			raf.close();

			this.imageWidth = 1920 * imageScale;
			System.out.println("imageWidth " + imageWidth);
			this.imageHeight = 1080 * imageScale;
			System.out.println("imageHeight " + imageHeight);

			this.red = Arrays.copyOfRange(bytes, 0, frameLength / 3);
			this.green = Arrays.copyOfRange(bytes, frameLength / 3, frameLength / 3 * 2);
			this.blue = Arrays.copyOfRange(bytes, frameLength / 3 * 2, frameLength);

		} catch (Exception e) {
			e.printStackTrace();
		}

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
	float samplingFactor;
	boolean antiAliasingEnabled;

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

	public int getOverlayDim() {
		return overlayDim;
	}

	public void setOverlayDim(int overlayDim) {
		this.overlayDim = overlayDim;
	}

	public float getScalingFactor() {
		return scalingFactor;
	}

	public void setScalingFactor(float scalingFactor) {
		this.scalingFactor = scalingFactor;
		this.samplingFactor = (float) (1.0 / scalingFactor);
	}

	public boolean isAntiAliasingEnabled() {
		return antiAliasingEnabled;
	}

	public void setAntiAliasingEnabled(boolean antiAliasingEnabled) {
		this.antiAliasingEnabled = antiAliasingEnabled;
	}

	public float getSamplingFactor() {
		return samplingFactor;
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
	private void readScaledImageRGB() {
		try {
			// this.offsetX = (int) ((1.0 - 1.0 / samplingFactor) / 2.0 *
			// this.window.getWindowWidth());
			// this.offsetY = (int) ((1.0 - 1.0 / samplingFactor) / 2.0 *
			// this.window.getWindowHeight());
			this.newWidth = (int) (1.0 / this.window.getSamplingFactor() * this.window.getWindowWidth());
			this.newHeight = (int) (1.0 / this.window.getSamplingFactor() * this.window.getWindowHeight());

			System.out.println("samplingFactor " + this.window.getSamplingFactor());
			System.out.println("offsetX " + offsetX);
			System.out.println("offsetY " + offsetY);
			System.out.println("newWidth " + newWidth);
			System.out.println("newHeight " + newHeight);

			// Render the image as per scale
			renderDefault();

			// Apply Anti-Aliasing
			if (this.window.antiAliasingEnabled) {
				// applyAntiAliasing();
				applyPartialAntiAliasing(0, this.newWidth, 0, this.newHeight);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void renderDefault() {
		int renderStartX = 0;
		int renderStartY = 0;
		int ind;
		for (float y = 0; y < this.image.getImageHeight(); y += this.window.getSamplingFactor()) {
			int xcoord = renderStartX;
			for (float x = 0; x < this.image.getImageWidth(); x += this.window.getSamplingFactor(), xcoord++) {

				ind = (int) x + (int) y * this.image.getImageWidth();

				byte r = this.image.red[ind];
				byte g = this.image.green[ind];
				byte b = this.image.blue[ind];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

				// TODO remove try catch
				try {
					this.imgOne.setRGB(xcoord, renderStartY, pix);

				} catch (Exception e) {
					break;
				}
			}
			renderStartY++;
		}
	}

	private void renderPartial(int renderStartX, int renderEndX, int renderStartY, int renderEndY) {
		// renderStartX = 0;
		// renderStartY = 0;

		renderStartX = renderStartX > 8 ? renderStartX : 0;
		renderStartY = renderStartY > 32 ? renderStartY : 0;
		renderEndX = renderEndX < this.newWidth ? renderEndX : this.newWidth;
		renderEndY = renderEndY < this.newHeight ? renderEndY : this.newHeight;

		int ind;
		for (float y = renderStartY * this.window.getSamplingFactor(); y < renderEndY
				* this.window.getSamplingFactor(); y += this.window.getSamplingFactor()) {
			int xcoord = renderStartX;
			for (float x = renderStartX * this.window.getSamplingFactor(); x < renderEndX
					* this.window.getSamplingFactor(); x += this.window.getSamplingFactor(), xcoord++) {

				ind = (int) x + (int) y * this.window.getWindowWidth();

				byte r = this.image.red[ind];
				byte g = this.image.green[ind];
				byte b = this.image.blue[ind];

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);

				// TODO remove try catch
				try {
					this.imgOne.setRGB(xcoord, renderStartY, pix);

				} catch (Exception e) {
					break;
				}
			}
			renderStartY++;
		}
	}

	public void applyAntiAliasing() {
		int AAGridSize = 1; // creates a W x W filter where W = 2*AAGridSize + 1
		int pixVal;
		int aaPixRed, aaPixGreen, aaPixBlue;
		int count;

		for (int row = 0; row < this.newWidth; row++) {
			for (int col = 0; col < this.newHeight; col++) {
				aaPixRed = 0;
				aaPixGreen = 0;
				aaPixBlue = 0;
				count = 0;

				for (int aaRow = row - AAGridSize; aaRow <= row + AAGridSize; aaRow++) {
					for (int aaCol = col - AAGridSize; aaCol <= col + AAGridSize; aaCol++) {

						if (aaRow >= 0 && aaRow < this.newWidth && aaCol >= 0
								&& aaCol < this.newHeight) {

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
					break;
				}
			}
		}
	}

	public void applyPartialAntiAliasing(int xStart, int xEnd, int yStart, int yEnd) {
		int AAGridSize = 1; // creates a W x W filter where W = 2*AAGridSize + 1
		int pixVal;
		int aaPixRed, aaPixGreen, aaPixBlue;
		int count;

		xStart = xStart > 8 ? xStart : 0;
		yStart = yStart > 32 ? yStart : 0;
		xEnd = xEnd < this.newWidth ? xEnd : this.newWidth;
		yEnd = yEnd < this.newHeight ? yEnd : this.newHeight;

		for (int row = xStart; row < xEnd; row++) {
			for (int col = yStart; col < yEnd; col++) {
				aaPixRed = 0;
				aaPixGreen = 0;
				aaPixBlue = 0;
				count = 0;

				for (int aaRow = row - AAGridSize; aaRow <= row + AAGridSize; aaRow++) {
					for (int aaCol = col - AAGridSize; aaCol <= col + AAGridSize; aaCol++) {

						if (aaRow >= xStart && aaRow < xEnd && aaCol >= 0
								&& aaCol < yEnd) {

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
					break;
				}
			}
		}
	}

	public void displayOverLay(int x, int y) {

		int gridSize = this.window.getOverlayDim();
		int ind;

		int pointerCoordX = x - 8;
		int pointerCoordY = y - 32;

		int OGcoordX, OGcoordY;

		OGcoordX = (int) (pointerCoordX * this.window.getSamplingFactor());
		OGcoordY = (int) (pointerCoordY * this.window.getSamplingFactor());

		OGcoordX -= this.window.getOverlayDim();

		for (int boxRow = pointerCoordX - gridSize; boxRow < pointerCoordX + gridSize; boxRow++) {
			for (int boxCol = pointerCoordY - gridSize,
					yCoord = OGcoordY - this.window.getOverlayDim(); boxCol < pointerCoordY + gridSize; boxCol++) {

				if (boxRow >= 0 && boxRow < newWidth && boxCol >= 0
						&& boxCol < newHeight && OGcoordX > 0 && OGcoordX < this.image.getImageWidth()
						&& yCoord > 0
						&& yCoord < this.image.getImageHeight()) {

					ind = (int) OGcoordX + (int) yCoord * this.image.getImageWidth();
					int boxPix = 0xff000000 | ((image.red[ind] & 0xff) << 16) | ((image.green[ind] & 0xff) << 8)
							| (image.blue[ind] & 0xff);
					this.imgOne.setRGB(boxRow, boxCol, boxPix);
				}

				yCoord++;
			}
			OGcoordX++;
		}
	}

	public void showIms(String[] args) {

		this.image = new ImageParams(args[0]);

		this.window = new WindowParams(this.image.getImageWidth(), this.image.getImageHeight());
		this.window.setScalingFactor(Float.parseFloat(args[1]));
		this.window.setAntiAliasingEnabled(Integer.parseInt(args[2]) == 1);
		this.window.setOverlayDim(Integer.parseInt(args[3]));

		// Read in the specified image
		this.imgOne = new BufferedImage((int) (this.window.getWindowWidth() * this.window.getScalingFactor()),
				(int) (this.window.getWindowHeight() * this.window.getScalingFactor()),
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
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				if (ctrlKeyPressed && e.getKeyCode() == 17) {
					ctrlKeyPressed = false;
					// renderDefault();
					renderPartial(0, newWidth, 0, newHeight);

					if (window.antiAliasingEnabled) {
						applyPartialAntiAliasing(0, newWidth, 0, newHeight);
					}
					frame.repaint();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {

			}
		});
		frame.addMouseMotionListener(new MouseMotionListener() {

			int prevX;
			int prevY;

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseMoved(MouseEvent e) {

				renderPartial(prevX - window.getOverlayDim() - 50, prevX + window.getOverlayDim() + 50,
						prevY - window.getOverlayDim() - 50, prevY + window.getOverlayDim() + 50);

				if (window.antiAliasingEnabled) {
					applyPartialAntiAliasing(prevX - window.getOverlayDim() - 50, prevX + window.getOverlayDim() + 50,
							prevY - window.getOverlayDim() - 50, prevY + window.getOverlayDim() + 50);
				}

				if (ctrlKeyPressed) {
					displayOverLay(e.getX(), e.getY());
				}

				frame.repaint();
				prevX = e.getX();
				prevY = e.getY();
			}

		});

		frame.setResizable(false);

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
