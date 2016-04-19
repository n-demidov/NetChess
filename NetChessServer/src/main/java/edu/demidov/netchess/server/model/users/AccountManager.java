package edu.demidov.netchess.server.model.users;

import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.exceptions.LoginIsBanException;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserException;
import edu.demidov.netchess.server.model.exceptions.UserCreationException;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.utils.EncryptAlgorithm;
import edu.demidov.netchess.utils.XmlSerialization;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс отвечает за хранение всех пользователей,
 * а также за процедуру создания и логина аккаунта
 */
public class AccountManager
{
    
    private static final int MIN_NAME_LENGTH = 3, MAX_NAME_LENGTH = 15;
    private static final String FILE_NAME = "data/registered_users.xml";
    
    private static final String NAME_LENGTH_EXCEPTION
            = String.format("Длина логина должна быть в диапазоне %d-%d символов",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH);
    private static final String NO_SUCH_USER_EXCEPTION
            = "Пользователя с логином '%s' не существует";
    private static final String NAME_ALREADY_USE_EXCEPTION
            = "Пользователь с таким логином уже существует";
    private static final String LOGIN_OR_NAME_EXCEPTION = "Ошибка в логине или пароле";
    
    // Бан по логину
    private static final String BANNED_LOGINS_FILE = "data/banned_logins.txt",
            ERROR_WHILE_PARSING_FILE_EXCEPTION
            = String.format("Возникла ошибка при считывании файла %s", BANNED_LOGINS_FILE);
    private static final String LOGIN_BANNED_EXCEPTION = "Логин '%s' внесён в чёрный список";
    
    private final XmlSerialization xmlSerialization;
    private final UtilUserList utilUserList;
    private final EncryptAlgorithm encryptAlgorithm;
    private static AccountManager instance;
    private final static Logger log = LoggerFactory.getLogger(AccountManager.class);
    
    public static synchronized AccountManager getInstance()
    {
        if (instance == null) instance = new AccountManager();
        return instance;
    }
    
    /**
     * Класс для сохранения\загрузки в xml
     */
    public static class UtilUserList implements Serializable
    {
        private Map<String, User> users = new HashMap<>();

        public UtilUserList()
        {
        }
        
        public Map<String, User> getUsers()
        {
            return users;
        }

        public void setUsers(final Map<String, User> users)
        {
            this.users = users;
        }
    }

    private AccountManager()
    {
        UtilUserList tempUtilUserList;
        xmlSerialization = XmlSerialization.getInstance();
        
        // Пробует считать пользователей из xml-файла, либо создаёт пустой список
        try
        {
            tempUtilUserList = (UtilUserList) xmlSerialization.read(FILE_NAME);
        } catch (final FileNotFoundException ex)
        {
            // Если файл не найден - создаём пустой список пользователей
            tempUtilUserList = new UtilUserList();
        }
        
        this.utilUserList = tempUtilUserList;
        encryptAlgorithm = EncryptAlgorithm.getInstance();
    }
    
