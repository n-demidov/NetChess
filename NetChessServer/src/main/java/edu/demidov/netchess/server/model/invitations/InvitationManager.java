package edu.demidov.netchess.server.model.invitations;

import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.users.User;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс отвечает за управление приглашениями игроков
 * 
 * В сигнатурах методов используется следующее обозначение:
 * User sourceUser - игрок отправивший приглашение
 * User targetUser - игрок получивший приглашение
 * 
 * Метод checkTTLs должен вызываться из игрового цикла. Метод можно вызывать каждую игровую итерацию:
 * Класс сам следит когда нужно запустить обработку внутри себя.
 */
public class InvitationManager implements InvitationObservable
{
    
    private static InvitationManager instance;
    private final List<InvitationObserver> listeners;
    private final static Logger log = LoggerFactory.getLogger(InvitationManager.class);
    
    /*
    Map<trgt, Map<Srce, Invitation>>
    
    Приглашённый польз-ль содержит список пригласивших его игроков.
    Т.к. входящие приглашения будут запрашиваться чаще, чем исходящие.
    */
    private final Map<User, Map<User, Invitation>> map;
    private Date nextLaunch = Calendar.getInstance().getTime();
    
    public static synchronized InvitationManager getInstance()
    {
        if (instance == null) instance = new InvitationManager();
        return instance;
    }

    private InvitationManager()
    {
        map = new HashMap<>();
        listeners = new ArrayList<>();
    }

    /**
     * sourceUser приглашает targetUser сыграть
     * @param sourceUser
     * @param targetUser 
     */
    public void invite(final User sourceUser, final User targetUser)
    {
        log.trace("invite sourceUser={}, targetUser={}", sourceUser, targetUser);
        assert sourceUser != null;
        assert targetUser != null;
        
        // Игрок не может приглашать сам себя
        if (targetUser.equals(sourceUser)) return;
        
        // Получаем приглашения для пользователя
        final Map<User, Invitation> targetInvites = getIncomingInvites(targetUser);
        
        // Если приглашение не было добавлено ранее - добавляем
        if (!targetInvites.containsKey(sourceUser))
        {
            log.trace("invite: adding invitation sourceUser={}, targetUser={}", sourceUser, targetUser);
            targetInvites.put(sourceUser, new Invitation(sourceUser, Calendar.getInstance().getTime()));
        }
        
        // Если и второй игрок добавил первого - начинаем партию
        if (getIncomingInvites(sourceUser).containsKey(targetUser))
        {
            usersAgreed(sourceUser, targetUser);
        }
    }

    /**
     * sourceUser отменяет отосланное игроку targetUser приглашение
     * @param sourceUser
     * @param targetUser 
     */
    public void cancelInvite(final User sourceUser, final User targetUser)
    {
        log.trace("cancelInvite sourceUser={}, targetUser={}", sourceUser, targetUser);
        deleteIncomingInvite(sourceUser, targetUser);
    }

    /**
     * targetUser принимает приглашение сыграть
     * @param sourceUser
     * @param targetUser 
     */
    public void acceptIncomingInvite(final User sourceUser, final User targetUser)
    {
        log.trace("acceptIncomingInvite sourceUser={}, targetUser={}", sourceUser, targetUser);
        // Если предложение еще в силе - начинаем партию
        final Map<User, Invitation> targetInvites = map.get(targetUser);
        if (targetInvites.containsKey(sourceUser))
        {
            usersAgreed(sourceUser, targetUser);
        }
    }

    /**
     * targetUser отменяет приглашение сыграть
     * @param sourceUser
     * @param targetUser 
     */
    public void rejectIncomingInvite(final User sourceUser, final User targetUser)
    {
        log.trace("rejectIncomingInvite sourceUser={}, targetUser={}", sourceUser, targetUser);
        deleteIncomingInvite(sourceUser, targetUser);
    }

    /**
     * Возвращает список игроков, пригласивших targetUser сыграть
     * @param targetUser 
     * @return  
     */
    public Set<User> getIncomingInviters(final User targetUser)
    {
        log.trace("getIncomingInviters targetUser={}", targetUser);
        if (map.containsKey(targetUser))
        {
            return map.get(targetUser).keySet();
        }

        return new HashSet<>();
    }

    /**
     * Возвращает true, если targetUser был приглашён sourceUser'ом
     * @param sourceUser
     * @param targetUser
     * @return 
     */
    public boolean isInvited(final User sourceUser, final User targetUser)
    {
        if (map.containsKey(targetUser)) return map.get(targetUser).containsKey(sourceUser);

        return false;
    }
    
