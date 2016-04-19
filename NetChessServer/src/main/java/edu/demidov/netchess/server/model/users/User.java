package edu.demidov.netchess.server.model.users;

import edu.demidov.netchess.common.model.users.AbstractUser;

public class User extends AbstractUser
{
    
    private String passwordHash;

    public User()
    {
        super();
    }

    public User(final String name, final String passwordHash, final int rank,
            final int wins, final int defeats, final int draws, final int totalTimeOnServer)
    {
        super(name, rank, wins, defeats, draws, totalTimeOnServer);
        this.passwordHash = passwordHash;
    }

    public User(final String name, final String passwordHash, final int rank)
    {
        this(name, passwordHash, rank, 0, 0, 0, 0);
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public void setPasswordHash(final String passwordHash)
    {
        this.passwordHash = passwordHash;
    }
    
    @Override
    public String toString()
    {
        return "User{" +
                "name=" + this.getName() +
                ", rank=" + this.getRank() +
                ", wins=" + this.getWins() +
                ", defeats=" + this.getDefeats() +
                ", draws=" + this.getDraws() +
                ", totalTimeOnServer=" + this.getTotalTimeOnServer() +
                //", passwordHash=" + passwordHash +
                '}';
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.getName().hashCode();
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
        final User that = (User) obj;
        if (!that.getName().equals(this.getName())) return false;
        return true;
    }

}
