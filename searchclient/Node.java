package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import searchclient.Command.Type;

public class Node {
	private static final Random RND = new Random(2);

	public int maxRow = 0;
	public int maxCol = 0;

	public int agentRow;
	public int agentCol;

	// Arrays are indexed from the top-left of the level, with first index being row and second being column.
	// Row 0: (0,0) (0,1) (0,2) (0,3) ...
	// Row 1: (1,0) (1,1) (1,2) (1,3) ...
	// Row 2: (2,0) (2,1) (2,2) (2,3) ...
	// ...
	// (Start in the top left corner, first go down, then go right)
	// E.g. this.walls[2] is an array of booleans having size MAX_COL.
	// this.walls[row][col] is true if there's a wall at (row, col)
	//

	public boolean[][] walls;
	public char[][] boxes;
	public char[][] goals;

	public Node parent;
	public Command action;

	private int g;
	
	private int _hash = 0;

	public Node(Node parent) {
		this.parent = parent;
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
		if(parent != null) {
			setMaxCol(parent.getMaxCol());
			setMaxRow(parent.getMaxRow());
		}
		boxes = new char[getMaxRow()][getMaxCol()];
	}
	
	public int getMaxRow() {
		return this.maxRow;
	}
	
	public int getMaxCol() {
		return this.maxCol;
	}

	public void setMaxRow(int maxRow) {
		this.maxRow = maxRow;
		
	}
	
	public void setMaxCol(int maxCol) {
		this.maxCol = maxCol;
	}
	
	public void setupMap() {
		walls = new boolean[getMaxRow()][getMaxCol()];
		boxes = new char[getMaxRow()][getMaxCol()];
		goals = new char[getMaxRow()][getMaxCol()];
	}
	
	public int g() {
		return this.g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}

	public boolean isGoalState(Node initialState) {
		for (int row = 1; row < getMaxRow() - 1; row++) {
			for (int col = 1; col < getMaxCol() - 1; col++) {
				char g = initialState.goals[row][col];
				char b = Character.toLowerCase(boxes[row][col]);
				if (g > 0 && b != g) {
					return false;
				}
			}
		}
		return true;
	}

	public ArrayList<Node> getExpandedNodes(Node initialState) {
			ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
			for (Command c : Command.EVERY) {
				// Determine applicability of action
				int newAgentRow = this.agentRow + Command.dirToRowChange(c.dir1);
				int newAgentCol = this.agentCol + Command.dirToColChange(c.dir1);
	
				if (c.actionType == Type.Move) {
					// Check if there's a wall or box on the cell to which the agent is moving
					if (this.cellIsFree(newAgentRow, newAgentCol, initialState)) {
						Node n = this.ChildNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
						expandedNodes.add(n);
					}
				} else if (c.actionType == Type.Push) {
					// Make sure that there's actually a box to move
					if (this.boxAt(newAgentRow, newAgentCol)) {
						int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
						int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
						// .. and that new cell of box is free
						if (this.cellIsFree(newBoxRow, newBoxCol, initialState)) {
							Node n = this.ChildNode();
							n.action = c;
							n.agentRow = newAgentRow;
							n.agentCol = newAgentCol;
							n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
							n.boxes[newAgentRow][newAgentCol] = 0;
							expandedNodes.add(n);
						}
					}
				} else if (c.actionType == Type.Pull) {
					// Cell is free where agent is going
					if (this.cellIsFree(newAgentRow, newAgentCol, initialState)) {
						int boxRow = this.agentRow + Command.dirToRowChange(c.dir2);
						int boxCol = this.agentCol + Command.dirToColChange(c.dir2);
						// .. and there's a box in "dir2" of the agent
						if (this.boxAt(boxRow, boxCol)) {
							Node n = this.ChildNode();
							n.action = c;
							n.agentRow = newAgentRow;
							n.agentCol = newAgentCol;
							n.boxes[this.agentRow][this.agentCol] = this.boxes[boxRow][boxCol];
							n.boxes[boxRow][boxCol] = 0;
							expandedNodes.add(n);
						}
					}
				}
			}
			
			Collections.shuffle(expandedNodes, RND);
			return expandedNodes;
	}

	private boolean cellIsFree(int row, int col, Node initialState) {
//		if(this.parent != null) {
//			return this.parent.cellIsFree(row, col);
//		}else{
//			return !this.walls[row][col] && this.boxes[row][col] == 0;
//		}
//		
		return !initialState.walls[row][col] && this.boxes[row][col] == 0;
	}

	private boolean boxAt(int row, int col) {
//		if(this.parent != null) {
//			return this.parent.boxAt(row, col);
//		}else{
//			return this.boxes[row][col] > 0;
//		}
//		
		return this.boxes[row][col] > 0;
	}

	private Node ChildNode() {
		Node copy = new Node(this);
		copy.setMaxCol(this.getMaxCol());
		copy.setMaxRow(this.getMaxRow());
		for (int row = 0; row < getMaxRow(); row++) {
			System.arraycopy(this.boxes[row], 0, copy.boxes[row], 0, getMaxCol());
		}
		return copy;
	}

	public LinkedList<Node> extractPlan() {
		LinkedList<Node> plan = new LinkedList<Node>();
		Node n = this;
		while (!n.isInitialState()) {
			plan.addFirst(n);
			n = n.parent;
		}
		return plan;
	}

	@Override
	public int hashCode() {
		if (this._hash == 0) {
			final int prime = 31;
			int result = 1;
			result = prime * result + this.agentCol;
			result = prime * result + this.agentRow;
			result = prime * result + Arrays.deepHashCode(this.boxes);
			result = prime * result + Arrays.deepHashCode(this.goals);
			result = prime * result + Arrays.deepHashCode(this.walls);
			this._hash = result;
		}
		return this._hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (this.agentRow != other.agentRow || this.agentCol != other.agentCol)
			return false;
		if (!Arrays.deepEquals(this.boxes, other.boxes))
			return false;
		if (!Arrays.deepEquals(this.goals, other.goals))
			return false;
		if (!Arrays.deepEquals(this.walls, other.walls))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int row = 0; row < getMaxRow(); row++) {
			if (!this.walls[row][0]) {
				break;
			}
			for (int col = 0; col < getMaxCol(); col++) {
				if (this.boxes[row][col] > 0) {
					s.append(this.boxes[row][col]);
				} else if (this.goals[row][col] > 0) {
					s.append(this.goals[row][col]);
				} else if (this.walls[row][col]) {
					s.append("+");
				} else if (row == this.agentRow && col == this.agentCol) {
					s.append("0");
				} else {
					s.append(" ");
				}
			}
			s.append("\n");
		}
		return s.toString();
	}

}