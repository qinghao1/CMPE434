import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

import javax.swing.JFrame;

public class PC extends JFrame {
	static InputStream inputStream;
	static DataInputStream dataInputStream;
	
	boolean mapMode = true; //Mapping or localization, true is map, false is localization 
	int locX, locY;
	byte[] maze = new byte[7 * 7 + 1 + 1];
	
	static byte[] test = {
	0x1, 0x44,
			
	(byte) 0x8D, 0x09, 0x0A, 0x0F,  0x0f, 0x0f, 0x0f,
	0x0c, 0x19, 0x00, 0x0b,  0x0f, 0x0f, 0x0f,
	0x07, 0x0c, 0x72, 0x0e,  0x0f, 0x0f, 0x0f,
	0x2d, 0x01, 0x41, 0x03,  0x0f, 0x0f, 0x0f,
	
	0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f,
	0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f,
	0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f,
	};
	
	public PC() {
		super("Map");
		setSize(450, 450);
		setVisible(true);
	}
	
	public static void main(String[] args) throws Exception	{
		byte[] inputBuffer = new byte[7 * 7 + 1 + 1]; //7*7 bytes for maze, 1 byte for mode, 1 byte for location 
		PC pc = new PC();
		
		pc.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		String ip = "10.0.1.1";
		
		@SuppressWarnings("resource")
		Socket socket = new Socket(ip, 1234);
		System.out.println("Connected!");
		
		inputStream = socket.getInputStream();
		dataInputStream = new DataInputStream(inputStream);
		
		while( true ){
			dataInputStream.readFully(inputBuffer);
			pc.processInput(inputBuffer);
			pc.repaint();
		}
		
//		pc.processInput(test);
//		pc.repaint();
		
	}

	
	public void paint( Graphics g ) {
		super.paint( g );
		drawGrid(g);
		if(mapMode) {
			drawMap(g);
		} else {
			drawLoc(g);
		}
	}
	
	public void processInput(byte[] inputArray) {
		mapMode = inputArray[0] == 0;
		byte locationData = inputArray[1];
		locY = locationData & 0x0F; //Lower 4 bits
		locX = locationData >> 4; //Upper 4 bits
		maze = Arrays.copyOfRange(inputArray, 2, inputArray.length);
	}
	
	public void drawGrid(Graphics g) {
		Graphics2D g2 = ( Graphics2D ) g;
		g2.setPaint( Color.GRAY );
		g2.setStroke( new BasicStroke( 1.0f ));
		
		//Cells are 50 pixels wide and tall
		//50 margin on each side
		
		for(int i = 0; i <= 7; i++) {
			//Horizontal
			g2.drawLine(50, i * 50 + 50, 400, i * 50 + 50);
			//Vertical
			g2.drawLine(i * 50 + 50, 50, i * 50 + 50, 400);
		}
	}
	
