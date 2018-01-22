Java lejOS code for CMPE434 final project in Bogazici University.
----
The robot is supposed to traverse a 4x4 maze with 33cm long grid cells.

First, it is supposed to explore and generate a map of the maze (visualized on connected PC). The red cell is a danger cell bounded by black cells, and should not be visited in this stage.

Then, it is placed in a random position in the maze (in the same orientation) and has to perform the following tasks in order:
1. If it has not picked up the ball (located in the blue cell), visit that cell.
2. Visit the red cell, then play a sound.
3. Visit the green cell, then play another sound.

## To Run

1. Connect lejOS robot to PC via bluetooth
2. Upload and run TermProj.java
3. Run PC.java on connected computer