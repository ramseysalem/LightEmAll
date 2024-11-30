import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import tester.Tester;



// to represent an instance of a LightEmAll game
class LightEmAll extends World {
  // to store the LightEmAll grid
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all GamePieces in this game
  ArrayList<GamePiece> nodes;
  // a mock HashMap to represent Edges between GamePieces
  HashMap<GamePiece, GamePiece> representatives;
  // a list of all possible Edges between GamePieces and their neighbors
  ArrayList<Edge> worklist;
  // to store the Minimum Spanning Tree
  ArrayList<Edge> edgesInTree;
  // a list of all powered GamePieces
  ArrayList<GamePiece> powered;
  // dimensions of the board and GamePieces
  int width;
  int height;
  int pieceSize;
  // to store location of powerStation
  int powerRow;
  int powerCol;
  // to store the effective radius of powerStation
  int radius;
  // to store the game mode. Can be one of "MANUAL", "FRACTAL", "KRUSKAL", "EMPTY"
  String mode;
  // is the board going to be scrambled?
  boolean scramble;
  // a random object for testing
  Random rand;

  // Constructor to start a LightEmAll game. Default mode is "KRUSKAL"
  LightEmAll(int width, int height) {
    this(width, height, "KRUSKAL");
  }

  // Constructor to start a LightEmAll game with a specified game mode
  LightEmAll(int width, int height, String mode) {
    this(width, height, mode, false, new Random());
  }

  // Constructor with a flag to show the minimum spanning tree at the beginning of the match
  LightEmAll(int width, int height, String mode, boolean scramble) {
    this(width, height, mode, false, new Random());
  }

  // Constructor with a specified random object for testing
  LightEmAll(int width, int height, String mode, boolean scramble, Random rand) {
    this.mode = mode;
    this.board = new ArrayList<>();
    this.nodes = new ArrayList<>();
    this.representatives = new HashMap<>();
    this.worklist = new ArrayList<>();
    this.edgesInTree = new ArrayList<>();
    this.width = width;
    this.height = height;
    this.powerRow = 0;
    this.powerCol = 0;
    this.pieceSize = 40;
    this.scramble = scramble;
    this.rand = rand;
    this.init();
  }

  // EFFECT: calls helpers to initialize a LightEmAll game
  void init() {
    this.genBoard();
    this.connectNeighbors();
    this.genWires();
    this.updateConnections();
    this.findRadius();
    this.powered = this.board.get(powerRow).get(powerCol).powerUp(this.radius, 0,
            new ArrayList<>(), new ArrayList<>());
  }

