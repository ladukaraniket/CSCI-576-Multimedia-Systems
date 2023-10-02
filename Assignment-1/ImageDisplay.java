
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
import javax.swing.WindowConstants;

class ImageParams {
	private int imageWidth;
	private int imageHeight;
	private int frameLength;
	byte[] red;
	byte[] green;
	byte[] blue;

	int imageScale;

	/**
	 * <pre>
	 *Reads the file at the given Path and intializes the ImageParams Object
	 * </pre>
	 * 
	 * @param imgPath
	 */
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
			this.imageHeight = 1080 * imageScale;

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
	int overlayOffset;
	float scalingFactor;
	float samplingFactor;
	boolean antiAliasingEnabled;

	public WindowParams() {

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

	public int getOverlayDim() {
		return overlayDim;
	}

	public void setOverlayDim(int overlayDim) {
		this.overlayDim = overlayDim;
		this.overlayOffset = overlayDim / 2;
	}

	public int getOverlayOffset() {
		return overlayOffset;
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

	boolean ctrlKeyPressed = false;

	/**
	 * <pre>
	 * Paints the initial view of scaled Image
	 * </pre>
	 */
	private void renderInitialImage() {
		try {
			// Render the image as per scale
			renderPartial(0, this.window.getWindowWidth(), 0, this.window.getWindowHeight());

			// Apply Anti-Aliasing
			if (this.window.antiAliasingEnabled) {
				applyPartialAntiAliasing(0, this.window.getWindowWidth(), 0, this.window.getWindowHeight());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <pre>
	 *Renders partial area of the canvas bound by the coordinates specified as inputs.
	 *This is useful while repainting the area once the overlay moves to a different location
	 * </pre>
	 */
	private void renderPartial(int renderStartX, int renderEndX, int renderStartY, int renderEndY) {

		renderStartX = renderStartX > 8 ? renderStartX : 0;
		renderStartY = renderStartY > 32 ? renderStartY : 0;
		renderEndX = renderEndX < this.window.getWindowWidth() ? renderEndX : this.window.getWindowWidth();
		renderEndY = renderEndY < this.window.getWindowHeight() ? renderEndY : this.window.getWindowHeight();

		int ind;
		for (float y = renderStartY * this.window.getSamplingFactor(); y < renderEndY
				* this.window.getSamplingFactor(); y += this.window.getSamplingFactor()) {
			int xcoord = renderStartX;
			for (float x = renderStartX * this.window.getSamplingFactor(); x < renderEndX
					* this.window.getSamplingFactor(); x += this.window.getSamplingFactor(), xcoord++) {

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

	/**
	 * <pre>
	 *Applies Anti-aliasing to partial area of the canvas bound by the coordinates specified as inputs.
	 *This is useful while applying AA to area repainted by renderPartial() instead of reapplying AA to the entire canvas.
	 * </pre>
	 */
	public void applyPartialAntiAliasing(int xStart, int xEnd, int yStart, int yEnd) {
		int AAGridSize = 2; // creates a W x W filter where W = 2*AAGridSize + 1
		long aaPixRed, aaPixGreen, aaPixBlue;
		int count;

		xStart = xStart > 8 ? xStart : 0;
		yStart = yStart > 32 ? yStart : 0;
		xEnd = xEnd < this.window.getWindowWidth() ? xEnd : this.window.getWindowWidth();
		yEnd = yEnd < this.window.getWindowHeight() ? yEnd : this.window.getWindowHeight();

		int ind;
		for (float y = yStart * this.window.getSamplingFactor(); y < yEnd
				* this.window.getSamplingFactor()
				&& yStart < this.window.getWindowHeight(); y += this.window.getSamplingFactor()) {
			int xcoord = xStart;
			for (float x = xStart * this.window.getSamplingFactor(); x < xEnd
					* this.window.getSamplingFactor()
					&& xcoord < this.window.getWindowWidth(); x += this.window.getSamplingFactor(), xcoord++) {

				aaPixRed = 0;
				aaPixGreen = 0;
				aaPixBlue = 0;
				count = 0;

				for (int aaRow = (int) x - AAGridSize; aaRow <= x + AAGridSize; aaRow++) {
					for (int aaCol = (int) y - AAGridSize; aaCol <= y + AAGridSize; aaCol++) {

						if (aaRow >= 0 && aaRow < this.image.getImageWidth() && aaCol >= 0
								&& aaCol < this.image.getImageHeight()) {

							ind = aaRow + aaCol * this.image.getImageWidth();

							aaPixRed += this.image.red[ind] & 0xff;
							aaPixGreen += this.image.green[ind] & 0xff;
							aaPixBlue += this.image.blue[ind] & 0xff;

							count++;
						}

					}
				}

				aaPixRed /= count;
				aaPixGreen /= count;
				aaPixBlue /= count;
				
				int aaPix = 0xff000000 | (((int) aaPixRed & 0xff) << 16) | (((int) aaPixGreen & 0xff) << 8)
						| ((int) aaPixBlue & 0xff);

				// TODO remove try catch
				this.imgOne.setRGB(xcoord, yStart, aaPix);

			}
			yStart++;
		}

	}

	
	public void applyPartialAntiAliasingOld(int xStart, int xEnd, int yStart, int yEnd) {
		int AAGridSize = 1; // creates a W x W filter where W = 2*AAGridSize + 1
		int pixVal;
		int aaPixRed, aaPixGreen, aaPixBlue;
		int count;

		xStart = xStart > 8 ? xStart : 0;
		yStart = yStart > 32 ? yStart : 0;
		xEnd = xEnd < this.window.getWindowWidth() ? xEnd : this.window.getWindowWidth();
		yEnd = yEnd < this.window.getWindowHeight() ? yEnd : this.window.getWindowHeight();

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

				this.imgOne.setRGB(row, col, aaPix);

			}
		}
	}

	/**
	 * <pre>
	 *Displays the overlay for a given position (x,y).
	 *The Overlay displays the part of the Image at its Native resolution.
	 * </pre>
	 * 
	 * @param x
	 * @param y
	 */
	public void displayOverLay(int x, int y) {

		int gridOffset = this.window.getOverlayOffset();
		int ind;

		int pointerCoordX = x - 8;
		int pointerCoordY = y - 32;

		int OGcoordX, OGcoordY;

		OGcoordX = (int) (pointerCoordX * this.window.getSamplingFactor());
		OGcoordY = (int) (pointerCoordY * this.window.getSamplingFactor());

		OGcoordX -= gridOffset;

		for (int boxRow = pointerCoordX - gridOffset; boxRow < pointerCoordX + gridOffset; boxRow++) {
			for (int boxCol = pointerCoordY - gridOffset,
					yCoord = OGcoordY - gridOffset; boxCol < pointerCoordY
							+ gridOffset; boxCol++) {

				if (boxRow >= 0
						&& boxRow < this.window.getWindowWidth() && boxCol >= 0
						&& boxCol < this.window.getWindowHeight() && OGcoordX > 0
						&& OGcoordX < this.image.getImageWidth()
						&& yCoord > 0
						&& yCoord < this.image.getImageHeight()) {

					ind = OGcoordX + yCoord * this.image.getImageWidth();

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

		this.window = new WindowParams();
		this.window.setScalingFactor(Float.parseFloat(args[1]));
		this.window.setAntiAliasingEnabled(Integer.parseInt(args[2]) == 1);
		this.window.setOverlayDim(Integer.parseInt(args[3]));
		this.window.setWindowHeight((int) (this.image.getImageHeight() * this.window.getScalingFactor()));
		this.window.setWindowWidth((int) (this.image.getImageWidth() * this.window.getScalingFactor()));

		// Read in the specified image
		this.imgOne = new BufferedImage(this.window.getWindowWidth(), this.window.getWindowHeight(),
				BufferedImage.TYPE_INT_RGB);

		renderInitialImage();

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		lbIm1 = new JLabel(new ImageIcon(imgOne));

		frame.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (!ctrlKeyPressed && e.getKeyCode() == 17) {
					ctrlKeyPressed = true;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (ctrlKeyPressed && e.getKeyCode() == 17) {
					ctrlKeyPressed = false;

					renderPartial(0, window.getWindowWidth(), 0, window.getWindowHeight());

					if (window.antiAliasingEnabled) {
						applyPartialAntiAliasing(0, window.getWindowWidth(), 0, window.getWindowHeight());
					}
					frame.repaint();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// No implementation required for this event
			}
		});
		frame.addMouseMotionListener(new MouseMotionListener() {

			int prevX;
			int prevY;

			@Override
			public void mouseDragged(MouseEvent e) {
				// No implementation required for this event
			}

			@Override
			public void mouseMoved(MouseEvent e) {

				if (ctrlKeyPressed) {
					renderPartial(prevX - window.getOverlayOffset() - 50, prevX + window.getOverlayOffset() + 50,
							prevY - window.getOverlayOffset() - 50, prevY + window.getOverlayOffset() + 50);

					if (window.antiAliasingEnabled) {
						applyPartialAntiAliasing(prevX - window.getOverlayOffset() - 50,
								prevX + window.getOverlayOffset() + 50,
								prevY - window.getOverlayOffset() - 50, prevY + window.getOverlayOffset() + 50);
					}
					displayOverLay(e.getX(), e.getY());
				}

				frame.repaint();
				prevX = e.getX();
				prevY = e.getY();
			}

		});

		frame.setResizable(false);

		StringBuilder title = new StringBuilder();
		String spacing = "      ";

		title.append("S: " + this.window.getScalingFactor());
		title.append(spacing);
		title.append("AA: " + this.window.antiAliasingEnabled);
		title.append(spacing);
		title.append("overlay: " + this.window.getOverlayDim() + "x" + this.window.getOverlayDim());
		title.append(spacing);
		title.append("this.window.getWindowWidth(): " + this.window.getWindowWidth());
		title.append(spacing);
		title.append("this.window.getWindowHeight(): " + this.window.getWindowHeight());

		frame.setTitle(title.toString());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

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
