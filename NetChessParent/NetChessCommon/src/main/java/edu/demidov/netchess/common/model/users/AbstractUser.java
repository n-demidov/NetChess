package edu.demidov.netchess.common.model.users;

import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractUser implements Serializable
{
    
    private String name;                // Уникальный логин
    private int rank;
    private int wins;
    private int defeats;
    private int draws;
    private int totalTimeOnServer;      // Общее время, проведённое на сервере, в секундах

    public AbstractUser() {}
    
    public AbstractUser(final String name, final int rank, final int wins,
            final int defeats, final int draws, final int totalTimeOnServer)
    {
        this.name = name;
        this.rank = rank;
        this.wins = wins;
        this.defeats = defeats;
        this.draws = draws;
        this.totalTimeOnServer = totalTimeOnServer;
    }

    //<editor-fold defaultstate="collapsed" desc="getters-and-setters">
    public String getName()
    {
        return name;
    }
    
    public void setName(final String name)
    {
        this.name = name;
    }
    
    public int getRank()
    {
        return rank;
    }
    
    public void setRank(final int rank)
    {
        this.rank = rank;
    }
    
    public int getWins()
    {
        return wins;
    }
    
    public void setWins(final int wins)
    {
        this.wins = wins;
    }
    
    public int getDefeats()
    {
        return defeats;
    }
    
    public void setDefeats(final int defeats)
    {
        this.defeats = defeats;
    }
    
    public int getDraws()
    {
        return draws;
    }
    
    public void setDraws(final int draws)
    {
        this.draws = draws;
    }
    
    public int getTotalTimeOnServer()
    {
        return totalTimeOnServer;
    }
    
    public void setTotalTimeOnServer(final int totalTimeOnServer)
    {
        this.totalTimeOnServer = totalTimeOnServer;
    }
//</editor-fold>
    
    /**
     * Добавляет время проведённое на сервере, в секундах
     * @param addingSeconds 
     */
    public void addTotalTimeOnServer(final long addingSeconds)
    {
        this.totalTimeOnServer += addingSeconds;
    }

    @Override
    public String toString()
    {
        return "AbstractUser{" + "name=" + name + ", rank=" + rank + ", wins=" + wins + ", defeats=" + defeats + ", draws=" + draws + ", totalTimeOnServer=" + totalTimeOnServer + '}';
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.name);
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
        final AbstractUser other = (AbstractUser) obj;
        if (!Objects.equals(this.name, other.name))
        {
            return false;
        }
        return true;
    }

}
