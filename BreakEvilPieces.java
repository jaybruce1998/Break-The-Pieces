import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single shape extracted from the ASCII-art diagram.
 * Stores the shape's grid and its bounding box coordinates.
 */
class Shape {
    String[] grid; // The grid representing the shape's characters
    int firstRow, firstCol, lastRow, lastCol; // Bounding box coordinates

    /**
     * Constructs a Shape from a character grid and bounding box coordinates.
     * @param g The character grid representing the shape
     * @param a First row index (inclusive)
     * @param b First column index (inclusive)
     * @param c Last row index (inclusive)
     * @param d Last column index (inclusive)
     */
    Shape(char[][] g, int a, int b, int c, int d) {
        grid = new String[g.length];
        for (int i = 0; i < g.length; i++) {
            grid[i] = String.valueOf(g[i]);
        }
        firstRow = a;
        firstCol = b;
        lastRow = c;
        lastCol = d;
    }

    /**
     * Removes a column from the shape's grid if it falls within the bounding box.
     * Used to reverse column expansions added during edge disambiguation.
     * @param c The column index to remove (relative to the original grid)
     */
    void resizeCols(int c) {
        if (c <= firstCol || c >= lastCol) return;
        int offset = c - firstCol;
        for (int i = 0; i < grid.length; i++) {
            grid[i] = grid[i].substring(0, offset) + grid[i].substring(offset + 1);
        }
        lastCol--;
    }

    /**
     * Removes a row from the shape's grid if it falls within the bounding box.
     * Used to reverse row expansions added during edge disambiguation.
     * @param r The row index to remove (relative to the original grid)
     */
    void resizeRows(int r) {
        if (r <= firstRow || r >= lastRow) return;
        int offset = r - firstRow;
        String[] g = new String[grid.length - 1];
        System.arraycopy(grid, 0, g, 0, offset);
        System.arraycopy(grid, offset + 1, g, offset, grid.length - offset - 1);
        grid = g;
        lastRow--;
    }

    /**
     * Converts the shape's grid to a string representation, trimming trailing spaces.
     * @return A string representing the shape, with each row separated by a newline
     */
    String stringify() {
        StringBuilder sb = new StringBuilder();
        for (String l : grid) {
            sb.append("\n").append(l.stripTrailing());
        }
        return sb.substring(1);
    }

    /**
     * Provides a detailed string representation of the shape, including its bounding box.
     * @return A string with bounding box coordinates and the grid content
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(")
                .append(firstRow).append(", ").append(firstCol)
                .append(")->(").append(lastRow).append(", ").append(lastCol).append("):");
        for (String l : grid) {
            sb.append("\n").append(l);
        }
        return sb.toString();
    }
}

/**
 * Solves the problem of decomposing an ASCII-art diagram into minimal enclosed shapes.
 * Uses flood-fill to identify enclosed regions and constructs shapes with proper boundaries.
 */
public class BreakEvilPieces {
    private static final char FILL_START = '}'; // Starting ASCII character for flood-fill markers

    /**
     * Expands columns in the grid to disambiguate overlapping edges (e.g., "++", "||", "+|").
     * Inserts separator characters ('-' or ' ') and tracks expanded column indices.
     * @param l The input grid as an array of strings
     * @param eCols List to store indices of expanded columns
     * @param len The current width of the grid
     * @return The updated width of the grid after expansion
     */
    private static int expandCols(String[] l, List<Integer> eCols, int len) {
        for (int i = 0; i < l.length; i++) {
            boolean changed;
            do {
                changed = false;
                // Check for overlapping edges
                int p = l[i].indexOf("++");
                if (p < 0) p = l[i].indexOf("||");
                if (p < 0) p = l[i].indexOf("+|");
                if (p >= 0) {
                    p++; // Move to insertion point
                    // Insert separator character across all rows
                    for (int j = 0; j < l.length; j++) {
                        char left = l[j].charAt(p - 1), right = l[j].charAt(p);
                        char insert = (left == '-' || right == '-' || (left == '+' && right == '+')) ? '-' : ' ';
                        l[j] = l[j].substring(0, p) + insert + l[j].substring(p);
                    }
                    len++;
                    // Update indices of previously expanded columns
                    for (int n = 0; n < eCols.size(); n++) {
                        if (eCols.get(n) > p) {
                            eCols.set(n, eCols.get(n) + 1);
                        }
                    }
                    eCols.add(p);
                    changed = true;
                }
            } while (changed);
        }
        return len;
    }