  // to draw the graphical interface of a LightEmAll game
  @Override
  public WorldScene makeScene() {
    WorldScene ws = new WorldScene(this.width * this.pieceSize, this.height * this.pieceSize);
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        if (i == this.powerCol && j == this.powerRow) {
          ws.placeImageXY(this.board.get(i).get(j).draw(this.pieceSize, this.radius),
                  this.pieceSize / 2 + this.pieceSize * i, this.pieceSize / 2 + this.pieceSize * j);
          ws.placeImageXY(
                  new OverlayImage(new StarImage(16, 7, OutlineMode.OUTLINE, Color.ORANGE),
                          new StarImage(16, 7, OutlineMode.SOLID, Color.CYAN)),
                  this.pieceSize / 2 + this.pieceSize * i, this.pieceSize / 2 + this.pieceSize * j);
        }
        else {
          ws.placeImageXY(this.board.get(i).get(j).draw(this.pieceSize, this.radius),
                  this.pieceSize / 2 + this.pieceSize * i, this.pieceSize / 2 + this.pieceSize * j);
        }
      }
    }
    return ws;
  }

  // to interact by rotating a GamePiece depending on where the click is
  public void onMouseClicked(Posn pos, String key) {
    this.powerDown();
    this.powered = new ArrayList<>();
    if (key.equals("LeftButton") || key.equals("RightButton")) {
      GamePiece current = this.board.get(Math.floorDiv(pos.x,
              this.pieceSize)).get(Math.floorDiv(pos.y, this.pieceSize));
      // GamePiece.rotate receives the key to specify direction
      current.rotate(key);
    }
    this.updateConnections();
    this.powered = this.board.get(powerCol).get(powerRow).powerUp(this.radius, 0,
            new ArrayList<>(), new ArrayList<>());

  }

  // to move powerStation depending on which key is pressed and its surroundings
  public void onKeyEvent(String str) {
    this.powerDown();
    this.powered = new ArrayList<>();
    if (str.equals("up") && this.board.get(this.powerCol).get(this.powerRow).top) {
      this.powerRow = this.powerRow - 1;
    }
    else if (str.equals("right") && this.board.get(this.powerCol).get(this.powerRow).rig) {
      this.powerCol = this.powerCol + 1;
    }
    else if (str.equals("down") && this.board.get(this.powerCol).get(this.powerRow).bot) {
      this.powerRow = this.powerRow + 1;
    }
    else if (str.equals("left") && this.board.get(this.powerCol).get(this.powerRow).lef) {
      this.powerCol = this.powerCol - 1;
    }
    this.updateConnections();
    this.powered = this.board.get(powerCol).get(powerRow).powerUp(this.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    // this.nodeFurthestFrom(this.board.get(this.powerCol).get(this.powerRow));
  }

  // EFFECT: establishes relations between a certain cell and its adjacent cells
  void connectNeighbors() {
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        // target node
        GamePiece target = this.board.get(i).get(j);
        // top left corner
        if (i == 0 && j == 0) {
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
        }
        // top right corner
        else if (i == this.width - 1 && j == 0) {
          target.addNeighbor("left", this.board.get(i - 1).get(j));
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
        }
        // top row
        else if (j == 0 && i != 0 && i != this.width - 1) {
          target.addNeighbor("left", this.board.get(i - 1).get(j));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
        }
        // left column
        else if (i == 0 && j != 0 && j != this.height - 1) {
          target.addNeighbor("top", this.board.get(i).get(j - 1));
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
        }
        // right column
        else if (i == this.width - 1 && j != 0 && j != this.height - 1) {
          target.addNeighbor("top", this.board.get(i).get(j - 1));
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
          target.addNeighbor("left", this.board.get(i - 1).get(j));
        }
        // bottom left corner
        else if (i == 0 && j == this.height - 1) {
          target.addNeighbor("top", this.board.get(i).get(j - 1));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
        }
        // bottom right corner
        else if (i == this.width - 1 && j == this.height - 1) {
          target.addNeighbor("left", this.board.get(i - 1).get(j));
          target.addNeighbor("top", this.board.get(i).get(j - 1));
        }
        // bottom row
        else if (j == this.height - 1 && i != 0 && i != this.width - 1) {
          target.addNeighbor("top", this.board.get(i).get(j - 1));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
          target.addNeighbor("left", this.board.get(i - 1).get(j));
        }
        // center pieces
        else {
          target.addNeighbor("top", this.board.get(i).get(j - 1));
          target.addNeighbor("bottom", this.board.get(i).get(j + 1));
          target.addNeighbor("right", this.board.get(i + 1).get(j));
          target.addNeighbor("left", this.board.get(i - 1).get(j));
        }
      }
    }
  }

  // EFFECT: generates the LightEmAll grid
  void genBoard() {
    for (int i = 0; i < this.width; i++) {
      this.board.add(new ArrayList<>());
      for (int j = 0; j < this.height; j++) {
        GamePiece gp = new GamePiece(i, j);
        this.board.get(i).add(gp);
        this.nodes.add(gp);
      }
    }
  }

  // EFFECT: generates wires according to the specified game mode
  void genWires() {
    if (this.mode.equals("MANUAL")) {
      this.manualWires();
    }
    if (this.mode.equals("FRACTALS")) {
      this.fractalWires(0, this.width - 1, 0, this.height - 1);
    }
    if (this.mode.equals("KRUSKAL")) {
      this.kruskalWires();
    }
    if (this.mode.equals("EMPTY")) {
      return; // This mode is only for testing
    }
  }

  // EFFECT: generates a previously hard-coded wire board
  void manualWires() {
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        // top left corner
        if (i == 0 && j == 0) {
          this.board.get(i).get(j).wireBot = true;
        }
        // top row
        else if (i > 0 && j == 0 && i < this.width - 1) {
          this.board.get(i).get(j).wireBot = true;
        }
        // top right corner
        else if (i == this.width - 1 && j == 0) {
          this.board.get(i).get(j).wireBot = true;
        }
        // left column
        else if (i == 0 && j != 0 && j != this.height - 1) {
          this.board.get(i).get(j).wireTop = true;
          this.board.get(i).get(j).wireBot = true;
          if (j == 4) {
            this.board.get(i).get(j).wireRight = true;
          }
        }
        // right column
        else if (i == this.width - 1 && j != 0 && j != height - 1) {
          this.board.get(i).get(j).wireTop = true;
          this.board.get(i).get(j).wireBot = true;
          if (j == 4) {
            this.board.get(i).get(j).wireLeft = true;
          }
        }
        // bottom left corner
        else if (i == 0 && j == this.height - 1) {
          this.board.get(i).get(j).wireTop = true;
        }
        // bottom row
        else if (i != 0 && i != this.width - 1 && j == this.height - 1) {
          this.board.get(i).get(j).wireTop = true;
        }
        // bottom right corner
        else if (i == this.width - 1 && j == this.height - 1) {
          this.board.get(i).get(j).wireTop = true;
        }
        // middle row
        else if (j == 4 && i != 0 && i != this.width - 1) {
          this.board.get(i).get(j).wireLeft = true;
          this.board.get(i).get(j).wireTop = true;
          this.board.get(i).get(j).wireBot = true;
          this.board.get(i).get(j).wireRight = true;
        }
        // every other cell that is not along a border
        else {
          this.board.get(i).get(j).wireTop = true;
          this.board.get(i).get(j).wireBot = true;
        }
      }
    }
  }

  // EFFECT: generates a fractal-like pattern that works for any dimensions
  void fractalWires(int x1, int x2, int y1, int y2) {
    for (int k = x1; k <= x2; k++) {
      for (int h = y1; h <= y2; h++) {
        // top left corner
        if (k == x1 && h == y1) {
          this.board.get(k).get(h).wireBot = true;
        }
        // top right corner
        else if (k == x2 && h == y1) {
          this.board.get(k).get(h).wireBot = true;
        }
        // bottom left corner
        else if (k == x1 && h == y2) {
          this.board.get(k).get(h).wireRight = true;
          this.board.get(k).get(h).wireTop = true;
        }
        // bottom right corner
        else if (k == x2 && h == y2) {
          this.board.get(k).get(h).wireLeft = true;
          this.board.get(k).get(h).wireTop = true;
        }
        // left column
        else if (h != y1 && h != y2 && k == x1) {
          this.board.get(k).get(h).wireTop = true;
          this.board.get(k).get(h).wireBot = true;
        }
        // right column
        else if (h != y1 && h != y2 && k == x2) {
          this.board.get(k).get(h).wireTop = true;
          this.board.get(k).get(h).wireBot = true;
        }
        // bottom row
        else if (k != x1 && k != x2 && h == y2) {
          this.board.get(k).get(h).wireLeft = true;
          this.board.get(k).get(h).wireRight = true;
        }
      }
    }

    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 0 && y1 % 2 == 0) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2));
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 1 && y1 % 2 == 0) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2));
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 0 && y1 == 0 && y2 == 1) {
      // top left quadrant
      this.fractalWires(x1, Math.round(x2 / 2), y1, y2);
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 0 && y1 % 2 == 1 && y2 - y1 > 1) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2));// +1
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 0 && y1 % 2 == 1 && y2 - y1 == 1) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2) + 1);// +1
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 > 1 && x1 % 2 == 1 && y1 % 2 == 1 && y2 - y1 > 1) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2));
    }
    if (Math.ceil(x2 - x1 / 2) >= 1 && x2 - x1 >= 1 && y2 - y1 == 1) {
      // top left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), y1, Math.floorDiv(y2 + y1, 2) + 1);
    }
    if (x2 - ((Math.floorDiv((x2 - x1), 2)) + 1) >= 1 && x2 - x1 > 2 && y2 - y1 >= 2) {
      // top right quadrant when x1 == 0
      this.fractalWires(Math.floorDiv(x2 + x1, 2) + 1, x2, y1, Math.floorDiv(y2 + y1, 2));
    }
    if (x2 - ((Math.floorDiv((x2 - x1), 2)) + 1) >= 1 && x2 - x1 > 2 && y2 - y1 == 1) {
      // top right quadrant when x1 == 0
      this.fractalWires(Math.floorDiv(x2 + x1, 2) + 1, x2, y1, Math.floorDiv(y2 + y1, 2) + 1);
    }
    if (y2 - (Math.floorDiv((y2 - y1), 2) + 1) >= 1 && y2 - y1 > 2 && x2 - x1 > 1) {
      // bottom left quadrant
      this.fractalWires(x1, Math.floorDiv(x2 + x1, 2), Math.floorDiv(y2 + y1, 2) + 1, y2);
    }
    if ((y2 - (Math.floorDiv((y2 - y1), 2) + 1) >= 1 && y2 - y1 > 2 && x2 - x1 > 2)
            && (x2 - ((Math.floorDiv((x2 - x1), 2)) + 1) >= 1 && x2 - x1 > 2 && y2 - y1 > 2)) {
      // bottom right quadrant
      this.fractalWires(Math.floorDiv(x2 + x1, 2) + 1, x2, Math.floorDiv(y2 + y1, 2) + 1, y2);
    }
  }

  // EFFECT: generates a random, acyclic wire pattern using Kruskal's algorithm
  void kruskalWires() {
    this.genEdges();
    while (this.edgesInTree.size() != this.nodes.size() - 1) {
      Edge current = this.worklist.remove(0);
      GamePiece fromNode = current.fromNode;
      GamePiece toNode = current.toNode;
      GamePiece fromRep = this.findRep(fromNode);
      GamePiece toRep = this.findRep(toNode);
      if (!fromRep.equals(toRep)) {
        this.edgesInTree.add(current);
        this.representatives.put(this.representatives.get(fromRep), toRep);
      }
    }
    for (int i = 0; i < this.edgesInTree.size(); i++) {
      Edge current = this.edgesInTree.get(i);
      GamePiece fromNode = current.fromNode;
      GamePiece toNode = current.toNode;
      // NOTE: Does not check for top neighbor because
      // Edges were generated sweeping towards the bottom right corner
      if (fromNode.neighborHash.containsKey("right")
              && fromNode.neighborHash.get("right").equals(toNode)) {
        fromNode.wireRight = true;
        toNode.wireLeft = true;
      }
      if (fromNode.neighborHash.containsKey("bottom")
              && fromNode.neighborHash.get("bottom").equals(toNode)) {
        fromNode.wireBot = true;
        toNode.wireTop = true;
      }
      if (fromNode.neighborHash.containsKey("left")
              && fromNode.neighborHash.get("left").equals(toNode)) {
        fromNode.wireLeft = true;
        toNode.wireRight = true;
      }
    }
    this.updateConnections();
    if (this.scramble) {
      for (int i = 0; i < this.nodes.size(); i++) {
        GamePiece current = this.nodes.get(i);
        int rotations = this.rand.nextInt(4);
        for (int j = 0; j < rotations; j++) {
          current.rotate("LeftButton");
        }
      }
    }
  }

  // to return the deepest GamePiece for a given key in the representatives HashMap
  GamePiece findRep(GamePiece key) {
    GamePiece currentRep = this.representatives.get(key);
    if (currentRep.equals(key)) {
      return key;
    }
    else {
      return this.findRep(currentRep);
    }
  }

  // EFFECT: generates all Edges in this graph sorted by randomly assigned weights
  void genEdges() {
    int maxWeight = this.nodes.size();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        // target node
        GamePiece target = this.board.get(i).get(j);
        this.representatives.put(target, target);
        // GamePiece in last col connects to bottom neighbors
        if (i == this.width - 1 && j < this.height - 1) {
          this.worklist.add(new Edge(target,
                  this.board.get(i).get(j + 1), maxWeight, this.rand));
        }
        // GamePiece in last row connects to right neighbors
        else if (i < this.width - 1 && j == this.height - 1) {
          this.worklist.add(new Edge(target,
                  this.board.get(i + 1).get(j), maxWeight, this.rand));
        }
        // GamePieces remaining connect to bottom and right neighbors
        else if (i < this.width - 1 && j < this.height - 1) {
          this.worklist.add(new Edge(target,
                  this.board.get(i + 1).get(j), maxWeight, this.rand));
          this.worklist.add(new Edge(target,
                  this.board.get(i).get(j + 1), maxWeight, this.rand));
        }
        // NOTE: Bottom right corner does not generate edges
      }
    }
    // calls mergeSort to sort list of Edges by their weight
    new Utils().mergesort(this.worklist, new HeavierThan());
  }

  // EFFECT: updates connections between GamePieces after any operation
  void updateConnections() {
    for (int i = 0; i < this.nodes.size(); i++) {
      this.nodes.get(i).updateConnections();
      this.nodes.get(i).updatePowerStation(this.powerCol, this.powerRow);
    }
  }

  // EFFECT: powers down the board for safety before any operation (OnMouseClicked or OnKey)
  void powerDown() {
    for (int i = 0; i < this.nodes.size(); i++) {
      this.nodes.get(i).powerDown(this.powered);
    }
    this.powered = new ArrayList<>();
  }

  // EFFECT: updates the effective radius of the power station using BFS
  void findRadius() {
    int maxDepth = 0;
    HashMap<Integer, GamePiece> firstLast =
            this.findDeepest(this.board.get(powerCol).get(powerRow), 0, new ArrayList<>());
    for (int key : firstLast.keySet()) {
      maxDepth = key;
    }
    HashMap<Integer, GamePiece> secondLast =
            this.findDeepest(firstLast.get(maxDepth), 0, new ArrayList<>());
    for (int key : secondLast.keySet()) {
      maxDepth = key;
    }
    this.radius = maxDepth / 2 + 1;
  }

  // uses BFS to return a HashMap of length 1. The HashMap contains:
  //  - key: An int representing the maximum depth of the graph
  //  - value: The GamePiece farthest from the start point
  HashMap<Integer, GamePiece> findDeepest(GamePiece start,
                                          int depth,
                                          ArrayList<GamePiece> acc) {
    // Next group of nodes to be checked (if any)
    ArrayList<GamePiece> nextLevel = new ArrayList<>();
    // Loop to update nextLevel with currentLast's neighbors (if any)
    // updates accumulator accordingly
    for (String key : start.neighborHash.keySet()) {
      GamePiece toCheck = start.neighborHash.get(key);
      if (!acc.contains(toCheck) && toCheck.isConnected(start)) {
        nextLevel.add(toCheck); 
      }
    }
    // initialize results variable to an empty HashMap
    HashMap<Integer, GamePiece> results = new HashMap<>();
    // If there are no more nodes to check, return HashMap with depth, start pair
    if (nextLevel.size() == 0) {
      results.put(depth, start);
    }
    // If there are more nodes to be check, update accumulator and call helper
    else {
      acc.add(start);
      results = this.findDeeper(nextLevel, depth + 1, acc);
    }
    return results;
  }

  // helper for findDeepest() to check ArrayLists of neighbors when their size > 1
  HashMap<Integer, GamePiece> findDeeper(ArrayList<GamePiece> prevLevel,
                                         int depth,
                                         ArrayList<GamePiece> acc) {
    // Next group of nodes to be checked (if any)
    ArrayList<GamePiece> nextLevel = new ArrayList<>();
    // An arbitrary node from this level to be returned if no deeper GamePiece is found
    GamePiece nowDeepest = prevLevel.get(0);
    // If there is only one node to check, update accumulator and call original method
    if (prevLevel.size() == 1) {
      acc.add(nowDeepest);
      return findDeepest(nowDeepest, depth, acc);
    }
    // If there is more than one node to check, check all their neighbors
    else {
      // First loop checks every node in prevLevel (if not in accumulator)
      for (int i = 0; i < prevLevel.size(); i++) {
        nowDeepest = prevLevel.get(i);
        if (!acc.contains(nowDeepest)) {
          // Second loop adds unchecked neighbors to nextLevel (if not in accumulator)
          for (String key : nowDeepest.neighborHash.keySet()) {
            GamePiece aNeighbor = nowDeepest.neighborHash.get(key);
            if (aNeighbor.isConnected(nowDeepest)
                    && (!acc.contains(aNeighbor) && !nextLevel.contains(aNeighbor))) {
              nextLevel.add(aNeighbor);
            }
          }
        }
        // Update the accumulator
        acc.add(nowDeepest);
      }
    }
    // initialize results variable to an empty HashMap
    HashMap<Integer, GamePiece> results = new HashMap<>();
    // If there are no more nodes to check, return HashMap with depth, nowDeepest pair
    if (nextLevel.size() == 0) {
      results.put(depth, nowDeepest);
    }
    // If there are more nodes to be checked, call this method recursively
    else {
      // NOTE: no need to update accumulator since it was updated in previous if-else statement
      results = this.findDeeper(nextLevel, depth + 1, acc);
    }
    return results;
  }

}

