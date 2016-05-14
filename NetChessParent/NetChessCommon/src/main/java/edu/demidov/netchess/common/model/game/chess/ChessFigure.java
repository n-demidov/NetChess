package edu.demidov.netchess.common.model.game.chess;

import java.io.Serializable;
import java.util.Objects;

public class ChessFigure implements Serializable
{
    
    private ChessColor color;
    private Type type;
    private int movesCount;
    
    public enum Type
    {
        Pawn,       // пешка
        Bishop,     // слон
        Knight,     // конь
        Castle,     // ладья
        Queen,      // ферзь
        King,       // король
    }
    
    public ChessFigure()
    {
    }
    
    public ChessFigure(final ChessColor color, final Type type)
    {
        this.color = color;
        this.type = type;
    }

    public ChessColor getColor()
    {
        return color;
    }

    public void setColor(final ChessColor color)
    {
        this.color = color;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(final Type type)
    {
        this.type = type;
    }

    public int getMovesCount()
    {
        return movesCount;
    }

    public void setMovesCount(final int movesCount)
    {
        this.movesCount = movesCount;
    }

    @Override
    public String toString()
    {
        return "ChessFigure{" + "color=" + color + ", type=" + type + ", movesCount=" + movesCount + '}';
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.color);
        hash = 17 * hash + Objects.hashCode(this.type);
        hash = 17 * hash + this.movesCount;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final ChessFigure other = (ChessFigure) obj;
        if (this.movesCount != other.movesCount)
        {
            return false;
        }
        if (this.color != other.color)
        {
            return false;
        }
        if (this.type != other.type)
        {
            return false;
        }
        return true;
    }

}