    /**
     * Decomposes an ASCII-art diagram into a list of enclosed shapes.
     * Expands overlapping edges, identifies enclosed regions via flood-fill,
     * and constructs shapes with proper boundaries.
     * @param shape The input ASCII-art diagram as a string
     * @return A list of strings, each representing a shape
     */
    public static List<String> solve(String shape) {
        List<String> r = new ArrayList<>();
        List<Integer> eCols = new ArrayList<>(), eRows = new ArrayList<>();
        String[] l = shape.split("\n");

        // Determine maximum width and pad rows to equal length
        int len = 0;
        for (String s : l) len = Math.max(len, s.length());

        for (int i = 0; i < l.length; i++) {
            l[i] = l[i] + " ".repeat(len - l[i].length());
        }

        // Expand columns to disambiguate edges
        int pastLen = len;
        len = expandCols(l, eCols, len);

        // Expand rows to disambiguate vertical overlaps
        for (int i = 1; i < l.length; i++) {
            for (int j = 0; j < len; j++) {
                if ((l[i].charAt(j) == '+' && l[i - 1].charAt(j) == '+') ||
                    (l[i].charAt(j) == '-' && l[i - 1].charAt(j) == '-')) {
                    // Insert a new row with appropriate separators
                    char[] line = new char[len];
                    for (int n = 0; n < len; n++) {
                        char top = l[i - 1].charAt(n), bot = l[i].charAt(n);
                        line[n] = (bot == '|' || top == '|' || (bot == '+' && top == '+')) ? '|' : ' ';
                    }
                    String[] g = new String[l.length + 1];
                    System.arraycopy(l, 0, g, 0, i);
                    g[i] = new String(line);
                    System.arraycopy(l, i, g, i + 1, l.length - i);
                    l = g;
                    eRows.add(i);
                }
            }
        }

        // Re-expand columns if row expansion occurred
        if (pastLen != len) {
            len = expandCols(l, eCols, len);
        }

        // Convert to character array for processing
        char[][] a = new char[l.length][len];
        for (int i = 0; i < a.length; i++) a[i] = l[i].toCharArray();

        // Sort expanded columns in reverse order for removal
        eCols.sort(Collections.reverseOrder());

        // Process shapes using flood-fill
        List<Shape> shapes = process(a);
        // Remove expanded columns and rows
        for (int col : eCols) {
            for (Shape s : shapes) {
                s.resizeCols(col);
            }
        }
        for (int i = eRows.size() - 1; i >= 0; i--) {
            for (Shape s : shapes) {
                s.resizeRows(eRows.get(i));
            }
        }
        // Collect stringified shapes
        for (Shape s : shapes) r.add(s.stringify());
        return r;
    }