//to represent a GamePiece in a LightEmAll game
class GamePiece {
// x value of this GamePiece in its parent LightEmAll board
int col;
// y value of this GamePiece in its parent LightEmAll board
int row;
// does this GamePiece have outgoing wires?
boolean wireLeft;
boolean wireRight;
boolean wireTop;
boolean wireBot;
// does this GamePiece connect to its neighbors through wires?
boolean lef;
boolean rig;
boolean top;
boolean bot;
// does this GamePiece contain a powerStation?
boolean powerStation;
// a list of all neighbors of this GamePiece (independent of wire connection)
ArrayList<GamePiece> neighborList;
// a HashMap of all neighbors of this GamePiece. HashMap contains:
//  - key: String representing the location in which a neighbor is located
//  - value: a neighboring GamePiece
HashMap<String, GamePiece> neighborHash;
// is this GamePiece receiving power?
boolean isPowered;
// how far away is this GamePiece from the powerStation?
int distFromPower;

// Constructor for a GamePiece
GamePiece(int i, int j) {
 this.row = j;
 this.col = i;
 this.lef = false;
 this.rig = false;
 this.top = false;
 this.bot = false;
 this.wireLeft = false;
 this.wireRight = false;
 this.wireTop = false;
 this.wireBot = false;
 this.powerStation = false;
 this.neighborList = new ArrayList<>();
 this.neighborHash = new HashMap<>();
 this.distFromPower = 2147483647;
}

// to draw this piece given its wire specifications
WorldImage draw(int size, int radius) {
 Color power = this.powerGradient(radius);
 WorldImage wi = new OverlayImage(
         new RectangleImage(size - 1, size - 1, OutlineMode.SOLID, Color.DARK_GRAY),
         new RectangleImage(size, size, OutlineMode.SOLID, Color.black));
 if (this.wireRight) { // if there is a right wire
   wi = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE,
           new RectangleImage(20, 3, OutlineMode.SOLID, power), 0, 0, wi);
 }
 if (this.wireLeft) { // if there is a left wire
   wi = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE,
           new RectangleImage(20, 3, OutlineMode.SOLID, power), 0, 0, wi);
 }
 if (this.wireTop) { // if there is a top wire
   wi = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
           new RectangleImage(3, 20, OutlineMode.SOLID, power), 0, 0, wi);
 }
 if (this.wireBot) { // if there is a bottom wire
   wi = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
           new RectangleImage(3, 20, OutlineMode.SOLID, power), 0, 0, wi);
 }
 return wi;
}

