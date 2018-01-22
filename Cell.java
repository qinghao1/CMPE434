public class Cell {
	//{NONE, BLACK, BLUE, GREEN, YELLOW, RED, WHITE, BROWN}
	int color, row, col;
	Cell N, S, E, W;
	
	public Cell(int r, int c, Cell _N, Cell _S, Cell _E, Cell _W, int _color) {
		row = r;
		col = c;
		N = _N;
		S = _S;
		E = _E;
		W = _W;
		color = _color;
	}
}