    /**
     * Identifies enclosed regions in the grid using flood-fill and constructs shapes.
     * Marks enclosed regions with unique characters and converts them to Shape objects.
     * @param a The character grid
     * @return A list of Shape objects representing enclosed regions
     */
    public static List<Shape> process(char[][] a) {
        List<Shape> r = new ArrayList<>();
        char n = FILL_START;
        // Flood-fill all blank regions
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j] == ' ' && !fill(a, i, j, ' ', n++)) {
                    // If region leaks to boundary, mark it with ','
                    fill(a, i, j, --n, ',');
                }
            }
        }
        // Convert each enclosed region to a Shape
        for (char c = FILL_START; c < n; c++) {
            r.add(stringify(a, c));
        }
        return r;
    }

    /**
     * Sets a grid cell to the specified character if it is a space.
     * Used to draw boundary characters ('-', '|', '+') during shape construction.
     * @param a The character grid
     * @param i Row index
     * @param j Column index
     * @param c The character to set
     */
    private static void put(char[][] a, int i, int j, char c) {
        if (a[i][j] == ' ') a[i][j] = c;
    }

    /**
     * Finds the first row containing the specified fill character.
     * @param a The character grid
     * @param z The fill character to search for
     * @return The first row index containing z, or -1 if not found
     */
    private static int firstRow(char[][] a, char z) {
        for (int i = 1; i < a.length; i++) {
            for (int j = 1; j < a[i].length; j++) {
                if (a[i][j] == z) return i;
            }
        }
        return -1;
    }

    /**
     * Finds the first column in or after the specified row containing the fill character.
     * @param a The character grid
     * @param z The fill character to search for
     * @param r The starting row index
     * @return The first column index containing z, or -1 if not found
     */
    private static int firstColumn(char[][] a, char z, int r) {
        for (int j = 1; j < a[r].length; j++) {
            for (int i = r; i < a.length; i++) {
                if (a[i][j] == z) return j;
            }
        }
        return -1;
    }

    /**
     * Finds the last row containing the specified fill character, starting from a given row and column.
     * @param a The character grid
     * @param z The fill character to search for
     * @param r The starting row index
     * @param c The starting column index
     * @return The last row index containing z, or r if not found
     */
    private static int lastRow(char[][] a, char z, int r, int c) {
        for (int i = a.length - 2; i > r; i--) {
            for (int j = c; j < a[i].length; j++) {
                if (a[i][j] == z) return i;
            }
        }
        return r;
    }

    /**
     * Finds the last column containing the specified fill character within a row range.
     * @param a The character grid
     * @param z The fill character to search for
     * @param r The starting row index
     * @param c The starting column index
     * @param l The last row index to search
     * @return The last column index containing z, or c if not found
     */
    private static int lastColumn(char[][] a, char z, int r, int c, int l) {
        for (int j = a[r].length - 1; j > c; j--) {
            for (int i = r; i <= l; i++) {
                if (a[i][j] == z) return j;
            }
        }
        return c;
    }

    /**
     * Constructs a Shape from a filled region in the grid.
     * Defines the bounding box, draws boundaries, and clears the fill character.
     * @param a The character grid
     * @param c The fill character identifying the region
     * @return A Shape object representing the enclosed region
     */
    private static Shape stringify(char[][] a, char c) {
        // Determine bounding box
        int fr = firstRow(a, c) - 1;
        int fc = firstColumn(a, c, fr) - 1;
        int lr = lastRow(a, c, fr, fc) + 1;
        int lc = lastColumn(a, c, fr, fc, lr) + 1;

        // Initialize subgrid
        char[][] r = new char[lr - fr + 1][lc - fc + 1];
        for (int i = 0; i < r.length; i++) {
            for (int j = 0; j < r[i].length; j++) {
                r[i][j] = ' ';
            }
        }

        // Copy filled region
        for (int i = 1; i < r.length - 1; i++) {
            for (int j = 1; j < r[i].length - 1; j++) {
                if (a[i + fr][j + fc] == c) r[i][j] = c;
            }
        }

        // Draw boundaries around filled cells
        for (int i = 1; i < r.length; i++) {
            for (int j = 1; j < r[i].length; j++) {
                if (r[i][j] == c) {
                    put(r, i - 1, j, '-');
                    put(r, i, j - 1, '|');
                    put(r, i, j + 1, '|');
                    put(r, i + 1, j, '-');
                }
            }
        }

        // Fix corners to place '+' where horizontal and vertical lines meet
        for (int i = 0; i < r.length; i++) {
            for (int j = 1; j < r[i].length; j++) {
                if (r[i][j] == '-' && (r[i][j - 1] == ' ' || r[i][j - 1] == '|')) r[i][j - 1] = '+';
            }
        }
        for (int i = 0; i < r.length; i++) {
            for (int j = 1; j < r[i].length; j++) {
                if ((r[i][j] == ' ' || r[i][j] == '|') && r[i][j - 1] == '-') r[i][j] = '+';
            }
        }
        for (int i = 1; i < r.length; i++) {
            for (int j = 0; j < r[i].length; j++) {
                if (r[i][j] == '|' && r[i - 1][j] == '-') r[i - 1][j] = '+';
            }
        }
        for (int i = 1; i < r.length; i++) {
            for (int j = 0; j < r[i].length; j++) {
                if (r[i][j] == '-' && r[i - 1][j] == '|') r[i][j] = '+';
            }
        }

        // Clear fill character, leaving boundaries and original content
        for (int i = 1; i < r.length; i++) {
            for (int j = 1; j < r[i].length; j++) {
                if (r[i][j] == c) r[i][j] = ' ';
            }
        }

        return new Shape(r, fr, fc, lr, lc);
    }

    /**
     * Performs flood-fill starting from the given cell, marking with a new character.
     * Returns false if the region touches the grid boundary (indicating it's not enclosed).
     * @param a The character grid
     * @param i Starting row index
     * @param j Starting column index
     * @param s The character to replace (usually ' ')
     * @param n The new character to mark the region
     * @return true if the region is fully enclosed, false if it touches the boundary
     */
    private static boolean fill(char[][] a, int i, int j, char s, char n) {
        if (i < 0 || i >= a.length || j < 0 || j >= a[i].length) return false;
        if (a[i][j] != s) return true;
        a[i][j] = n;
        boolean w = fill(a, i - 1, j, s, n);
        boolean x = fill(a, i + 1, j, s, n);
        boolean y = fill(a, i, j - 1, s, n);
        boolean z = fill(a, i, j + 1, s, n);
        return w && x && y && z;
    }
}
