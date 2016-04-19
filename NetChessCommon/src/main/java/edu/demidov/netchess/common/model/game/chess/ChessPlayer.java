package edu.demidov.netchess.common.model.game.chess;

import edu.demidov.netchess.common.model.users.AbstractUser;

public class ChessPlayer extends AbstractUser
{
    
    private ChessColor color;
    private long timeLeft;
    private boolean isOfferedDraw;
    private int accruedScores;

    public ChessPlayer()
    {
        super();
    }
    
    public ChessPlayer(final ChessColor color, final long timeLeft, final String name,
            final int rank, final int wins, final int defeats, final int draws, final int totalTimeOnServer)
    {
        super(name, rank, wins, defeats, draws, totalTimeOnServer);
        assert color != null;
        this.color = color;
        this.timeLeft = timeLeft;
    }

    public ChessColor getColor()
    {
        return color;
    }

    public void setColor(final ChessColor color)
    {
        this.color = color;
    }

    public long getTimeLeft()
    {
        return timeLeft;
    }

    public void setTimeLeft(final long timeLeft)
    {
        this.timeLeft = timeLeft;
    }
    
    public boolean isOfferedDraw()
    {
        return isOfferedDraw;
    }

    public void setOfferedDraw(final boolean isOfferedDraw)
    {
        this.isOfferedDraw = isOfferedDraw;
    }
    
    public int getAccruedScores()
    {
        return accruedScores;
    }

    public void setAccruedScores(final int accruedScores)
    {
        this.accruedScores = accruedScores;
    }

    @Override
    public String toString()
    {
        return "ChessPlayer{" + "color=" + color + ", timeLeft=" + timeLeft + ", isOfferedDraw=" + isOfferedDraw + ", accruedScores=" + accruedScores + '}';
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 67 * hash + this.getName().hashCode();
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
        final ChessPlayer that = (ChessPlayer) obj;
        if (!that.getName().equals(this.getName())) return false;
        return true;
    }
    
}