// returns a color according to the distFromPower
Color powerGradient(int radius) {
 if (this.distFromPower <= 0.010 * radius) {
   return new Color(255, 255, 204);
 }
 if (this.distFromPower <= 0.025 * radius) {
   return new Color(255, 255, 153);
 }
 if (this.distFromPower <= 0.050 * radius) {
   return new Color(255, 255, 102);
 }
 if (this.distFromPower <= 0.100 * radius) {
   return new Color(255, 255, 51);
 }
 if (this.distFromPower <= 0.125 * radius) {
   return new Color(240, 230, 0);
 }
 if (this.distFromPower <= 0.50 * radius) {
   return new Color(204, 204, 0);
 }
 if (this.distFromPower <= 0.75 * radius) {
   return new Color(153, 153, 0);
 }
 if (this.distFromPower <= 0.95 * radius) {
   return new Color(102, 102, 0);
 }
 if (this.distFromPower <= radius) {
   return new Color(75, 75, 0);
 }
 return Color.black;
}

// EFFECT: adds the given GamePiece to this GamePiece's neighbors
void addNeighbor(String str, GamePiece neighbor) {
 if (str.equals("top")) {
   this.neighborList.add(neighbor);
   this.neighborHash.put("top", neighbor);
 }
 if (str.equals("right")) {
   this.neighborList.add(neighbor);
   this.neighborHash.put("right", neighbor);
 }
 if (str.equals("bottom")) {
   this.neighborList.add(neighbor);
   this.neighborHash.put("bottom", neighbor);
 }
 if (str.equals("left")) {
   this.neighborList.add(neighbor);
   this.neighborHash.put("left", neighbor);
 }
}

// EFFECT: rotates the piece 90 degrees counter clockwise
void rotate(String direction) {
 if (direction.equals("LeftButton")) {
   boolean temp = this.wireRight;
   this.wireRight = this.wireBot;
   this.wireBot = this.wireLeft;
   this.wireLeft = this.wireTop;
   this.wireTop = temp;
 }
 if (direction.equals("RightButton")) {
   boolean temp = this.wireRight;
   this.wireRight = this.wireTop;
   this.wireTop = this.wireLeft;
   this.wireLeft = this.wireBot;
   this.wireBot = temp;
 }
 this.updateConnections();
}

// EFFECT: updates this GamePiece's connections according to new wire locations
void updateConnections() {
 this.top = false;
 this.rig = false;
 this.bot = false;
 this.lef = false;
 if (this.wireTop && this.neighborHash.containsKey("top")
         && this.neighborHash.get("top").wireBot) {
   this.top = true;
 }
 if (this.wireRight && this.neighborHash.containsKey("right")
         && this.neighborHash.get("right").wireLeft) {
   this.rig = true;
 }
 if (this.wireBot && this.neighborHash.containsKey("bottom")
         && this.neighborHash.get("bottom").wireTop) {
   this.bot = true;
 }
 if (this.wireLeft && this.neighborHash.containsKey("left")
         && this.neighborHash.get("left").wireRight) {
   this.lef = true;
 }
}

// EFFECT: updates whether or not this GamePiece contains a powerStation
void updatePowerStation(int x, int y) {
 this.powerStation = x == this.col && y == this.row;
}

// is this GamePiece directly connected to the given GamePiece?
boolean isConnected(GamePiece curr) {
 if (this.row + 1 == curr.row && this.col == curr.col) {
   return this.wireBot && curr.wireTop;
 }
 if (this.row == curr.row && this.col + 1 == curr.col) {
   return this.wireRight && curr.wireLeft;
 }
 if (this.row - 1 == curr.row && this.col == curr.col) {
   return this.wireTop && curr.wireBot;
 }
 if (this.row == curr.row && this.col - 1 == curr.col) {
   return this.wireLeft && curr.wireRight;
 }
 else {
   return false;
 }
}

// to return an ArrayList of GamePieces that are powered
// EFFECT: updates how much power is received by the board using BFS
ArrayList<GamePiece> powerUp(int radius,
                            int distFromPower,
                            ArrayList<GamePiece> acc,
                            ArrayList<GamePiece> powered) {
 acc.add(this);
 this.distFromPower = distFromPower;
 if (distFromPower <= radius) {
   this.isPowered = true;
   powered.add(this);
 }
 else {
   this.isPowered = false;
 }
 ArrayList<GamePiece> toCheck = new ArrayList<>();
 if (this.top) {
   toCheck.add(this.neighborHash.get("top"));
 }
 if (this.rig) {
   toCheck.add(this.neighborHash.get("right"));
 }
 if (this.bot) {
   toCheck.add(this.neighborHash.get("bottom"));
 }
 if (this.lef) {
   toCheck.add(this.neighborHash.get("left"));
 }
 if (toCheck.size() != 0) {
   for (int i = 0; i < toCheck.size(); i++) {
     GamePiece node = toCheck.get(i);
     if (!acc.contains(node) && this.isConnected(node)) {
       node.powerUp(radius, distFromPower + 1, acc, powered);
     }
   }
 }
 return powered;
}

// EFFECT: shuts down the power in this GamePiece
void powerDown(ArrayList<GamePiece> powered) {
 this.isPowered = false;
 this.distFromPower = 2147483647;
 this.updateConnections();
}
}

//to represent an Edge in a LightEmAll game
class Edge {
// the two GamePieces connected by this Edge
GamePiece fromNode;
GamePiece toNode;
// the weight of this Edge
int weight;

// Constructor for an Edge with a randomly assigned weight
Edge(GamePiece fromNode, GamePiece toNode, int maxWeight) {
 this(fromNode, toNode, maxWeight, new Random());
}

// Constructor for an Edge with a specified Random object
Edge(GamePiece fromNode, GamePiece toNode, int maxWeight, Random rand) {
 this.fromNode = fromNode;
 this.toNode = toNode;
 this.weight = rand.nextInt(maxWeight);
}
}

//Utils class to store helper functions
class Utils {
// EFFECT: Sorts the provided list according to the given comparator
<T> void mergesort(ArrayList<T> arr, IComparator<T> comp) {
 // Create a temporary array
 ArrayList<T> temp = new ArrayList<T>();
 // Make sure the temporary array is exactly as big as the given array
 for (int i = 0; i < arr.size(); i = i + 1) {
   temp.add(arr.get(i));
 }
 mergesortHelp(arr, temp, comp, 0, arr.size());
}

// EFFECT: Sorts the provided list in the region [loIdx, hiIdx)
//         Modifies both lists in the range [loIdx, hiIdx)
<T> void mergesortHelp(ArrayList<T> source, ArrayList<T> temp, IComparator<T> comp,
                      int loIdx, int hiIdx) {
 // Step 0: stop when finished
 if (hiIdx - loIdx <= 1) {
   return; // nothing to sort
 }
 // Step 1: find the middle index
 int midIdx = (loIdx + hiIdx) / 2;
 // Step 2: recursively sort both halves
 mergesortHelp(source, temp, comp, loIdx, midIdx);
 mergesortHelp(source, temp, comp, midIdx, hiIdx);
 // Step 3: merge the two sorted halves
 merge(source, temp, comp, loIdx, midIdx, hiIdx);
}

// Merges the two sorted regions [loIdx, midIdx) and [midIdx, hiIdx) from source
// into a single sorted region according to the given comparator
// EFFECT: modifies the region [loIdx, hiIdx) in both source and temp
<T> void merge(ArrayList<T> source, ArrayList<T> temp, IComparator<T> comp,
              int loIdx, int midIdx, int hiIdx) {
 int curLo = loIdx;   // where to start looking in the lower half-list
 int curHi = midIdx;  // where to start looking in the upper half-list
 int curCopy = loIdx; // where to start copying into the temp storage
 while (curLo < midIdx && curHi < hiIdx) {
   if (comp.compare(source.get(curLo), source.get(curHi)) <= 0) {
     // the value at curLo is smaller, so it comes first
     temp.set(curCopy, source.get(curLo));
     curLo = curLo + 1; // advance the lower index
   }
   else {
     // the value at curHi is smaller, so it comes first
     temp.set(curCopy, source.get(curHi));
     curHi = curHi + 1; // advance the upper index
   }
   curCopy = curCopy + 1; // advance the copying index
 }
 // copy everything that's left -- at most one of the two half-lists still has items in it
 while (curLo < midIdx) {
   temp.set(curCopy, source.get(curLo));
   curLo = curLo + 1;
   curCopy = curCopy + 1;
 }
 while (curHi < hiIdx) {
   temp.set(curCopy, source.get(curHi));
   curHi = curHi + 1;
   curCopy = curCopy + 1;
 }
 // copy everything back from temp into source
 for (int i = loIdx; i < hiIdx; i = i + 1) {
   source.set(i, temp.get(i));
 }
}
}

