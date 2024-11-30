# LightEmAll Game - README

## Overview
**LightEmAll** is an interactive puzzle game implemented in Java using `javalib.impworld`. The goal of the game is to connect a grid of GamePieces with wires such that all pieces are powered by a central power station. The game supports multiple modes for generating wire patterns and offers graphical interaction through mouse and keyboard inputs.

---

## Features

### Core Gameplay
- **Game Board**: A grid of `GamePiece` objects connected by wires.
- **Power Station**: A central piece that powers the grid within a specific radius.
- **Game Modes**:
  - **Manual**: Predefined wire layouts.
  - **Fractals**: Procedurally generated fractal-like patterns.
  - **Kruskal**: Random wire patterns generated using Kruskal's algorithm.
  - **Empty**: Blank board for testing purposes.
- **Interactivity**:
  - Mouse Clicks:
    - Left-click to rotate pieces counter-clockwise.
    - Right-click to rotate pieces clockwise.
  - Keyboard Controls:
    - Move the power station using arrow keys.

### Additional Features
- **Dynamic Power Radius**: The effective radius of the power station adjusts based on the board layout.
- **Power Visualization**: GamePieces change color based on their distance from the power station.
- **Graphical User Interface**: Fully interactive with real-time updates.

---

## Classes and Responsibilities

### 1. `LightEmAll`
- Manages the game board and gameplay logic.
- Generates the board and wires based on the chosen mode.
- Handles user input and updates the game state.

### 2. `GamePiece`
- Represents a single piece on the board.
- Tracks its connections, power status, and visual representation.
- Rotates wires and updates connections dynamically.

### 3. `Edge`
- Represents a connection (wire) between two GamePieces.
- Used in Kruskal's algorithm to generate random wire patterns.

### 4. `Utils`
- Provides helper methods like merge sort for sorting edges.

### 5. `ExamplesLightEmAll`
- Contains test cases to validate the game's functionality.
- Includes a `testBigBang` method to start the game interactively.

---

## Game Modes
- **Manual**: Hardcoded wire layouts for specific gameplay scenarios.
- **Fractals**: Recursive wire generation for complex, unique layouts.
- **Kruskal**: Random wire placement using Minimum Spanning Tree principles.
- **Empty**: No wires; used for testing.

---

## How to Play
1. **Setup**:
   - Instantiate a `LightEmAll` object with desired dimensions and mode.
   - Example: `LightEmAll game = new LightEmAll(5, 5, "KRUSKAL");`
2. **Game Start**:
   - Use `bigBang` to launch the game.
   - Example: `game.bigBang(200, 200);`
3. **Controls**:
   - **Mouse**: Rotate pieces by clicking (Left/Right).
   - **Keyboard**: Move the power station using arrow keys (`up`, `down`, `left`, `right`).

---

## Testing
The `ExamplesLightEmAll` class includes comprehensive tests for:
- Board and wire generation.
- Power station radius calculations.
- User interactions (mouse and keyboard).
- Connectivity and power propagation.

---

## Known Limitations and Future Enhancements
- **Limitations**:
  - Visuals are basic; future versions could include smoother animations and better design.
- **Potential Enhancements**:
  - Add a scoring system.
  - Introduce levels with increasing difficulty.
  - Implement more visual effects and power gradients.

---

## Dependencies
- `javalib.impworld` for game world management.
- `tester` library for unit testing.

---

## How to Run
1. Compile and run the `LightEmAll` class.
2. Use the `testBigBang` method in `ExamplesLightEmAll` to play the game.
