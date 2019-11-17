package tablut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;

/**
 * A Player that automatically generates moves.
 *
 * @author Shreyansh Loharuka
 */
class AI extends Player {

    /**
     * A position-score magnitude indicating a win (for white if positive,
     * black if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /**
     * A position-score magnitude indicating a forced win in a subsequent
     * move.  This differs from WINNING_VALUE to avoid putting off wins.
     */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI with no piece or controller (intended to produce
     * a template).
     */
    AI() {
        this(null, null);
    }

    /**
     * A new AI playing PIECE under control of CONTROLLER.
     */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        Move move = findMove();
        if (board().winner() != null || board().turn() != super._myPiece) {
            _controller.reportMove(move);
            return "dump";
        } else if (move != null) {
            _controller.reportMove(move);
            return move.toString();
        }
        return "dump";
    }

    @Override
    boolean isManual() {
        return false;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        int s;
        if (super.myPiece() == BLACK) {
            s = -1;
        } else {
            s = 1;
        }
        findMove(b, maxDepth(b), true, s, -INFTY, INFTY);
        return _lastFoundMove;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        int best = 0;
        Move bestMove = null;
        if (depth == 0 || board.winner() != null) {
            return staticScore(board);
        } else if (board.winner() == myPiece() && myPiece() == BLACK) {
            return -WINNING_VALUE;
        } else if (board.winner() == myPiece() && myPiece() == WHITE) {
            return WINNING_VALUE;
        } else if (board.winner() == myPiece().opponent()) {
            return -INFTY;
        }

        if (sense == 1) {
            best = -INFTY;
            for (Move move : board.legalMoves(WHITE)) {
                board.makeMove(move);
                int response = findMove(board, depth - 1,
                        false, -1, alpha, beta);
                board.undo();
                if (response >= best) {
                    bestMove = move;
                    best = response;
                    alpha = max(alpha, response);
                    if (beta <= alpha) {
                        break;
                    }
                }

            }

        } else {
            best = INFTY;
            for (Move move : board.legalMoves(BLACK)) {
                board.makeMove(move);
                int response = findMove(board, depth - 1,
                        false, 1, alpha, beta);
                board.undo();
                if (response <= best) {
                    bestMove = move;
                    best = response;
                    beta = min(beta, response);
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = bestMove;
        }
        return best;
    }

    /**
     * Return a heuristically determined maximum search depth
     * based on characteristics of BOARD.
     */
    private static int maxDepth(Board board) {
        int b = 0, w = 0;
        for (Piece[] row : board.getContents()) {
            for (Piece piece : row) {
                if (piece == BLACK) {
                    b++;
                } else {
                    w++;
                }
            }
        }
        int a = 16 + 9, c = 16 + 6, d = 16 + 3,
                e = 16, f = 16 - 3, g = 16 - 6;
        if (b + w >= a) {
            return 3;
        } else if (b + w >= c) {
            return 4;
        } else if (b + w >= d) {
            return 5;
        } else if (b + w >= e) {
            return 6;
        } else if (b + w >= f) {
            return 7;
        } else if (b + w >= g) {
            return 8;
        } else {
            return 9;
        }
    }

    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        int score = 0;
        int b = 0, w = 0;
        if (this.myPiece().side() == WHITE) {
            if (kingOnEdge(board)) {
                return WINNING_VALUE;
            } else if (kingWin(board)) {
                return WILL_WIN_VALUE;
            } else if (kingCaptured(board)) {
                return -INFTY;
            }
        } else if (this.myPiece().side() == BLACK) {
            if (kingCaptured(board)) {
                return -WINNING_VALUE;
            } else if (kingWin(board)) {
                return WILL_WIN_VALUE;
            } else if (kingOnEdge(board)) {
                return WINNING_VALUE;
            }
        }


        for (Piece[] row : board.getContents()) {
            for (Piece piece : row) {
                if (piece == BLACK) {
                    b++;
                } else if (piece == WHITE || piece == KING) {
                    w++;
                }
            }
        }
        b = b * 9;
        w = w * 16;
        int pieces = b + w;

        score += surround(board, b, w);
        score += myStrat(board, b, w);

        int capture = 0;
        HashSet<Square> myPieces = board.pieceLocations(this.myPiece().side());
        for (Square square : myPieces) {
            capture += capturePoints(board, square);
        }
        capture *= 1000;
        score += capture + pieces;
        return score;
    }

    /**
     * Takes a BOARD,B,W and Returns the number
     * of squares surrounded on the board for my pieces.
     */
    private int surround(Board board, int b, int w) {
        int surrounded = 0;
        for (Square[] row : board.getBoard()) {
            for (Square square : row) {
                if (board.get(square) == this.myPiece()
                        && isSurrounded(board, square)) {
                    if (square == board.kingPosition()) {
                        surrounded += 10;
                    } else {
                        surrounded++;
                    }
                }
            }
        }
        if (this.myPiece() == BLACK) {
            surrounded = surrounded * 100 * 2;
        } else if (this.myPiece() == WHITE) {
            surrounded = surrounded * 100 * 2 * -1;
        }

        int throne = thrones(board) * 100 * 2 * -1;

        return throne + surrounded;
    }

    /**
     * Checks if SQUARE is surrounded on this BOARD and returns true.
     */
    private boolean isSurrounded(Board board, Square square) {
        for (int i = 0; i < 4; i++) {
            Square other = square.rookMove(i, 1);
            if (other != null) {
                Piece me = board.get(square);
                Piece oppo = board.get(other);
                if (oppo == me.opponent()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the count of the number of squares
     * that are orthogonally adjacent to the BOARD
     * and occupied by Black Pieces.
     */
    private int thrones(Board board) {
        int count = 0;
        List<Square> arr = new ArrayList<>();
        arr.add(Board.NTHRONE);
        arr.add(Board.WTHRONE);
        arr.add(Board.ETHRONE);
        arr.add(Board.STHRONE);
        for (Square square : arr) {
            if (board.get(square) == BLACK) {
                count++;
            }
        }
        return count;
    }


    /**
     * Returns a Static score for my Piece's Strategy based on the BOARD,B,W.
     */
    private int myStrat(Board board, int b, int w) {
        if (super.myPiece() == BLACK) {
            int score = 0;
            for (Move move : board.legalMoves(BLACK)) {
                if (move.to().adjacent(board.kingPosition())) {
                    score += 1;
                }
            }

            int surrounded = 0;
            int edge = 0;
            for (Square[] row : board.getBoard()) {
                for (Square square : row) {
                    if (board.getContents()[square.row()][square.col()]
                            == this.myPiece() && isSurrounded(board, square)) {
                        surrounded++;
                    }
                    if (board.getContents()[square.row()][square.col()]
                            == this.myPiece() && square.isEdge()) {
                        edge++;
                    }
                }
            }
            surrounded = surrounded * 100 * 2 * -1;
            edge = edge * 100 * 2 * -1;
            score = score * 1000 * 1000;
            return score + edge + surrounded;
        } else {
            int score = 0;
            for (Move move : board.legalMoves(WHITE)) {
                if (move.to().adjacent(board.kingPosition())) {
                    score += 1;
                }
            }
            score *= 1000 * 10;
            return score;
        }
    }

    /**
     * Takes a BOARD, and a SQUARE and returns capturing points.
     */
    private int capturePoints(Board board, Square square) {
        int cap = 0;
        for (Move.MoveList moves : ROOK_MOVES[square.index()]) {
            for (Move move : moves) {
                if (board.isLegal(move)) {
                    Square me = move.to();
                    Piece my = board.get(square);
                    for (Square sq : board.pieceLocations(my.opponent())) {
                        if (me.adjacent(sq)) {
                            cap++;
                        }
                    }
                }
            }
        }
        return cap;
    }

    /**
     * Takes a BOARD and returns whether the king has a clear win.
     */
    private boolean kingWin(Board board) {
        Square king = board.kingPosition();
        if (king == null) {
            return false;
        }
        for (Move.MoveList moves : ROOK_MOVES[king.index()]) {
            for (Move move : moves) {
                if (move.to().isEdge() && board.isLegal(move)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Takes a BOARD and returns whether the king has reached the edge or not.
     */
    private boolean kingOnEdge(Board board) {
        return board.kingPosition() != null && board.kingPosition().isEdge();
    }

    /**
     * Takes a BOARD Returns if the king has been captured.
     */
    private boolean kingCaptured(Board board) {
        return board.kingPosition() == null;
    }


}
