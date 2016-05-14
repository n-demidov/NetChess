package edu.demidov.netchess.common.model.users;

public class UserProfile extends AbstractUser
{
    
    private boolean isInvited;
    private boolean isPlaying;

    public UserProfile()
    {
        super();
    }
    
    public UserProfile(final String name, final int rank,final int wins,
            final int defeats, final int draws, final int totalTimeOnServer, final boolean isInvited, final boolean isPlaying)
    {
        super(name, rank, wins, defeats, draws, totalTimeOnServer);
        this.isInvited = isInvited;
        this.isPlaying = isPlaying;
    }
    
    public boolean isInvited()
    {
        return isInvited;
    }

    public void setInvited(final boolean isInvited)
    {
        this.isInvited = isInvited;
    }

    public boolean isPlaying()
    {
        return isPlaying;
    }

    public void setPlaying(final boolean isPlaying)
    {
        this.isPlaying = isPlaying;
    }

    @Override
    public String toString()
    {
        return "UserProfile{" +
                "name=" + getName() +
                ", rank=" + getRank() +
                ", wins=" + getWins() +
                ", defeats=" + getDefeats() +
                ", draws=" + getDraws() +
                ", isInvited=" + isInvited +
                ", isPlaying=" + isPlaying +
                ", totalTimeOnServer=" + this.getTotalTimeOnServer() +
                '}';
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 59 * hash + this.getName().hashCode();
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
        final UserProfile that = (UserProfile) obj;
        if (!that.getName().equals(this.getName())) return false;
        return true;
    }

}
