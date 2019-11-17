package tablut;
import java.util.Stack;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Formatter;
import java.util.Arrays;
import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.SQUARE_LIST;
import static tablut.Square.sq;


/**
 * The state of a Tablut Game.
 *
 * @author Shreyansh Loharuka
 */
class Board {

    /**
     * The number of squares on a side of the board.
     */
    static final int SIZE = 9;

    /**
     * The throne (or castle) square and its four surrounding squares..
     */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /**
     * Initial positions of attackers.
     */
    static final Square[] INITIAL_ATTACKERS = {
            sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
            sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
            sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
            sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /**
     * Initial positions of defenders of the king.
     */
    static final Square[] INITIAL_DEFENDERS = {NTHRONE, ETHRONE,
                                                  STHRONE, WTHRONE,
                                                sq(4, 6), sq(4, 2),
                                                sq(2, 4), sq(6, 4)
    };

    /**
     * Initial Board.
     */
    private Square[][] _board;

    /**
     * Contents of the Board.
     */
    private Piece[][] _contents;
    /**
     * Position of Attackers.
     */
    private Square[] _attackers;

    /**
     * Position of Defenders.
     */
    private Square[] _defenders;

    /**
     * Initializes a game board with SIZE squares on a side in the
     * initial position.
     */
    Board() {
        init();
    }


    /**
     * Initializes a copy of MODEL.
     */
    Board(Board model) {
        copy(model);
    }


    /** Copies MODEL into me.
     * */
    @SuppressWarnings("unchecked")
    void copy(Board model) {
        if (model == this) {
            return;
        }
        _attackers = new Square[16];
        _defenders = new Square[8];
        _board = new Square[SIZE][SIZE];
        _contents = new Piece[SIZE][SIZE];
        _turn = model._turn;
        _moveCount = 0;
        _repeated = false;
        _winner = null;
        _stateOfBoard = new Stack<Board>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                _board[i][j] = sq(i, j);
            }
        }
        System.arraycopy(model._attackers, 0, _attackers, 0, 16);
        System.arraycopy(model._defenders, 0, _defenders, 0, 8);
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(model._contents[i], 0, this._contents[i], 0, SIZE);
        }
        _stateOfBoard = (Stack<Board>) model._stateOfBoard.clone();
    }

    /**
     * Clears the board to the initial position.
     */
    void init() {
        _board = new Square[SIZE][SIZE];
        _attackers = new Square[16];
        _defenders = new Square[8];
        _turn = BLACK;
        _moveCount = 0;
        _moveLimit = 0;
        _repeated = false;
        _winner = null;
        _stateOfBoard = new Stack<Board>();
        System.arraycopy(INITIAL_ATTACKERS, 0, _attackers, 0, 16);
        System.arraycopy(INITIAL_DEFENDERS, 0, _defenders, 0, 8);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                _board[i][j] = sq(i, j);
            }
        }
        _contents = new Piece[SIZE][SIZE];
        for (Square[] row : _board) {
            for (Square square : row) {
                if (square.equals(THRONE)) {
                    put(KING, square);
                } else if (Arrays.asList(_defenders).contains(square)) {
                    put(WHITE, square);
                } else if (Arrays.asList(_attackers).contains(square)) {
                    put(BLACK, square);
                } else {
                    put(EMPTY, square);
                }
            }
        }
        Board copy = new Board(this);
        _stateOfBoard.push(copy);
    }

    /**
     * Takes in CONTENTS Make a copy of the contents of the board and return it.
     */
    public Piece[][] copyContents(Piece[][] contents) {
        Piece[][] copy = new Piece[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(contents[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    /**
     *Returns contents of the board.
     */
    Piece[][] getContents() {
        return _contents;
    }

    /**
     * Return the board.
     */
    Square[][] getBoard() {
        return _board;
    }

    /**
     * Takes N and Set the move limit to LIM.
     * It is an error if 2*LIM <= moveCount().
     */
    void setMoveLimit(int n) {
        _moveLimit = n;
    }

    /**
     * Return a Piece representing whose move it is (WHITE or BLACK).
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return the winner in the current position, or null if there is no winner
     * yet.
     */
    Piece winner() {
        return _winner;
    }

    /**
     * Returns true iff this is a win due to a repeated position.
     */
    boolean repeatedPosition() {
        return _repeated;
    }

    /**
     * Record current position and set winner() next mover if the current
     * position is a repeat.
     */
    private void checkRepeated() {
        for (Board b : _stateOfBoard) {
            if (Arrays.deepEquals(b._contents, _contents)) {
                if (b._turn.equals(_turn)) {
                    _repeated = true;
                    _winner = _turn;
                    return;
                }
            }
        }
    }

    /**
     * Return the number of moves since the initial position that have not been
     * undone.
     */
    int moveCount() {
        return _moveCount;
    }

    /**
     * Return location of the king.
     */
    Square kingPosition() {
        for (Square[] row : _board) {
            for (Square square : row) {
                if (get(square) == KING) {
                    return square;
                }
            }
        }
        return null;
    }

    /**
     * Return the contents of the square at S.
     */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /**
     * Return the contents of the square at (COL, ROW), where
     * 0 <= COL, ROW <= 9.
     */
    final Piece get(int col, int row) {
        return _contents[row][col];
    }

    /**
     * Return the contents of the square at COL ROW.
     */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /**
     * Set square S to P.
     */
    final void put(Piece p, Square s) {
        _contents[s.row()][s.col()] = p;

    }

    /**
     * Set square S to P and record for undoing.
     */
    final void revPut(Piece p, Square s) {
        put(p, s);
    }

    /**
     * Set square COL ROW to P.
     */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }


    /**
     * Move FROM-TO, assuming this is a legal move.
     */
    void makeMove(Square from, Square to) {
        if (isLegal(from)) {
            if (_moveCount >= 2 * _moveLimit && _moveLimit != 0) {
                _winner = _turn.opponent();
                return;
            }
            revPut(get(from), to);
            revPut(EMPTY, from);

            HashSet<Square> loc = pieceLocations(turn());
            if (!loc.contains(THRONE)) {
                loc.add(THRONE);
            }
            _moveCount += 1;
            for (Square square : loc) {
                capture(to, square);
            }
            _turn = _turn.opponent();
            checkRepeated();
            Board copy = new Board(this);
            _stateOfBoard.push(copy);
            if (kingPosition() != null && kingPosition().isEdge()) {
                _winner = WHITE;
            }
        }

    }

    /**
     * Move according to MOVE, assuming it is a legal move.
     */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /**
     * Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     * SQ0 and the necessary conditions are satisfied.
     */
    private void capture(Square sq0, Square sq2) {
        if (!canCapture(sq0, sq2)) {
            return;
        }
        Square sq1 = sq0.between(sq2);
        Piece type1 = get(sq1);
        if ((sq1 == THRONE || sq1 == NTHRONE || sq1 == STHRONE
                || sq1 == ETHRONE || sq1 == WTHRONE) && type1 == KING) {
            Square sq3 = sq0.diag1(sq1);
            Square sq4 = sq0.diag2(sq1);
            if (hostility(sq0, sq1, sq2) && hostility(sq3, sq1, sq4)) {
                revPut(EMPTY, sq1);
                _winner = BLACK;
            }
            return;
        }
        if (hostility(sq0, sq1, sq2)) {
            if (type1 == KING) {
                _winner = BLACK;
            }
            revPut(EMPTY, sq1);
        }
    }


    /**
     * Returns true if SQ0 and SQ2 has something in between them.
     */
    boolean canCapture(Square sq0, Square sq2) {
        int[][] dir = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}
        };
        for (int i = 0; i < 4; i++) {
            Square square = sq0.rookMove(i, 2);

            if (square == sq2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if SQ0 and SQ2 are hostile to SQ1.
     */
    boolean hostility(Square sq0, Square sq1, Square sq2) {
        boolean same = false, opposite = false;
        Piece type0 = get(sq0).side();
        Piece type1 = get(sq1).side();
        Piece type2 = get(sq2).side();
        if (sq0 == THRONE && type0 == EMPTY) {
            return type1 != type2;
        } else if (sq2 == THRONE && type2 == EMPTY) {
            return type1 != type0;
        } else if (sq2 == THRONE && type2 == WHITE) {
            if (type1 == WHITE) {
                Square a0 = sq2.rookMove(0, 1);
                Square a1 = sq2.rookMove(1, 1);
                Square a2 = sq2.rookMove(2, 1);
                Square a3 = sq2.rookMove(3, 1);
                return anyThree(a0, a1, a2, a3);
            } else if (type1 == BLACK) {
                return type0 == type2;
            } else {
                return true;
            }
        } else {
            if (type0 == type2 && type0 != EMPTY) {
                same = true;
                if (type1 != type2) {
                    opposite = true;
                }
            }
            return same && opposite;
        }
    }


    /** Takes in SQ0,SQ1,SQ2,SQ3 and returns if three sides are covered.*/
    private boolean anyThree(Square sq0, Square sq1, Square sq2, Square sq3) {
        if (_contents[sq0.row()][sq0.col()] == WHITE) {
            boolean t1 = _contents[sq1.row()][sq1.col()] == BLACK;
            boolean t2 = _contents[sq2.row()][sq2.col()] == BLACK;
            boolean t3 = _contents[sq3.row()][sq3.col()] == BLACK;
            return t1 && t2 && t3;
        } else if (_contents[sq1.row()][sq1.col()] == WHITE) {
            boolean t1 = _contents[sq0.row()][sq0.col()] == BLACK;
            boolean t2 = _contents[sq2.row()][sq2.col()] == BLACK;
            boolean t3 = _contents[sq3.row()][sq3.col()] == BLACK;
            return t1 && t2 && t3;
        } else if (_contents[sq2.row()][sq2.col()] == WHITE) {
            boolean t1 = _contents[sq0.row()][sq0.col()] == BLACK;
            boolean t2 = _contents[sq1.row()][sq1.col()] == BLACK;
            boolean t3 = _contents[sq3.row()][sq3.col()] == BLACK;
            return t1 && t2 && t3;
        } else if (_contents[sq3.row()][sq3.col()] == WHITE) {
            boolean t1 = _contents[sq0.row()][sq0.col()] == BLACK;
            boolean t2 = _contents[sq2.row()][sq2.col()] == BLACK;
            boolean t3 = _contents[sq1.row()][sq1.col()] == BLACK;
            return t1 && t2 && t3;
        } else {
            return false;
        }
    }

    /**
     * Undo one move.  Has no effect on the initial board.
     */
    void undo() {
        if (moveCount() > 0) {
            undoPosition();
            _turn = _turn.opponent();
        }
    }

    /**
     * Remove record of current position in the set of positions encountered,
     * unless it is a repeated position or we are at the first move.
     */
    private void undoPosition() {
        if (!_repeated) {
            if (!_stateOfBoard.empty()) {
                _stateOfBoard.pop();
                _contents = copyContents(_stateOfBoard.peek()._contents);
            }
        }
        _repeated = false;
    }

    /**
     * Clear the undo stack and board-position counts. Does not modify the
     * current position or win status.
     */
    void clearUndo() {
        _stateOfBoard = new Stack<Board>();
        Board copy = new Board(this);
        _stateOfBoard.push(copy);
        _moveCount = 1;
    }

    /**
     * Return true iff SIDE has a legal move.
     */
    boolean hasMove(Piece side) {
        return !legalMoves(side).isEmpty();
    }

    /**
     * Return a new mutable list of all legal moves on the current board for
     * SIDE (ignoring whose turn it is at the moment).
     */
    List<Move> legalMoves(Piece side) {
        List<Move> legal = new ArrayList<Move>();
        HashSet<Square> loc = pieceLocations(side);
        for (Square square : loc) {
            int ind = square.index();
            for (List<Move> sqList : ROOK_MOVES[ind]) {
                for (Move move : sqList) {
                    if (isLegal(move)) {
                        legal.add(move);
                    }
                }
            }
        }
        return legal;
    }

    /**
     * Return true iff FROM is a valid starting square for a move.
     */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /**
     * Return true iff FROM-TO is a valid move.
     */
    boolean isLegal(Square from, Square to) {
        if (from.isRookMove(to)) {
            if (to == THRONE && get(from) != KING) {
                return false;
            }
            return isUnblockedMove(from, to) && isLegal(from);
        }
        return false;
    }

    /**
     * Return true iff MOVE is a legal move in the current
     * position.
     */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /**
     * Return true iff FROM - TO is an unblocked rook move on the current
     * board.  For this to be true, FROM-TO must be a rook move and the
     * squares along it, other than FROM, must be empty.
     */
    boolean isUnblockedMove(Square from, Square to) {
        if (from.row() == to.row()) {
            int row = from.row();
            if (from.col() < to.col()) {
                for (int i = from.col() + 1; i <= to.col(); i++) {
                    if (_contents[row][i] != EMPTY) {
                        return false;
                    }
                }
            } else {
                for (int i = from.col() - 1; i >= to.col(); i--) {
                    if (_contents[row][i] != EMPTY) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            int col = from.col();
            if (from.row() < to.row()) {
                for (int i = from.row() + 1; i <= to.row(); i++) {
                    if (_contents[i][col] != EMPTY) {
                        return false;
                    }
                }
            } else {
                for (int i = from.row() - 1; i >= to.row(); i--) {
                    if (_contents[i][col] != EMPTY) {
                        return false;
                    }
                }
            }
            return true;
        }
    }


    /**
     * Return the locations of all pieces on SIDE.
     */
    HashSet<Square> pieceLocations(Piece side) {
        HashSet<Square> loc = new HashSet<Square>();
        assert side != EMPTY;
        Piece type = side.side();
        for (Square[] row : _board) {
            for (Square square : row) {
                if (get(square).side() == type) {
                    loc.add(square);
                }
            }
        }
        return loc;
    }

    /**
     * Return the contents of _board in the order of SQUARE_LIST as a sequence
     * of characters: the toString values of the current turn and Pieces.
     */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Return a text representation of this Board.  If COORDINATES, then row
     * and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /**
     * Piece whose turn it is (WHITE or BLACK).
     */
    private Piece _turn;
    /**
     * Cached value of winner on this board, or EMPTY if it has not been
     * computed.
     */
    private Piece _winner;
    /**
     * Number of (still undone) moves since initial position.
     */
    private int _moveCount;
    /**
     * True when current board is a repeated position (ending the game).
     */
    private boolean _repeated;
    /**
     * Move Limit before winner is declared.
     */
    private int _moveLimit;
    /**
     * Stack of board states.
     */
    private Stack<Board> _stateOfBoard;


}