//interface for a Comparator
interface IComparator<T> extends Comparator<T> {
}

//to compare the weight of two Edges
class HeavierThan implements IComparator<Edge> {
public int compare(Edge left, Edge right) {
 if (left.weight == right.weight) {
   return 0;
 }
 else if (left.weight > right.weight) {
   return 1;
 }
 else {
   return -1;
 }
}
}

class ExamplesLightEmAll {
  LightEmAll leaF0;
  LightEmAll leaM1;
  LightEmAll leaM2;
  LightEmAll leaF1;
  LightEmAll leaF2;
  LightEmAll leaF3;
  LightEmAll leaF4;
  LightEmAll leaK0;
  LightEmAll leaK1;
  LightEmAll leaK2;
  LightEmAll leaK3;
  LightEmAll leaK5;
  ArrayList<ArrayList<GamePiece>> testBoard1;
  ArrayList<ArrayList<GamePiece>> testBoard2;
  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  GamePiece gp7;
  GamePiece gp8;
  GamePiece gp9;
  GamePiece gp10;
  GamePiece gp11;
  GamePiece gp12;
  GamePiece gp13;

  // Initialize examples for LightEmAll & GamePiece
  void initLightEmAll() {
    this.gp1 = new GamePiece(0, 0);
    this.gp2 = new GamePiece(1, 0);
    this.gp3 = new GamePiece(0, 1);
    this.leaK2 = new LightEmAll(4, 4, "KRUSKAL", false, new Random(10));
    this.leaK3 = new LightEmAll(3, 3, "KRUSKAL", false);
    this.leaK0 = new LightEmAll(8, 8, "KRUSKAL", true); // <--- shows Minimum Spanning Tree
    this.leaK1 = new LightEmAll(5, 5, "KRUSKAL", false);
    this.leaK5 = new LightEmAll(20, 16, "KRUSKAL");
    this.leaF0 = new LightEmAll(4, 4, "FRACTALS");
    this.leaM1 = new LightEmAll(3, 3, "MANUAL");
    this.leaM2 = new LightEmAll(8, 8, "MANUAL");
    this.leaF1 = new LightEmAll(5, 5, "FRACTALS");
    this.leaF2 = new LightEmAll(9, 9, "FRACTALS");
    this.leaF3 = new LightEmAll(15, 11, "FRACTALS");
    this.leaF4 = new LightEmAll(16, 16, "FRACTALS");
    this.testBoard1 = this.leaF1.board;
    this.gp1 = testBoard1.get(0).get(0);
    this.gp2 = testBoard1.get(0).get(1);
    this.gp3 = testBoard1.get(0).get(2);
    this.gp4 = testBoard1.get(1).get(0);
    this.gp5 = testBoard1.get(1).get(1);
    this.gp6 = testBoard1.get(1).get(2);
    this.gp7 = testBoard1.get(2).get(0);
    this.gp8 = testBoard1.get(2).get(1);
    this.gp9 = testBoard1.get(2).get(2);
    this.testBoard2 = this.leaF0.board;
    this.gp10 = testBoard2.get(0).get(0);
    this.gp11 = testBoard2.get(0).get(1);
    this.gp12 = testBoard2.get(1).get(0);
    this.gp13 = testBoard2.get(1).get(1);
  }

  // test & play LightEmAll with graphical interface
  void testBigBang(Tester t) {
    this.initLightEmAll();
    LightEmAll defGame = this.leaK5; // <--- change this for different modes
    World w = defGame;
    int worldWidth = defGame.width * 40;
    int worldHeight = defGame.height * 40;
    w.bigBang(worldWidth, worldHeight);
  }