    /**
     * Создаёт аккаунт пользователя
     * @param userName
     * @param password
     * @return 
     * @throws FileNotFoundException 
     * @throws UserCreationException 
     * @throws java.security.NoSuchAlgorithmException 
     */
    public User createUser(final String userName, final String password)
            throws FileNotFoundException, UserCreationException, NoSuchAlgorithmException
    {
        log.trace("createUser userName={}", userName);
        try
        {
            // Проверяем имя на валидность
            checkNewNameForCreateUser(userName);
            
            // Проверяем бан по логину
            checkBannedLogins(userName);
            
            // Хешируем пароль
            final String passwordHash = getHashForPassword(password);
            
            // Создаем пользователя
            final User user = new User(userName, passwordHash, Options.USER_RANK_DEFAULT);
            
            // Добавляем в список пользователей
            this.utilUserList.getUsers().put(userName, user);
            
            // Сохраняем список пользователей в xml-файл
            xmlSerialization.write(this.utilUserList, FILE_NAME);
            
            return user;
        } catch (final LoginIsBanException ex)
        {
            log.trace("createUser: login banned, userName={}", userName);
            throw new UserCreationException(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Проверяет возможность\правильность логина логина.
     * В случае успеха возвращает объект пользователя
     * В случае неудачи выбрасывает исключения.
     * @param userName
     * @param password
     * @return
     * @throws UserLoginException 
     * @throws java.security.NoSuchAlgorithmException 
     */
    public User loginUser(final String userName, final String password)
            throws UserLoginException, NoSuchAlgorithmException
    {
        log.trace("loginUser userName={}", userName);
        
        try
        {
            // Проверяем есть ли такое имя
            if (!utilUserList.getUsers().containsKey(userName))
            {
                log.trace("loginUser NO_SUCH_USER_EXCEPTION");
                throw new UserLoginException(String.format(
                        NO_SUCH_USER_EXCEPTION, userName));
            }
            
            // Проверяем бан по логину
            checkBannedLogins(userName);

            // Хешируем пароль
            final String passwordHash = getHashForPassword(password);

            // Проверяем логин, пароль
            final User user = utilUserList.getUsers().get(userName);
            if (!(userName.equals(user.getName()) && passwordHash.equals(user.getPasswordHash())))
            {
                log.trace("loginUser {}", LOGIN_OR_NAME_EXCEPTION);
                throw new UserLoginException(LOGIN_OR_NAME_EXCEPTION);
            }

            return user;
        } catch (final LoginIsBanException ex)
        {
            log.trace("loginUser: login banned, userName={}", userName);
            throw new UserLoginException(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Возвращает объект пользователя по имени
     * @param userName
     * @return
     * @throws NoSuchUserException 
     */
    public User getUser(final String userName) throws NoSuchUserException
    {
        // Проверяем есть ли такое имя
        if (!utilUserList.getUsers().containsKey(userName)) throw new NoSuchUserException();
        
        return utilUserList.getUsers().get(userName);
    }
    
    /**
     * Сохраняет данные о пользователе
     * @param user 
     * @throws FileNotFoundException 
     * @throws UserLoginException 
     */
    public void updateUser(final User user) throws FileNotFoundException, UserLoginException
    {
        log.trace("updateUser user={}", user);
        final String userName = user.getName();
        
        // Проверяем есть ли такое имя
        if (!utilUserList.getUsers().containsKey(userName))
        {
            log.trace("loginUser NO_SUCH_USER_EXCEPTION");
            throw new UserLoginException(String.format(
                    NO_SUCH_USER_EXCEPTION,
                    userName));
        }
        
        // Обновляем объект в списке пользователей
        this.utilUserList.getUsers().put(userName, user);
        
        // Сохраняем список пользователей в xml-файл
        xmlSerialization.write(this.utilUserList, FILE_NAME);
    }
    
    // Хеширует пароль
    private String getHashForPassword(final String password) throws NoSuchAlgorithmException
    {
        return encryptAlgorithm.getHashCodeFromString(
                EncryptAlgorithm.SHA512_ALGORITHM, password);
    }
    
    /**
     * Проверяет имя аккаунта на валидность
     * @param userName
     * @throws UserCreationException 
     */
    private void checkNewNameForCreateUser(final String userName) throws UserCreationException
    {
        log.trace("checkNewNameForCreateUser userName={}", userName);
        
        // Проверяем длину логина
        final int nameLength = userName.length();
        if (nameLength < MIN_NAME_LENGTH || nameLength > MAX_NAME_LENGTH)
        {
            log.trace("checkNewNameForCreateUser {}", NAME_LENGTH_EXCEPTION);
            throw new UserCreationException(NAME_LENGTH_EXCEPTION);
        }

        // Проверяем не занят ли логин
        if (utilUserList.getUsers().containsKey(userName))
        {
            log.trace("checkNewNameForCreateUser {}", NAME_ALREADY_USE_EXCEPTION);
            throw new UserCreationException(NAME_ALREADY_USE_EXCEPTION);
        }
    }
    
    // Проверяет не внесён ли логин в чёрный список
    private void checkBannedLogins(final String name) throws LoginIsBanException
    {
        log.trace("checkBannedLogins name={}", name);
        
        try (final BufferedReader br = new BufferedReader(new FileReader(BANNED_LOGINS_FILE)))
        {
            // Считываем и обрабатываем строки из файла
            String s;
            while ((s = br.readLine()) != null)
            {
                s = s.trim();
                if (s.isEmpty()) continue;

                // Непосредственно проверяем логин в списке банов
                if (name.equals(s))
                {
                    log.trace("checkBannedLogins name={}, LOGIN_BANNED_EXCEPTION", name);
                    throw new LoginIsBanException(String.format(
                            LOGIN_BANNED_EXCEPTION, name));
                }
            }
        } catch (final IOException ex)
        {
            log.error("checkBannedLogins: name={}, {}", name, ERROR_WHILE_PARSING_FILE_EXCEPTION, ex);
        }
    }
    
}
