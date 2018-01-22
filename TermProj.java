import lejos.hardware.*;
import lejos.hardware.ev3.*;
import lejos.hardware.lcd.*;
import lejos.hardware.motor.*;
import lejos.hardware.port.*;
import lejos.remote.nxt.*;
import lejos.hardware.sensor.*;
import lejos.robotics.*;
import lejos.utility.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.*;

public class TermProj {
	final static int defaultSpeed = 200;

	static EV3 ev3 = (EV3) BrickFinder.getDefault();
	
	static GraphicsLCD graphicsLCD = ev3.getGraphicsLCD();

	static EV3UltrasonicSensor ultrasonicSensor = new EV3UltrasonicSensor(SensorPort.S4);
	static SampleProvider ultrasonicProvider = ultrasonicSensor.getDistanceMode();

	static EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S3);
	static SampleProvider colorProvider = colorSensor.getColorIDMode();
	
	static NXTUltrasonicSensor ballSensor = new NXTUltrasonicSensor(SensorPort.S2);
	static SampleProvider ballProvider = ballSensor.getDistanceMode();
	
	static EV3GyroSensor gyroSensor = new EV3GyroSensor(SensorPort.S1);
	static SampleProvider gyroProvider = gyroSensor.getAngleMode();

	//Basic functions
		
	/* Function to show string on LCD display
	 * @param s string to display
	 */
	private static void displayString(String s) {
		graphicsLCD.clear();
		graphicsLCD.drawString(s, graphicsLCD.getWidth()/2, graphicsLCD.getHeight()/2, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
	}
	
	private static void setSpeed(int spd) {
		Motor.A.setSpeed(spd);
		Motor.D.setSpeed(spd);
	}
	
	private static void setDefaultSpeed() {
		Motor.A.setSpeed(defaultSpeed);
		Motor.D.setSpeed(defaultSpeed);
	}
	
	/* Turn using gyroscope to measure direction
	 * @param angle positive is counter-clockwise
	 */
	private static void turnGyro(int angle) {
		setSpeed(50);
		float startAngle = getGyro();
		float endAngle = angle < 0 ? startAngle + angle + 2 : startAngle + angle - 2; //+-2 to adjust for error

//		//Round to nearest 10
//		endAngle = Math.round(endAngle / 10f) * 10f;
		
		if (angle > 0) {
			Motor.A.forward();
			Motor.D.backward();
		} else {
			Motor.A.backward();
			Motor.D.forward();
		}
			
		while (angle > 0 ? getGyro() < endAngle : getGyro() > endAngle) {
			//Keep turning
		}
			
		Motor.A.stop(true);
		Motor.D.stop(true);
		
		Delay.msDelay(200);
			
		setDefaultSpeed();
	}
	
	private static float getDistanceInDirection(int dir) {
		float [] sample = new float[ultrasonicProvider.sampleSize()];
		float distance;
		
		if (currentDirection == 0) {
			switch (dir) {
			case 1: Motor.C.rotate(-90);
					break;
			case 2: Motor.C.rotate(90);
					break;
			case 3: Motor.C.rotate(180);
					break;
			}
		
			ultrasonicProvider.fetchSample(sample, 0);
			distance = sample[0];

			switch (dir) {
			case 1: Motor.C.rotate(90);
					break;
			case 2: Motor.C.rotate(-90);
					break;
			case 3: Motor.C.rotate(-180);
					break;
			}
		} else if (currentDirection == 1) {
			switch (dir) {
			case 0: Motor.C.rotate(90);
					break;
			case 2: Motor.C.rotate(180);
					break;
			case 3: Motor.C.rotate(-90);
					break;
			}
		
			ultrasonicProvider.fetchSample(sample, 0);
			distance = sample[0];

			switch (dir) {
			case 0: Motor.C.rotate(-90);
					break;
			case 2: Motor.C.rotate(-180);
					break;
			case 3: Motor.C.rotate(90);
					break;
			}
		} else if (currentDirection == 2) {
			switch (dir) {
			case 0: Motor.C.rotate(-90);
					break;
			case 1: Motor.C.rotate(-180);
					break;
			case 3: Motor.C.rotate(90);
					break;
			}
		
			ultrasonicProvider.fetchSample(sample, 0);
			distance = sample[0];

			switch (dir) {
			case 0: Motor.C.rotate(90);
					break;
			case 1: Motor.C.rotate(180);
					break;
			case 3: Motor.C.rotate(-90);
					break;
			}
		} else {
//			currentDirection == 3
			switch (dir) {
			case 0: Motor.C.rotate(180);
					break;
			case 1: Motor.C.rotate(90);
					break;
			case 2: Motor.C.rotate(-90);
					break;
			}
		
			ultrasonicProvider.fetchSample(sample, 0);
			distance = sample[0];

			switch (dir) {
			case 0: Motor.C.rotate(-180);
					break;
			case 1: Motor.C.rotate(-90);
					break;
			case 2: Motor.C.rotate(90);
					break;
			}
		}
		
			return distance;
	}
	
	private static boolean wallInDirection(int dir) {
		float WALL_THRESHOLD = 0.4f;
		return getDistanceInDirection(dir) < WALL_THRESHOLD;
	}
	
	private static void resetDirection() {
		turnGyro((int)(0-Math.round(getGyro())));
		currentDirection = 0;
	}
	
	//{NONE, BLACK, BLUE, GREEN, YELLOW, RED, WHITE, BROWN}
	private static int getColor() {
			float [] sample = new float[colorProvider.sampleSize()];
			colorProvider.fetchSample(sample, 0);
			float color = sample[0];
			
			int colorInt = (int) color;
			
			
			//Switch needed because color specified in lejOS API is different from actual value
			switch(colorInt) {
			case 6: return 0; //None
			case 7: return 1; //Black
			case 2: return 2; //Blue
			case 1: return 3; //Green
			case 0: return 5; //Red
			}
			
			return 0;
	}
	
	static boolean hasBall = false;
	
	private static boolean atBall() {
		if (hasBall || !ballExists) return false;
		final float BALL_THRESHOLD = 0.1f;
		
		float [] sample = new float[ballProvider.sampleSize()];
		ballProvider.fetchSample(sample, 0);
		float dist = sample[0];
		
		return dist < BALL_THRESHOLD;
	}
	
	private static void getBall() {
		if(!hasBall) Motor.B.rotate(130);
		hasBall = true;
	}
	
	private static float getGyro() {
		float [] sample = new float[gyroProvider.sampleSize()];
		gyroProvider.fetchSample(sample, 0);
		float angle = sample[0];
		
		return angle;
	}
	
	private static void moveForward(int cells) {
		final int DEG = 705;
		Motor.A.rotate(DEG * cells, true);
		Motor.D.rotate(DEG * cells, true);
		while(Motor.A.isMoving()) {
			if (atBall()) getBall();
		}
//		Motor.A.stop(true);
//		Motor.D.stop(true);
		
		Delay.msDelay(200);
	}
	
	//Where the robot is facing. 0 is start direction, 1 is left, 2 is right, 3 is behind
	static int currentDirection = 0;
	
	static int turnCt = 0;
	private static void turnDirection(int newDirection) {
		
		if (currentDirection == 0) {
			switch(newDirection) {
			case 1: turnGyro(90);
					break;
			case 2: turnGyro(-90);
					break;
			case 3: turnGyro(180);
					break;
			}
		} else if (currentDirection == 1) {
			switch(newDirection) {
			case 0: turnGyro(-90);
					break;
			case 2: turnGyro(180);
					break;
			case 3: turnGyro(90);
					break;
			}
		} else if (currentDirection == 2) {
			switch(newDirection) {
			case 0: turnGyro(90);
					break;
			case 1: turnGyro(180);
					break;
			case 3: turnGyro(-90);
					break;
			}
		} else if (currentDirection == 3) {
			switch(newDirection) {
			case 0: turnGyro(180);
					break;
			case 1: turnGyro(-90);
					break;
			case 2: turnGyro(90);
					break;
			}
		}
		
		currentDirection = newDirection;
	}
	
	//Higher level functions
	private static void moveInDir(int dir) {
		if(currentDirection != dir) turnDirection(dir);
		moveForward(1);
		switch(dir) {
		case 0:
			currentRow -= 1;
			break;
		case 1:
			currentCol -= 1;
			break;
		case 2:
			currentCol += 1;
			break;
		case 3:
			currentRow += 1;
			break;
		}
	}
	
	static Cell[][] internalMap = new Cell[7][7];
	static int[][] exploredStatus = new int[7][7]; //0 is unexplored, 1 is giant, 2 is seen, 3 is visited
	static Cell ballCell = null;
	static Cell giantCell = null;
	static Cell princeCell = null;
	static int currentRow;
	static int currentCol;
	
	private static ArrayList<Cell> findWayToCell(Cell begin, Cell end) {
		Queue<BFSCell> bfs = new LinkedList<>();
		Set<Cell> visited = new HashSet<>();
		
		BFSCell first = new BFSCell(begin, null);
		bfs.add(first);
		visited.add(begin);
		
		while(!bfs.isEmpty() && bfs.peek().cell != end) {
			BFSCell currBFS = bfs.poll();
			Cell curr = currBFS.cell;
			visited.add(curr);
			
			if (curr.N != null && !visited.contains(curr.N)) bfs.add(new BFSCell(curr.N, currBFS));
			if (curr.S != null && !visited.contains(curr.S)) bfs.add(new BFSCell(curr.S, currBFS));
			if (curr.E != null && !visited.contains(curr.E)) bfs.add(new BFSCell(curr.E, currBFS));
			if (curr.W != null && !visited.contains(curr.W)) bfs.add(new BFSCell(curr.W, currBFS));
		}
		
		BFSCell last = bfs.peek();
		
		ArrayList<Cell> path = new ArrayList<>();
		
		while(last.previous != null) {
			path.add(last.cell);
			last = last.previous;
		}
		
		return path;
	}
	
	private static void exploreCell(Cell currentCell) {
		int cellColor = getColor();
		if (cellColor == 2) {
			ballCell = currentCell;
		} else if (cellColor == 3) {
			princeCell = currentCell;
		}
		currentCell.color = cellColor;
		if (currentCell.color == 1) {
			//Mark possible giant locations as such
			//"Up"
			if (!wallInDirection(0)) {
				exploredStatus[currentRow - 1][currentCol] = Math.max(exploredStatus[currentRow - 1][currentCol], 1);
				currentCell.N = internalMap[currentRow - 1][currentCol];
				internalMap[currentRow - 1][currentCol].S = currentCell;
			}
			//"Left"
			if (!wallInDirection(1)) {
				exploredStatus[currentRow][currentCol - 1] = Math.max(exploredStatus[currentRow][currentCol - 1], 1);
				currentCell.W = internalMap[currentRow][currentCol - 1];
				internalMap[currentRow][currentCol - 1].E = currentCell;
			}
			//"Right"
			if (!wallInDirection(2)) {
				exploredStatus[currentRow][currentCol + 1] = Math.max(exploredStatus[currentRow][currentCol + 1], 1);
				currentCell.E = internalMap[currentRow][currentCol + 1];
				internalMap[currentRow][currentCol + 1].W = currentCell;
			}
			//"Down"
			if (!wallInDirection(3)) {
				exploredStatus[currentRow + 1][currentCol] = Math.max(exploredStatus[currentRow + 1][currentCol], 1);
				currentCell.S = internalMap[currentRow + 1][currentCol];
				internalMap[currentRow + 1][currentCol].N = currentCell;
			}
		} else {
			//"Up"
			if (!wallInDirection(0)) {
				exploredStatus[currentRow - 1][currentCol] = Math.max(exploredStatus[currentRow - 1][currentCol], 2);
				currentCell.N = internalMap[currentRow - 1][currentCol];
				internalMap[currentRow - 1][currentCol].S = currentCell;
			}
			//"Left"
			if (!wallInDirection(1)) {
				exploredStatus[currentRow][currentCol - 1] = Math.max(exploredStatus[currentRow][currentCol - 1], 2);
				currentCell.W = internalMap[currentRow][currentCol - 1];
				internalMap[currentRow][currentCol - 1].E = currentCell;
			}
			//"Right"
			if (!wallInDirection(2)) {
				exploredStatus[currentRow][currentCol + 1] = Math.max(exploredStatus[currentRow][currentCol + 1], 2);
				currentCell.E = internalMap[currentRow][currentCol + 1];
				internalMap[currentRow][currentCol + 1].W = currentCell;
			}
			//"Down"
			if (!wallInDirection(3)) {
				exploredStatus[currentRow + 1][currentCol] = Math.max(exploredStatus[currentRow + 1][currentCol], 2);
				currentCell.S = internalMap[currentRow + 1][currentCol];
				internalMap[currentRow + 1][currentCol].N = currentCell;
			}
		}
		exploredStatus[currentRow][currentCol] = 3;
	}
	
	private static Cell nextUnexploredCell() {
		//Try closest cells (Front, Left, Back) first
		switch(currentDirection) {
		//N
		case 0:
			//Front
			if(currentRow > 0 && exploredStatus[currentRow - 1][currentCol] == 2) return internalMap[currentRow - 1][currentCol];
			//Left
			if(currentCol > 0 && exploredStatus[currentRow][currentCol - 1] == 2) return internalMap[currentRow][currentCol - 1];
			//Right
			if(currentCol < 6 && exploredStatus[currentRow][currentCol + 1] == 2) return internalMap[currentRow][currentCol + 1];
		//W
		case 1:
			//Front(Left)
			if(currentCol > 0 && exploredStatus[currentRow][currentCol - 1] == 2) return internalMap[currentRow][currentCol - 1];
			//Left(Back)
			if(currentRow < 6 && exploredStatus[currentRow + 1][currentCol] == 2) return internalMap[currentRow + 1][currentCol];
			//Right(Front)
			if(currentRow > 0 && exploredStatus[currentRow - 1][currentCol] == 2) return internalMap[currentRow - 1][currentCol];
		//E
		case 2:
			//Front(Right)
			if(currentCol < 6 && exploredStatus[currentRow][currentCol + 1] == 2) return internalMap[currentRow][currentCol + 1];
			//Left(Front)
			if(currentRow > 0 && exploredStatus[currentRow - 1][currentCol] == 2) return internalMap[currentRow - 1][currentCol];
			//Right(Back)
			if(currentRow < 6 && exploredStatus[currentRow + 1][currentCol] == 2) return internalMap[currentRow + 1][currentCol];
		//S
		case 3:
			//Front(Back)
			if(currentRow < 6 && exploredStatus[currentRow + 1][currentCol] == 2) return internalMap[currentRow + 1][currentCol];
			//Left(Right)
			if(currentCol < 6 && exploredStatus[currentRow][currentCol + 1] == 2) return internalMap[currentRow][currentCol + 1];
			//Right(Left)
			if(currentCol > 0 && exploredStatus[currentRow][currentCol - 1] == 2) return internalMap[currentRow][currentCol - 1];
		};
		
		//Iterate through exploredStatus
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				if (exploredStatus[i][j] == 2) return internalMap[i][j];
			}
		}
		
		return null;
	}
	
	private static void moveTo(Cell currentCell, Cell nextCell) {

		if(currentCell == nextCell) return;
		if(currentCell == null || nextCell == null) return;
		
		displayMap();

		ArrayList<Cell> path = findWayToCell(currentCell, nextCell);
		for(int i = path.size() - 1; i >= 0; i--) {
			Cell c = path.get(i);
			if (currentCell.row != c.row) {
				//Down
				if (c.row > currentCell.row) {
					moveInDir(3);
				}
				//Up
				else {
					moveInDir(0);
				}
			} else {
				//Right
				if (c.col > currentCell.col) {
					moveInDir(2);
				}
				else {
				//Left
					moveInDir(1);
				}
			}
			currentCell = c;
			displayMap();
		}
	}
	
	static boolean ballExists = false;
	
	private static void mapping() {
	    Button.ESCAPE.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(Key k) {
			}

			@Override
			public void keyReleased(Key k) {
				graphicsLCD.clear();
				graphicsLCD.drawString("RESET", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
				graphicsLCD.drawString("Mapping", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
				graphicsLCD.refresh();
				Button.waitForAnyPress();
				mapping();
			}
	    });
	    
		//Initialize internalMap and exploredStatus
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				internalMap[i][j] = new Cell(i, j, null, null, null, null, 0);
				exploredStatus[i][j] = 0;
			}
		}
		
		gyroSensor.reset();
	    
		ballExists = false;
		currentRow = currentCol = 3;
		Cell currentCell = internalMap[3][3];
		exploreCell(currentCell);
		Cell nextCell = nextUnexploredCell();
		while(nextCell != null) {
			moveTo(currentCell, nextCell);
			currentCell = nextCell;
			exploreCell(currentCell);
			nextCell = nextUnexploredCell();
			if(Button.readButtons() == Button.ID_ENTER) Button.waitForAnyPress();
			if(Button.readButtons() == Button.ID_ESCAPE) return;
		}
		
		//Mark giant cell (there should be 1 and only 1)
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				if (exploredStatus[i][j] == 1) giantCell = internalMap[i][j];
			}
		}
		
		displayMap();
		
		//Play 3 sounds
		Sound.beep();
		Sound.beep();
		Sound.beep();
		
		control();
	}
	
	static Set<Cell> possibleLocations = new HashSet<>();
	
	private static void localize(Set<Cell> possibilities) {
		while(possibilities.size() > 1) {
			displayLocalize();
			if(Button.readButtons() == Button.ID_ENTER) Button.waitForAnyPress();
			if(Button.readButtons() == Button.ID_ESCAPE) return;
			
			Set<Cell> toBeRemoved = new HashSet<>();
			Set<Cell> toBeAdded = new HashSet<>();
			
			int cellColor = getColor();
			for(Cell c : possibilities) {
				if (c.color != cellColor) toBeRemoved.add(c);
			}
			
			possibilities.removeAll(toBeRemoved);
			toBeRemoved.clear();
			
			displayLocalize();
			
			if (cellColor == 1 && !hasBall) {
				//Accidentally moved near giant without weapon, retreat
				int backDir = 3 - currentDirection;
				moveInDir(backDir); //Robot will face backDir, so won't explore same direction
				switch(backDir) {
					case 0:
						for(Cell c : possibilities) {
							toBeRemoved.add(c);
							toBeAdded.add(c.N);
						}
						break;
					case 1:
						for(Cell c : possibilities) {
							toBeRemoved.add(c);
							toBeAdded.add(c.W);
						}
						break;
					case 2:
						for(Cell c : possibilities) {
							toBeRemoved.add(c);
							toBeAdded.add(c.E);
						}
						break;
					case 3:
						for(Cell c : possibilities) {
							toBeRemoved.add(c);
							toBeAdded.add(c.S);
						}
						break;
					}
			}
			
			possibilities.removeAll(toBeRemoved);
			possibilities.addAll(toBeAdded);
			
			toBeRemoved.clear();
			toBeAdded.clear();
			
			boolean[] wall = new boolean[4];
			for(int i = 0; i < 4; i++) {
				wall[i] = wallInDirection(i);
				switch(i) {
				case 0:
					if(wall[i]) {
						for(Cell c : possibilities) {
							if (c.N != null) toBeRemoved.add(c);
						}
					}
					break;
				case 1:
					if(wall[i]) {
						for(Cell c : possibilities) {
							if (c.W != null) toBeRemoved.add(c);
						}
					}
					break;
				case 2:
					if(wall[i]) {
						for(Cell c : possibilities) {
							if (c.E != null) toBeRemoved.add(c);
						}
					}
					break;
				case 3:
					if(wall[i]) {
						for(Cell c : possibilities) {
							if (c.S != null) toBeRemoved.add(c);
						}
					}
					break;
				}
			}
			
			possibilities.removeAll(toBeRemoved);
			toBeRemoved.clear();
			
			displayLocalize();
			
			if(possibilities.size() > 1) {
				for(int i = 0; i < 4; i++) {
					if(!wall[i]) {
						moveInDir(i);
						switch(i) {
						case 0:
							for(Cell c : possibilities) {
								toBeRemoved.add(c);
								toBeAdded.add(c.N);
							}
							break;
						case 1:
							for(Cell c : possibilities) {
								toBeRemoved.add(c);
								toBeAdded.add(c.W);
							}
							break;
						case 2:
							for(Cell c : possibilities) {
								toBeRemoved.add(c);
								toBeAdded.add(c.E);
							}
							break;
						case 3:
							for(Cell c : possibilities) {
								toBeRemoved.add(c);
								toBeAdded.add(c.S);
							}
							break;
						}
						break;
					}
				}

				possibilities.removeAll(toBeRemoved);
				possibilities.addAll(toBeAdded);
				
				toBeRemoved.clear();
				toBeAdded.clear();
			}
		}
	}
	
	private static byte cellToByte(Cell c) {
		byte b = 0;
		//{NONE, BLACK, BLUE, GREEN, YELLOW, RED, WHITE, BROWN}
		switch(c.color){
		case 1: b |= 112;
				break;
		case 2: b |= 16;
				break;
		case 3: b |= 32;
				break;
		}
		if(c == giantCell) b |= 64;
		
		if(c.N == null) b |= 8;
		if(c.W == null) b |= 4;
		if(c.E == null) b |= 2;
		if(c.S == null) b |= 1;
		
		return b;
	}
	
	private static void displayMap() {
		byte[] outputBuffer = new byte[7 * 7 + 1 + 1]; //7*7 bytes for maze, 1 byte for mode, 1 byte for location 
		outputBuffer[0] = 0;
		outputBuffer[1] = (byte) ((currentCol << 4) + currentRow);
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				Cell c = internalMap[i][j];
				byte b = 0;
				if(exploredStatus[i][j] == 3 || c == giantCell) {
					//Only mark explored cells
					b = cellToByte(c);
				}
				outputBuffer[i * 7 + j + 2] = b;
			}
		}
		try{
			dataOutputStream.write(outputBuffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void displayLocalize() {
		byte[] outputBuffer = new byte[7 * 7 + 1 + 1]; //7*7 bytes for maze, 1 byte for mode, 1 byte for location 
		outputBuffer[0] = 1;
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				Cell c = internalMap[i][j];
				byte b = 0;
				if(exploredStatus[i][j] == 3 || c == giantCell) {
					//Only mark explored cells
					b = cellToByte(c);
				}
				if(possibleLocations.contains(c)) b |= 128;
				outputBuffer[i * 7 + j + 2] = b;
			}
		}
		try{
			dataOutputStream.write(outputBuffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void task() {
	    Button.ESCAPE.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(Key k) {
			}

			@Override
			public void keyReleased(Key k) {
				graphicsLCD.clear();
				graphicsLCD.drawString("RESET", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
				graphicsLCD.drawString("Task", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
				graphicsLCD.refresh();
				Button.waitForAnyPress();
				task();
			}
	    });
	    
		gyroSensor.reset();
	    
		ballExists = true;
		
		//Add all possible locations
		for(int i = 0; i < 7; i++) {
			for(int j = 0; j < 7; j++) {
				if (exploredStatus[i][j] == 3) possibleLocations.add(internalMap[i][j]);
			}
		}
		
		localize(possibleLocations);
		
		Cell currentCell = (Cell) possibleLocations.iterator().next();
		displayMap();
		
		//Move to ball
		if(!hasBall) {
			moveTo(currentCell, ballCell);
			currentCell = ballCell;
			displayMap();
		}

		//If still no ball
		if(!hasBall) {
			displayString("No ball");
		}
		
		//Move to giant
		moveTo(currentCell, giantCell);
		currentCell = giantCell;
		displayMap();
		
		//Long beep
		Sound.playTone(1000, 1000);
		
		//Move to prince
		if (princeCell != null) {
			moveTo(currentCell, princeCell);
			currentCell = princeCell;
			displayMap();
		} else {
			//Find prince
			exploreCell(giantCell);
			Cell nextCell = nextUnexploredCell();
			while(nextCell != null && princeCell == null) {
				moveTo(currentCell, nextCell);
				currentCell = nextCell;
				exploreCell(currentCell);
				nextCell = nextUnexploredCell();
				displayMap();
			}

			moveTo(currentCell, princeCell);
		}
		
		//Play 3 sounds
		Sound.beep();
		Sound.beep();
		Sound.beep();
		
		control();
	}
	
	static DataOutputStream dataOutputStream;
	
	public static void control() {
		graphicsLCD.clear();
		graphicsLCD.drawString("TermProj", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
		graphicsLCD.drawString("Ready", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
		graphicsLCD.refresh();
		
		switch(Button.waitForAnyPress()) {
		case(Button.ID_UP): {
			graphicsLCD.clear();
			graphicsLCD.drawString("TermProj", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
			graphicsLCD.drawString("Mapping", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
			graphicsLCD.refresh();
			mapping();
			break;
		}
		case(Button.ID_DOWN): {
			graphicsLCD.clear();
			graphicsLCD.drawString("TermProj", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
			graphicsLCD.drawString("Task", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
			graphicsLCD.refresh();
			task();
			break;
		}
		}
	}
	
	public static void main(String[] args) throws Exception {
		setDefaultSpeed();
		
//		while(true) moveForward(1);
		
		ServerSocket serverSocket = new ServerSocket(1234);
		
		graphicsLCD.clear();
		graphicsLCD.drawString("TermProj", graphicsLCD.getWidth()/2, 0, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
		graphicsLCD.drawString("Waiting", graphicsLCD.getWidth()/2, 20, GraphicsLCD.VCENTER|GraphicsLCD.HCENTER);
		graphicsLCD.refresh();
		
		Socket client = serverSocket.accept();
		
		OutputStream outputStream = client.getOutputStream();
		
		dataOutputStream = new DataOutputStream(outputStream);
		
		control();
	}
}