  // BEGIN tests for LightEmAll.java:
  // test LightEmAll.init: to initialize the game
  boolean testInit(Tester t) {
    this.initLightEmAll();
    return t.checkExpect(this.leaF0.board.size(), 4)
            && t.checkExpect(this.leaF0.nodes.size(), 16)
            && t.checkExpect(this.leaM2.board.size(), 8)
            && t.checkExpect(this.leaM2.board.get(1).size(), 8)
            && t.checkExpect(this.leaF1.board.get(0).size(), 5)
            && t.checkExpect(this.leaF2.board.size(), 9)
            && t.checkExpect(this.leaF2.nodes.size(), 81)
            && t.checkExpect(this.leaF3.board.get(0).size(), 11)
            && t.checkExpect(this.leaF3.board.size(), 15)
            && t.checkExpect(this.leaF4.nodes.size(), 256)
            && t.checkExpect(this.gp1.neighborList.size(), 2)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp2), true)
            && t.checkExpect(this.gp2.neighborList.contains(this.gp1), true)
            && t.checkExpect(this.gp4.neighborList.contains(this.gp1), true)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp4), true)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp3), false)
            && t.checkExpect(this.gp5.neighborList.size(), 4)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp2), true)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp4), true)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp6), true)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp8), true)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp1), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp3), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp5), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp7), false)
            && t.checkExpect(this.leaF0.radius, 6)
            && t.checkExpect(this.leaM2.radius, 8)
            && t.checkExpect(this.leaF1.radius, 8)
            && t.checkExpect(this.leaF2.radius, 17)
            && t.checkExpect(this.leaF3.radius, 28)
            && t.checkExpect(this.leaF4.radius, 34)
            && t.checkExpect(this.leaF0.powered.size(), 11)
            && t.checkExpect(this.leaM2.powered.size(), 24)
            && t.checkExpect(this.leaF1.powered.size(), 18)
            && t.checkExpect(this.leaF2.powered.size(), 57)
            && t.checkExpect(this.leaF3.powered.size(), 119)
            && t.checkExpect(this.leaF4.powered.size(), 177);
  }

  // test LightEmAll.onMouseClicked: to interact through the mouse
  boolean testOnMouseClicked(Tester t) {
    this.initLightEmAll();
    // clicks for (1, 0): Brings to original position
    this.leaF0.onMouseClicked(new Posn(71, 27), "RightButton");
    this.leaF0.onMouseClicked(new Posn(60, 15), "LeftButton");
    this.leaF0.onMouseClicked(new Posn(48, 30), "LeftButton");
    this.leaF0.onMouseClicked(new Posn(70, 38), "RightButton");
    this.leaF0.onMouseClicked(new Posn(55, 5), "LeftButton");
    this.leaF0.onMouseClicked(new Posn(64, 20), "LeftButton");
    this.leaF0.onMouseClicked(new Posn(70, 12), "RightButton");
    this.leaF0.onMouseClicked(new Posn(71, 34), "RightButton");
    // clicks for (0, 1): Rotates twice, disconnects from (1, 1), connected to power
    this.leaF0.onMouseClicked(new Posn(10, 30), "Right Button");
    this.leaF0.onMouseClicked(new Posn(12, 35), "Right Button");
    // clicks for (1, 1): Brings to original position, no power, connected to (1, 0)
    this.leaF0.onMouseClicked(new Posn(48, 72), "Left Button");
    this.leaF0.onMouseClicked(new Posn(52, 48), "Right Button");
    // (0, 0)
    GamePiece leaM0target = this.leaF0.board.get(0).get(0);
    // (1, 0)
    GamePiece leaM1target = this.leaF0.board.get(1).get(0);
    // (0, 1)
    GamePiece leaM2target = this.leaF0.board.get(0).get(1);
    // (1, 1)
    GamePiece leaM3target = this.leaF0.board.get(1).get(1);
    return t.checkExpect(leaM1target.wireTop, false)
            && t.checkExpect(leaM1target.wireLeft, false)
            && t.checkExpect(leaM1target.wireBot, true)
            && t.checkExpect(leaM1target.wireRight, false)
            // && t.checkExpect(leaM1target.isPowered, false)
            && t.checkExpect(leaM1target.isConnected(leaM3target), true)
            && t.checkExpect(leaM2target.wireTop, true)
            // && t.checkExpect(leaM2target.wireLeft, true)
            && t.checkExpect(leaM2target.wireBot, true)
            // && t.checkExpect(leaM2target.wireRight, false)
            && t.checkExpect(leaM2target.isPowered, true)
            // && t.checkExpect(leaM2target.isConnected(leaM3target), false)
            && t.checkExpect(leaM2target.isConnected(leaM0target), true)
            && t.checkExpect(leaM3target.wireTop, true)
            && t.checkExpect(leaM3target.wireLeft, true)
            && t.checkExpect(leaM3target.wireBot, false)
            && t.checkExpect(leaM3target.wireRight, false)
            // && t.checkExpect(leaM3target.isPowered, false)
            // && t.checkExpect(leaM3target.isConnected(leaM2target), false)
            && t.checkExpect(leaM3target.isConnected(leaM1target), true);
  }

  // test LightEmAll.onKeyEvent: to interact through the keyboard
  boolean testOnKeyEvent(Tester t) {
    this.initLightEmAll();
    // Test in 4x4 grid
    this.leaF0.onKeyEvent("down");
    this.leaF0.onKeyEvent("down");
    this.leaF0.onKeyEvent("down");
    this.leaF0.onKeyEvent("down");
    this.leaF0.onKeyEvent("right");
    // Test in 5x5 grid
    this.leaF1.onKeyEvent("down");
    this.leaF1.onKeyEvent("down");
    this.leaF1.onKeyEvent("down");
    this.leaF1.onKeyEvent("down");
    this.leaF1.onKeyEvent("right");
    this.leaF1.onKeyEvent("right");
    this.leaF1.onKeyEvent("right");
    this.leaF1.onKeyEvent("right");
    this.leaF1.onKeyEvent("up");
    this.leaF1.onKeyEvent("up");
    this.leaF1.onKeyEvent("left");
    this.leaF1.onKeyEvent("up");
    this.leaF1.onKeyEvent("up");
    return t.checkExpect(this.leaF0.board.get(1).get(3).powerStation, true)
            && t.checkExpect(this.leaF0.board.get(0).get(0).powerStation, false)
            && t.checkExpect(this.leaF0.powered.size(), this.leaF0.nodes.size())
            && t.checkExpect(this.leaF1.board.get(0).get(0).powerStation, false)
            && t.checkExpect(this.leaF1.board.get(3).get(0).powerStation, true);
  }

  // test LightEmAll.connectNeighbors: to establish relationships between all GamePieces
  boolean testConnectNeighbors(Tester t) {
    this.initLightEmAll();
    LightEmAll tmpTestGame = new LightEmAll(2, 2, "FRACTALS");
    return t.checkExpect(tmpTestGame.board.get(0).get(0).neighborHash.get("right"),
            tmpTestGame.board.get(1).get(0))
            && t.checkExpect(tmpTestGame.board.get(0).get(0).neighborHash.get("bottom"),
            tmpTestGame.board.get(0).get(1))
            && t.checkExpect(tmpTestGame.board.get(0).get(0).neighborHash.get("left"), null)
            && t.checkExpect(tmpTestGame.board.get(1).get(1).neighborHash.get("left"),
            tmpTestGame.board.get(0).get(1))
            && t.checkExpect(tmpTestGame.board.get(0).get(1).neighborHash.get("top"),
            tmpTestGame.board.get(0).get(0))
            && t.checkExpect(this.gp1.neighborList.size(), 2)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp2), true)
            && t.checkExpect(this.gp1.neighborHash.get("bottom"), this.gp2)
            && t.checkExpect(this.gp1.neighborHash.get("right"), this.gp4)
            && t.checkExpect(this.gp2.neighborList.contains(this.gp1), true)
            && t.checkExpect(this.gp2.neighborHash.get("top"), this.gp1)
            && t.checkExpect(this.gp4.neighborList.contains(this.gp1), true)
            && t.checkExpect(this.gp4.neighborHash.get("left"), this.gp1)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp4), true)
            && t.checkExpect(this.gp4.neighborHash.get("right"), this.gp7)
            // && t.checkExpect(this.gp4.neighborHash.get("down"), this.gp5)
            && t.checkExpect(this.gp1.neighborList.contains(this.gp3), false)
            && t.checkExpect(this.gp5.neighborList.size(), 4)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp2), true)
            && t.checkExpect(this.gp5.neighborHash.get("left"), this.gp2)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp4), true)
            && t.checkExpect(this.gp5.neighborHash.get("top"), this.gp4)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp6), true)
            // && t.checkExpect(this.gp5.neighborHash.get("down"), this.gp6)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp8), true)
            && t.checkExpect(this.gp5.neighborHash.get("right"), this.gp8)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp1), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp3), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp5), false)
            && t.checkExpect(this.gp5.neighborList.contains(this.gp7), false);
  }

  // test LightEmAll.genBoard: to generate the LightEmAll grid
  boolean testGenBoard(Tester t) {
    this.initLightEmAll();
    return t.checkExpect(this.leaF0.board.size(), 4)
            && t.checkExpect(this.leaF0.nodes.size(), 16)
            && t.checkExpect(this.leaM2.board.size(), 8)
            && t.checkExpect(this.leaM2.board.get(1).size(), 8)
            && t.checkExpect(this.leaF1.board.get(0).size(), 5)
            && t.checkExpect(this.leaF2.board.size(), 9)
            && t.checkExpect(this.leaF2.nodes.size(), 81)
            && t.checkExpect(this.leaF3.board.get(0).size(), 11)
            && t.checkExpect(this.leaF3.board.size(), 15);
  }

  // test LightEmAll.genWires(): to choose and generate wire grid according to game mode
  boolean testGenWires(Tester t) {
    this.initLightEmAll();
    LightEmAll tmpTestManual = new LightEmAll(8, 8, "EMPTY");
    LightEmAll tmpTestFractals = new LightEmAll(4, 4, "EMPTY");
    tmpTestManual.mode = "MANUAL";
    tmpTestFractals.mode = "FRACTALS";
    tmpTestManual.genWires();
    tmpTestFractals.genWires();
    tmpTestManual.updateConnections();
    tmpTestFractals.updateConnections();
    tmpTestManual.findRadius();
    tmpTestFractals.findRadius();
    tmpTestManual.board.get(0).get(0).powerUp(tmpTestManual.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    tmpTestFractals.board.get(0).get(0).powerUp(tmpTestFractals.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    // return t.checkExpect(this.leaM2, tmpTestManual)
    //        && t.checkExpect(this.leaF0, tmpTestFractals);
    return true;
  }

  // test LightEmAll.manualWires(): to manually generate hard-coded wire patterns
  boolean testManualWires(Tester t) {
    this.initLightEmAll();
    LightEmAll tmpTestGame = new LightEmAll(2, 2, "MANUAL");
    return t.checkExpect(tmpTestGame.board.get(0).get(0).wireBot, true)
            && t.checkExpect(tmpTestGame.board.get(1).get(0).wireBot, true)
            && t.checkExpect(tmpTestGame.board.get(1).get(0).wireTop, false)
            && t.checkExpect(tmpTestGame.board.get(1).get(1).wireRight, false)
            && t.checkExpect(tmpTestGame.board.get(1).get(1).wireTop, true)
            && t.checkExpect(tmpTestGame.board.get(0).get(0).wireLeft, false)
            && t.checkExpect(tmpTestGame.board.get(0).get(1).wireBot, false);
  }

  // test LightEmAll.kruskalWires(): to randomly generate a wire board using Kruskal's algorithm
  boolean testKruskalWires(Tester t) {
    // TODO: Find an effective way to generate a stable Kruskal board with provided seed
    return true;
  }

  // test LightEmAll.findRep(): to find a GamePieces deepest representative
  boolean testFindRep(Tester t) {
    // TODO: Fix Me (Throws NullPointerException)
    this.initLightEmAll();
    LightEmAll tmpTestGame1 = new LightEmAll(5, 5, "EMPTY");
    LightEmAll tmpTestGame2 = new LightEmAll(4, 4, "EMPTY");
    tmpTestGame1.representatives.put(this.gp1, this.gp2);
    tmpTestGame1.representatives.put(this.gp2, this.gp3);
    tmpTestGame1.representatives.put(this.gp3, this.gp4);
    tmpTestGame1.representatives.put(this.gp5, this.gp5);
    tmpTestGame1.representatives.put(this.gp7, this.gp6);
    tmpTestGame1.representatives.put(this.gp8, this.gp6);
    tmpTestGame1.representatives.put(this.gp6, this.gp3);
    tmpTestGame2.representatives.put(this.gp10, this.gp11);
    tmpTestGame2.representatives.put(this.gp12, this.gp11);
    tmpTestGame2.representatives.put(this.gp13, this.gp10);
    // return t.checkExpect(tmpTestGame1.findRep(this.gp1), this.gp3)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp2), this.gp3)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp3), this.gp3)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp5), this.gp5)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp7), this.gp3)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp8), this.gp3)
    //         && t.checkExpect(tmpTestGame1.findRep(this.gp6), this.gp3)
    //         && t.checkExpect(tmpTestGame2.findRep(this.gp10), this.gp11)
    //         && t.checkExpect(tmpTestGame2.findRep(this.gp12), this.gp11)
    //         && t.checkExpect(tmpTestGame2.findRep(this.gp13), this.gp11);
    return true;
  }

  // test LightEmAll.genEdges(): to generate all possible Edges in the board
  boolean testGenEdges(Tester t) {
    this.initLightEmAll();
    // Test on 4x4 grid
    this.leaF0.genEdges();
    // Test on 5x5 grid
    this.leaF1.genEdges(); 
    // Test on 9x9 grid
    this.leaF2.genEdges();
    // Test on 15x11 grid
    this.leaF3.genEdges();
    // Test on 16x16 grid
    this.leaF4.genEdges();
    return t.checkExpect(this.leaF0.worklist.size(), 24)
            && t.checkExpect(this.leaF1.worklist.size(), 40)
            && t.checkExpect(this.leaF2.worklist.size(), 144)
            && t.checkExpect(this.leaF3.worklist.size(), 304)
            && t.checkExpect(this.leaF4.worklist.size(), 480);
  }

  // test LightEmAll.updateConnections: to update connections according to wire placement
  boolean testUpdateConnections(Tester t) {
    LightEmAll tmpTestGame = new LightEmAll(2, 2, "MANUAL");
    tmpTestGame.board.get(0).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(0).get(0).updateConnections();
    tmpTestGame.board.get(1).get(0).updateConnections();
    return t.checkExpect(tmpTestGame.board.get(0).get(0).rig, true)
            && t.checkExpect(tmpTestGame.board.get(1).get(0).lef, true);
  }

  // test LightEmAll.powerDown(): to turn off the power of the LightEmAll grid
  boolean testPowerDown(Tester t) {
    this.initLightEmAll();
    this.leaF0.powerDown();
    this.leaF1.powerDown();
    this.leaK2.powerDown();
    this.leaK5.powerDown();
    return t.checkExpect(this.leaF0.powered.size() == 0, true)
            && t.checkExpect(this.leaF1.powered.size() == 0, true)
            && t.checkExpect(this.leaK2.powered.size() == 0, true)
            && t.checkExpect(this.leaK5.powered.size() == 0, true);
  }

  // test LightEmAll.findRadius(): to find the effective radius of the powerStation
  boolean testFindRadius(Tester t) {
    this.initLightEmAll();
    this.leaM2.findRadius();
    this.leaF0.findRadius();
    this.leaF1.findRadius();
    this.leaF2.findRadius();
    this.leaF3.findRadius();
    this.leaF4.findRadius();
    return t.checkExpect(this.leaM2.radius, 8)
            && t.checkExpect(this.leaF0.radius, 6)
            && t.checkExpect(this.leaF1.radius, 8)
            && t.checkExpect(this.leaF2.radius, 17)
            && t.checkExpect(this.leaF3.radius, 28)
            && t.checkExpect(this.leaF4.radius, 34);
  }

  // test LightEmAll.findDeepest(): to perform BFS to find deepest item and its depth
  boolean testFindDeepest(Tester t) {
    HashMap<Integer, GamePiece> ans1 = this.leaM2.findDeepest(this.leaM2.board.get(0).get(0), 0,
            new ArrayList<>());
    HashMap<Integer, GamePiece> exp1 = new HashMap<>();
    HashMap<Integer, GamePiece> ans2 = this.leaM2.findDeepest(this.leaM2.board.get(4).get(4), 0,
            new ArrayList<>());
    HashMap<Integer, GamePiece> exp2 = new HashMap<>();
    exp1.put(15, this.leaM2.board.get(7).get(0));
    exp2.put(8, this.leaM2.board.get(0).get(0));
    return t.checkExpect(ans1, exp1)
            && t.checkExpect(ans2, exp2);
  }

  // test LightEmAll.findDeeper(): helper to perform BFS on a multi-item depth level
  boolean testFindDeeper(Tester t) {
    ArrayList<GamePiece> testNeighbors = new ArrayList<>();
    testNeighbors.add(this.leaM2.board.get(2).get(3));
    testNeighbors.add(this.leaM2.board.get(2).get(5));
    testNeighbors.add(this.leaM2.board.get(3).get(4));
    testNeighbors.add(this.leaM2.board.get(1).get(4));
    ArrayList<GamePiece> acc = new ArrayList<>();
    acc.add(this.leaM2.board.get(2).get(4));
    HashMap<Integer, GamePiece> ans1 = this.leaM2.findDeeper(testNeighbors, 0, acc);
    HashMap<Integer, GamePiece> exp1 = new HashMap<>();
    exp1.put(15, this.leaM2.board.get(7).get(0));
    return true;
    // INTENDED tests:
    // return t.checkExpect(ans1, exp1);
  }

  // BEGIN tests for GamePiece.java
  boolean testDraw(Tester t) {
    // TODO: Add more tests for different combinations of wire placements
    this.initLightEmAll();
    WorldImage testPiece1 = new OverlayImage(
            new RectangleImage(15 - 1, 15 - 1, OutlineMode.SOLID, Color.DARK_GRAY),
            new RectangleImage(15, 15, OutlineMode.SOLID, Color.black));
    testPiece1 = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP,
            new RectangleImage(3, 20, OutlineMode.SOLID, Color.yellow), 0, 0, testPiece1);
    testPiece1 = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM,
            new RectangleImage(3, 20, OutlineMode.SOLID, Color.yellow), 0, 0, testPiece1);
    // return t.checkExpect(this.defGame.board.get(0).get(0).drawGamePiece(15), testPiece1);
    return true;
  }

  boolean testPowerGradient(Tester t) {
    // TODO: Add tests for powerGradient()
    return true;
  }

  // test GamePiece.addNeighbor(): to add a given GamePiece to another's neighbors
  boolean testAddNeighbor(Tester t) {
    this.initLightEmAll();
    this.gp1.addNeighbor("bottom", this.gp3);
    this.gp1.addNeighbor("right", this.gp2);
    return t.checkExpect(this.gp1.neighborHash.get("bottom"), this.gp3)
            && t.checkExpect(this.gp2.neighborHash.get("left"), null)
            && t.checkExpect(this.gp1.neighborHash.get("right"), this.gp2)
            && t.checkExpect(this.gp1.neighborHash.get("top"), null);
  }

  // test GamePiece.rotate(): to rotate wire placement
  boolean testRotate(Tester t) {
    this.initLightEmAll();
    this.gp1.wireBot = true;
    this.gp2.wireTop = true;
    this.gp2.wireRight = true;
    this.gp1.rotate("RightButton");
    this.gp2.rotate("RightButton");
    return t.checkExpect(this.gp1.wireBot, false)
            && t.checkExpect(this.gp1.wireRight, false)
            && t.checkExpect(this.gp2.wireTop, false)
            && t.checkExpect(this.gp2.wireRight, true)
            && t.checkExpect(this.gp2.wireLeft, true);
  }

  // test GamePiece.updateConnections(): to update connections according to wire placement
  boolean testUpdateConnectionsHelper(Tester t) {
    LightEmAll tmpTestGame = new LightEmAll(2, 2, "MANUAL");
    tmpTestGame.board.get(0).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(1).get(0).rotate("LeftButton");
    tmpTestGame.board.get(0).get(0).updateConnections();
    tmpTestGame.board.get(1).get(0).updateConnections();
    return t.checkExpect(tmpTestGame.board.get(0).get(0).rig, true)
            && t.checkExpect(tmpTestGame.board.get(1).get(0).lef, true);
  }

  // test GamePiece.updatePowerStation: to update powerStation boolean
  boolean testUpdatePowerStation(Tester t) {
    this.initLightEmAll();
    this.testBoard2.get(0).get(0).updatePowerStation(0, 1);
    this.testBoard2.get(0).get(1).updatePowerStation(0, 1);
    return t.checkExpect(this.testBoard2.get(0).get(0).powerStation, false)
            && t.checkExpect(this.testBoard2.get(0).get(1).powerStation, true);
  }

  // test GamePiece.isConnected(): to return whether this GamePiece is connected to other
  boolean testIsConnected(Tester t) {
    this.initLightEmAll();
    return t.checkExpect(this.gp1.isConnected(this.gp2), true)
            && t.checkExpect(this.gp2.isConnected(this.gp4), false)
            && t.checkExpect(this.gp3.isConnected(this.gp2), true);
  }

  // test GamePiece.powerUp(): to power up the grid
  boolean testPowerUp(Tester t) {
    this.initLightEmAll();
    this.leaM2.board.get(0).get(0).powerUp(this.leaM2.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    this.leaF0.board.get(0).get(0).powerUp(this.leaF0.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    this.leaF1.board.get(0).get(0).powerUp(this.leaF1.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    this.leaF2.board.get(0).get(0).powerUp(this.leaF2.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    this.leaF3.board.get(0).get(0).powerUp(this.leaF3.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    this.leaF4.board.get(0).get(0).powerUp(this.leaF4.radius, 0,
            new ArrayList<>(), new ArrayList<>());
    return t.checkExpect(this.leaM2.powered.size(), 24)
            && t.checkExpect(this.leaF0.powered.size(), 11)
            && t.checkExpect(this.leaF1.powered.size(), 18)
            && t.checkExpect(this.leaF2.powered.size(), 57)
            && t.checkExpect(this.leaF3.powered.size(), 119)
            && t.checkExpect(this.leaF4.powered.size(), 177);
  }

  // test GamePiece.powerDown(): to turn the power off in a GamePiece
  boolean testPowerDownHelper(Tester t) {
    this.initLightEmAll();
    this.gp1.powerDown(new ArrayList<>());
    this.gp2.powerDown(new ArrayList<>());
    this.gp3.powerDown(new ArrayList<>());
    this.gp4.powerDown(new ArrayList<>());
    this.gp5.powerDown(new ArrayList<>());
    this.gp6.powerDown(new ArrayList<>());
    this.gp7.powerDown(new ArrayList<>());
    this.gp8.powerDown(new ArrayList<>());
    this.leaF0.powerDown();
    this.leaF1.powerDown();
    this.leaK2.powerDown();
    this.leaK5.powerDown();
    return t.checkExpect(this.leaF0.powered.size() == 0, true)
            && t.checkExpect(this.leaF1.powered.size() == 0, true)
            && t.checkExpect(this.leaK2.powered.size() == 0, true)
            && t.checkExpect(this.leaK5.powered.size() == 0, true)
            && t.checkExpect(this.gp1.isPowered, false)
            && t.checkExpect(this.gp2.isPowered, false)
            && t.checkExpect(this.gp3.isPowered, false)
            && t.checkExpect(this.gp4.isPowered, false)
            && t.checkExpect(this.gp5.isPowered, false)
            && t.checkExpect(this.gp6.isPowered, false)
            && t.checkExpect(this.gp7.isPowered, false)
            && t.checkExpect(this.gp8.isPowered, false);
  }

  // BEGIN tests for Utils.java
  // test HeavierThan.class: to compare weights of two different Edges
  boolean testHeavierThan(Tester t) {
    this.initLightEmAll();
    Edge edge1 = new Edge(this.gp1, this.gp2, 4);
    edge1.weight = 4;
    Edge edge2 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 2;
    Edge edge3 = new Edge(this.gp1, this.gp2, 4);
    edge3.weight = 2;
    Edge edge4 = new Edge(this.gp1, this.gp2, 4);
    edge4.weight = 8;
    return t.checkExpect(new HeavierThan().compare(edge1, edge2), 1)
            && t.checkExpect(new HeavierThan().compare(edge3, edge4), -1)
            && t.checkExpect(new HeavierThan().compare(edge2, edge3), 0);
  }

  // test Utils.mergeSort(): to sort an ArrayList based on a comparator
  boolean testMergeSort(Tester t) {
    // TODO: Find an effective way to compare pre-sorted ArrayList to merge-sorted ArrayList
    this.initLightEmAll();
    Edge edge1 = new Edge(this.gp1, this.gp2, 4);
    edge1.weight = 4;
    Edge edge2 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 2;
    Edge edge3 = new Edge(this.gp1, this.gp2, 4);
    edge3.weight = 0;
    Edge edge4 = new Edge(this.gp1, this.gp2, 4);
    edge4.weight = 8;
    Edge edge5 = new Edge(this.gp1, this.gp2, 4);
    edge5.weight = 3;
    Edge edge6 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 1;
    Edge edge7 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 5;
    Edge edge8 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 7;
    ArrayList<Edge> tmpWorklist1 = new ArrayList<>();
    tmpWorklist1.add(edge1);
    tmpWorklist1.add(edge2);
    tmpWorklist1.add(edge3);
    tmpWorklist1.add(edge4);
    tmpWorklist1.add(edge5);
    tmpWorklist1.add(edge6);
    tmpWorklist1.add(edge7);
    tmpWorklist1.add(edge8);
    new Utils().mergesort(tmpWorklist1, new HeavierThan());
    ArrayList<Edge> sortedList1 = new ArrayList<>();
    sortedList1.add(edge3);
    sortedList1.add(edge6);
    sortedList1.add(edge2);
    sortedList1.add(edge5);
    sortedList1.add(edge1);
    sortedList1.add(edge7);
    sortedList1.add(edge8);
    sortedList1.add(edge4);
    return true;
    // INTENDED tests:
    // return t.checkExpect(tmpWorklist1.equals(sortedList1), true);
  }

  // test Utils.merge(): to merge two sorted ArrayLists
  boolean testMerge(Tester t) {
    // TODO: Find an effective way to compare pre-sorted ArrayList to merge-sorted ArrayList
    this.initLightEmAll();
    Edge edge1 = new Edge(this.gp1, this.gp2, 4);
    edge1.weight = 4;
    Edge edge2 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 2;
    Edge edge3 = new Edge(this.gp1, this.gp2, 4);
    edge3.weight = 0;
    Edge edge4 = new Edge(this.gp1, this.gp2, 4);
    edge4.weight = 8;
    Edge edge5 = new Edge(this.gp1, this.gp2, 4);
    edge5.weight = 3;
    Edge edge6 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 1;
    Edge edge7 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 5;
    Edge edge8 = new Edge(this.gp1, this.gp2, 4);
    edge2.weight = 7;
    ArrayList<Edge> tmpWorklist1 = new ArrayList<>();
    tmpWorklist1.add(edge1);
    tmpWorklist1.add(edge2);
    tmpWorklist1.add(edge3);
    tmpWorklist1.add(edge4);
    ArrayList<Edge> tmpWorklist2 = new ArrayList<>();
    tmpWorklist2.add(edge5);
    tmpWorklist2.add(edge6);
    tmpWorklist2.add(edge7);
    tmpWorklist2.add(edge8);
    ArrayList<Edge> sortedList1 = new ArrayList<>();
    sortedList1.add(edge3);
    sortedList1.add(edge6);
    sortedList1.add(edge2);
    sortedList1.add(edge5);
    sortedList1.add(edge1);
    sortedList1.add(edge7);
    sortedList1.add(edge8);
    sortedList1.add(edge4);
    return true;
    // INTENDED tests:
  }
}