	public void drawMap(Graphics g) {
		Graphics2D g2 = ( Graphics2D ) g;
		g2.setPaint(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(4.0f));
		
		/*
		 * 1 byte used for each cell, as follows:
		 * 1 bit for localization
		 * 1 bit for giant cell (red)
		 * 1 bit for prince cell (green)
		 * 1 bit for ball cell (blue)
		 * 4 bits for 4 walls (top, left, right, down)
		 */
		
		int cellX = 0;
		int cellY = -1;
		
		//Top left to bottom right in row order
		
		for(int i = 0; i < maze.length; i++) {
			byte cell = maze[i];
			if (i % 7 == 0) cellY++;
			cellX = i % 7;
			
			//mapX, mapY are actual graphics coordinates of top left corner of cell
			int mapX = cellX * 50 + 50;
			int mapY = cellY * 50 + 50;

			if((cell & 112) == 112) {
				//Danger cell
				g2.setPaint(Color.GRAY);
				g2.fillRect(mapX, mapY, 50, 50);
			}else if((cell & 64) == 64) {
				//Giant cell
				g2.setPaint(Color.RED);
				g2.fillRect(mapX, mapY, 50, 50);
			} else if ((cell & 32) == 32) {
				//Prince cell
				g2.setPaint(Color.GREEN);
				g2.fillRect(mapX, mapY, 50, 50);
			} else if ((cell & 16) == 16) {
				//Ball cell
				g2.setPaint(Color.BLUE);
				g2.fillRect(mapX, mapY, 50, 50);
			}
			
			g2.setPaint(Color.BLACK);
			
			if((cell & 8) == 8) {
				//Top wall
				g2.drawLine(mapX, mapY, mapX + 50, mapY);
			}
			if((cell & 4) == 4) {
				//Left wall
				g2.drawLine(mapX, mapY, mapX, mapY + 50);
			}
			if((cell & 2) == 2) {
				//Right wall
				g2.drawLine(mapX + 50, mapY, mapX + 50, mapY + 50);
			}
			if((cell & 1) == 1) {
				//Bottom wall
				g2.drawLine(mapX, mapY + 50, mapX + 50, mapY + 50);
			}
			
		}
		
		//robotX, robotY are graphics coordinates of robot (draw robot in middle of cell)
		int robotX = locX * 50 + 60;
		int robotY = locY * 50 + 80;
		g2.drawString("RBT", robotX, robotY);
	}
	
	public void drawLoc(Graphics g) {
		Graphics2D g2 = ( Graphics2D ) g;
		g2.setPaint(Color.BLACK);
		g2.setStroke(new BasicStroke(3.0f));
		
		/*
		 * 1 byte used for each cell, as follows:
		 * 1 bit for localization (possible location)
		 * 1 bit for giant cell (red)
		 * 1 bit for prince cell (green)
		 * 1 bit for ball cell (blue)
		 * All cell bits on means danger cell (black)
		 * 4 bits for 4 walls (top, left, right, down)
		 */
		
		int cellX = 0;
		int cellY = -1;
		
		//Top left to bottom right in row order
		
		for(int i = 0; i < maze.length; i++) {
			byte cell = maze[i];
			if (i % 7 == 0) cellY++;
			cellX = i % 7;
			
//			System.out.println(String.format("%8s", Integer.toBinaryString(cell & 0xFF)).replace(' ', '0') + " " + cellX + " " + cellY);
			
			//mapX, mapY are actual graphics coordinates of top left corner of cell
			int mapX = cellX * 50 + 50;
			int mapY = cellY * 50 + 50;

			if((cell & 112) == 112) {
				//Danger cell
				g2.setPaint(Color.GRAY);
				g2.fillRect(mapX, mapY, 50, 50);
			}else if((cell & 64) == 64) {
				//Giant cell
				g2.setPaint(Color.RED);
				g2.fillRect(mapX, mapY, 50, 50);
			} else if ((cell & 32) == 32) {
				//Prince cell
				g2.setPaint(Color.GREEN);
				g2.fillRect(mapX, mapY, 50, 50);
			} else if ((cell & 16) == 16) {
				//Ball cell
				g2.setPaint(Color.BLUE);
				g2.fillRect(mapX, mapY, 50, 50);
			}
			
			g2.setPaint(Color.BLACK);
			
			if((cell & 8) == 8) {
				//Top wall
				g2.drawLine(mapX, mapY, mapX + 50, mapY);
			}
			if((cell & 4) == 4) {
				//Left wall
				g2.drawLine(mapX, mapY, mapX, mapY + 50);
			}
			if((cell & 2) == 2) {
				//Right wall
				g2.drawLine(mapX + 50, mapY, mapX + 50, mapY + 50);
			}
			if((cell & 1) == 1) {
				//Bottom wall
				g2.drawLine(mapX, mapY + 50, mapX + 50, mapY + 50);
			}
			
			if((cell & 128) == 128) {
				//Possible location
				g2.drawString("X", mapX + 20, mapY + 30);
			}
			
		}
	}

}

