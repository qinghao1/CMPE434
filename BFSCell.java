public class BFSCell {
	BFSCell previous;
	Cell cell;
	
	public BFSCell(Cell _cell, BFSCell prev) {
		cell = _cell;
		previous = prev;
	}
}
