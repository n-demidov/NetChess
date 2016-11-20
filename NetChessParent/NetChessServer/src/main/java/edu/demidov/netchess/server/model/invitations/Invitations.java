package edu.demidov.netchess.server.model.invitations;

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
public class Invitations implements InvitationsObservable
{
    private final List<InvitationsObserver> listeners = new ArrayList<>();
    private final static Logger log = LoggerFactory.getLogger(Invitations.class);

    /*
        Map<trgt, Map<Srce, Invitation>>

        Приглашённый польз-ль содержит список пригласивших его игроков.
        Т.к. входящие приглашения будут запрашиваться чаще, чем исходящие.
        */
    private final Map<User, Map<User, Invitation>> map = new HashMap<>();
    private Date nextLaunch = Calendar.getInstance().getTime();
    private final int invitationsFreqManageMinutes;
    private final int invitationsTtlMinutes;

    public Invitations(final int invitationsFreqManageMinutes, final int invitationsTtlMinutes)
    {
        this.invitationsFreqManageMinutes = invitationsFreqManageMinutes;
        this.invitationsTtlMinutes = invitationsTtlMinutes;
    }

    /**
     * Возвращает true, если targetUser был приглашён sourceUser'ом
     */
    public boolean isInvited(final User sourceUser, final User targetUser)
    {
        if (map.containsKey(targetUser))
        {
            return map.get(targetUser).containsKey(sourceUser);
        }

        return false;
    }

    /**
     * Возвращает список игроков, пригласивших targetUser сыграть
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

        if (targetInvites == null)
        {
            return;
        }

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
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, invitationsFreqManageMinutes);
            nextLaunch = calendar.getTime();

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
                    if (isDateExpired(invitation.getInvitedDate(), invitationsTtlMinutes))
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
    public void addListener(final InvitationsObserver listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final InvitationsObserver listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void notifySubscribers(final User source, final User target)
    {
        log.trace("notifySubscribers source={}, target={}", source, target);

        for (final InvitationsObserver listener : listeners)
        {
            listener.usersAgreed(source, target);
        }
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

    private boolean isDateExpired(final Date startDate, final int minutes)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.MINUTE, minutes);
        final Date expiredDate = calendar.getTime();

        return expiredDate.before(Calendar.getInstance().getTime());
    }
}
