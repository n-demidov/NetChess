package edu.demidov.netchess.common.model.game.chess;

import edu.demidov.netchess.utils.Point;
import java.io.Serializable;
import java.util.Arrays;

public class ChessAction implements Serializable
{
    
    private Type type;
    private Point[] points;
    private ChessFigure.Type chooseFigureType;

    public enum Type
    {
        
        Move,
        Surrender,                  // игрок сдался
        OfferedDraw,                // игрок предложил ничью
        ChooseFigureInsteadPawn     // игрок выбрал фигуру (когда пешка долшла до конца доски)
        
    }

    public ChessAction()
    {
    }
    
    public ChessAction(final Type type)
    {
        this.type = type;
    }
    
    public ChessAction(final Type type, final Point[] points)
    {
        this.type = type;
        this.points = points;
    }

    public ChessAction(final Type type, final ChessFigure.Type chooseFigureType)
    {
        this.type = type;
        this.chooseFigureType = chooseFigureType;
    }
    
    public Type getType()
    {
        return type;
    }

    public void setType(final Type type)
    {
        this.type = type;
    }

    public Point[] getPoints()
    {
        return points;
    }

    public void setPoints(final Point[] points)
    {
        this.points = points;
    }
    
    public ChessFigure.Type getChooseFigureType()
    {
        return chooseFigureType;
    }

    public void setChooseFigureType(final ChessFigure.Type chooseFigureType)
    {
        this.chooseFigureType = chooseFigureType;
    }

    @Override
    public String toString()
    {
        return "ChessAction{" + "type=" + type + ", points=" + Arrays.toString(points) + ", chooseFigureType=" + chooseFigureType + '}';
    }

}