    /**
     * Управляет временем жизни отосланных приглашений.
     * Должен вызываться из игрового цикла. Метод можно вызывать каждую игровую итерацию:
     * Метод запускает обработку раз в n минут.
     */
    public void checkTTLs()
    {
        if (nextLaunch.before(Calendar.getInstance().getTime()))
        {
            log.trace("checkTTLs starts process by time");
            // Устанавливаем время следующей проверки TTL
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, Options.INVITATIONS_FREQ_MANAGE_MINUTES);
            nextLaunch = calendar.getTime();
            
            // Находим разницу между текущим временем и INVITES_TTL_MIN
            calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -Options.INVITATIONS_TTL_MINUTES);
            final Date curDeltaDate = calendar.getTime();
            
            // Для каждого приглашённого пользователя
            final Iterator<Entry<User, Map<User, Invitation>>> it1 = map.entrySet().iterator();
            while (it1.hasNext())
            {
                final Map<User, Invitation> invitesMap = it1.next().getValue();
                
                // Для каждого приглашения
                final Iterator<Entry<User, Invitation>> it2 = invitesMap.entrySet().iterator();
                while (it2.hasNext())
                {
                    final Invitation invitation = it2.next().getValue();
                    // Если TTL приглашения истекло - удаляем его
                    if (invitation.getInvitedDate().before(curDeltaDate))
                    {
                        log.trace("checkTTLs invitation's time expired, invitation={}", invitation);
                        it2.remove();
                    }
                }
                
                // Если приглашений ноль - удаляем приглашённого пользователя из map
                if (invitesMap.isEmpty()) it1.remove();
            }
        }
    }
    
    
    @Override
    public void addListener(final InvitationObserver listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final InvitationObserver listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void notifySubscribers(final User source, final User target)
    {
        log.trace("notifySubscribers source={}, target={}", source, target);
        for (final InvitationObserver listener : listeners)
            listener.usersAgreed(source, target);
    }
    
    /**
     * Возвращает входящие приглашения для targetUser
     * Проверяет был ли добавлен targetUser в map. Если нет - создаёт его.
     * @param targetUser
     * @return 
     */
    private Map<User, Invitation> getIncomingInvites(final User targetUser)
    {
        // Проверяем был ли добавлен targetUser в map. Если нет - создаём
        if (!map.containsKey(targetUser)) map.put(targetUser, new HashMap<>());
        
        return map.get(targetUser);
    }
    
    /**
     * Удаляет sourceUser из списка для targetUser
     * @param sourceUser
     * @param targetUser 
     */
    private void deleteIncomingInvite(final User sourceUser, final User targetUser)
    {
        // Если targetUser'a нет в map - ничего делать не надо
        if (!map.containsKey(targetUser)) return;
        
        // Получаем входящие приглашения, удаляем приглашение от sourceUser
        final Map<User, Invitation> targetInvites = map.get(targetUser);
        targetInvites.remove(sourceUser);
        
        // Если для пользователя нет входящих приглашений - удалим его из map
        if (targetInvites.isEmpty())
        {
            map.remove(targetUser);
        }
    }
    
    /**
     * Вызывается, когда пользователи обоюдно приняли приглашение
     * @param source
     * @param target 
     */
    private void usersAgreed(final User source, final User target)
    {
        log.trace("usersAgreed source={}, target={}", source, target);
        // Удаляем заявки у обоих
        deleteIncomingInvite(source, target);
        deleteIncomingInvite(target, source);
        
        notifySubscribers(source, target);
    }
    
    private static class Invitation
    {
        
        private final User sourceUser;
        private final Date invitedDate;

        public Invitation(final User sourceUser, final Date invitedDate)
        {
            assert sourceUser != null;
            assert invitedDate != null;
            
            this.sourceUser = sourceUser;
            this.invitedDate = invitedDate;
        }

        public User getSourceUser()
        {
            return sourceUser;
        }

        public Date getInvitedDate()
        {
            return invitedDate;
        }

        @Override
        public String toString()
        {
            return "Invitation{" + "sourceUser=" + sourceUser + ", invitedDate=" + invitedDate + '}';
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.sourceUser);
            hash = 61 * hash + Objects.hashCode(this.invitedDate);
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
            final Invitation other = (Invitation) obj;
            if (!Objects.equals(this.sourceUser, other.sourceUser))
            {
                return false;
            }
            if (!Objects.equals(this.invitedDate, other.invitedDate))
            {
                return false;
            }
            return true;
        }
        
    }
    
}
