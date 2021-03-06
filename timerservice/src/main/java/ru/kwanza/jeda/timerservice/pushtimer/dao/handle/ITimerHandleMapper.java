package ru.kwanza.jeda.timerservice.pushtimer.dao.handle;

import ru.kwanza.jeda.api.pushtimer.manager.TimerHandle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * @author Michael Yeskov
 */
public interface ITimerHandleMapper {
    public static final  String NOT_CONFIGURED_ERROR = "This handle is not configured to work with timerName = ";
    public static final  String FIX_DB_ERROR = "Unknown timer name. Timer was reconfigured without database migration. Please fix database.";


    public Object toId(TimerHandle timerHandle);
    public TimerHandle fromRs(ResultSet rs, int pz) throws SQLException;
    /*
     * field java.sql.Types  for update
     */
    public int getSQLType();


    public Set<String> getCompatibleTimerNames ();
    /*
     * will be called in synchronized block
     */
    void registerCompatibleTimer(String timerName);
}
