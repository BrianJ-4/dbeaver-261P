package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DBExecUtilsRecoverFSMTest extends DBeaverUnitTest {
    // Create DataSource mock
    private DBPDataSource makeDataSource(boolean recoverEnabled, int retryCount) {
        // Mock preference store: represents DBeaver settings/preferences
        DBPPreferenceStore prefs = mock(DBPPreferenceStore.class);

        // When tryExecuteRecover checks if recovery is enabled, return the provided value for that test
        when(prefs.getBoolean(ModelPreferences.EXECUTE_RECOVER_ENABLED)).thenReturn(recoverEnabled);

        if (recoverEnabled) {
            when(prefs.getInt(ModelPreferences.EXECUTE_RECOVER_RETRY_COUNT)).thenReturn(retryCount);
        }

        // Mock DataSourceContainer to return mocked preferences
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);

        // When tryExecuteRecover retrieves the preference store from the container, return mocked prefs
        when(container.getPreferenceStore()).thenReturn(prefs);

        // Mock DataSource and also implement DBPErrorAssistant for error type discovery
        DBPDataSource ds = Mockito.mock(DBPDataSource.class, withSettings().extraInterfaces(DBPErrorAssistant.class));
        
        // Link DataSource to container so that tryExecuteRecover can access preferences
        when(ds.getContainer()).thenReturn(container);

        return ds;
    }

    // Helper method to stub error type returned by error assistant for any exception
    // Allows to force error type for testing recovery logic
    private static void stubErrorType(DBPDataSource ds, DBPErrorAssistant.ErrorType type) {
        DBPErrorAssistant assistantView = (DBPErrorAssistant) ds;

        // No matter what Throwable is passed in, return this type
        when(assistantView.discoverErrorType(any(Throwable.class))).thenReturn(type);
    }

    @Test
    public void successFirstAttempt_returnsTrue() throws Exception {
        // Recovery enabled but does not matter since it should succeed on first try
        DBPDataSource ds = makeDataSource(true, 2);

        // Operation that should succeed without throwing any exception
        @SuppressWarnings("unchecked")  // Silence warning for generic mock
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Run method under test
        boolean ok = DBExecUtils.tryExecuteRecover(monitor, ds, runnable);

        // Should return true on success
        assertTrue(ok);

        // Verify runnable ran exactly 1 time
        verify(runnable, times(1)).run(monitor);
    }

    @Test
    public void interrupted_returnsFalse_noRetry() throws Exception {
        // Recovery enabled but does not matter since InterruptedException should not trigger retries
        DBPDataSource ds = makeDataSource(true, 2);

        // Operation that throws InterruptedException to simulate cancellation/interruption
        @SuppressWarnings("unchecked")
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Force runnable to throw InterruptedException when executed
        doThrow(new InterruptedException("stop")).when(runnable).run(monitor);

        // Run method under test
        boolean ok = DBExecUtils.tryExecuteRecover(monitor, ds, runnable);

        // Should return false on interruption
        assertFalse(ok);

        // Verify runnable ran exactly 1 time
        verify(runnable, times(1)).run(monitor);
    }

    @Test
    public void recoveryDisabled_failureThrows_noRetry() throws Exception {
        // Recovery disabled, so it should not retry
        DBPDataSource ds = makeDataSource(false, 999);

        // Operation that throws a RuntimeException to simulate failure
        @SuppressWarnings("unchecked")
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Force runnable to throw RuntimeException when executed
        doThrow(new RuntimeException("problem")).when(runnable).run(monitor);

        // Run method under test and expect to throw DBException due to failure and no recovery
        assertThrows(DBException.class, () -> DBExecUtils.tryExecuteRecover(monitor, ds, runnable));
        
        // Verify runnable ran exactly 1 time
        verify(runnable, times(1)).run(monitor);
    }

    @Test
    public void recoverableConnectionLost_retryThenSuccess_returnsTrue() throws Exception {
        // Recovery enabled with 1 retry
        DBPDataSource ds = makeDataSource(true, 1);

        // Stub error type to CONNECTION_LOST to trigger recovery logic (Also works for TRANSACTION_ABORTED)
        stubErrorType(ds, DBPErrorAssistant.ErrorType.CONNECTION_LOST);

        // Operation that fails first time and succeeds second time to simulate recoverable error
        @SuppressWarnings("unchecked")
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Simulate cancellation check to allow retry logic to proceed
        when(monitor.isCanceled()).thenReturn(true);

        // First call throws exception and second call succeeds
        // doThrow().doNothing() allows to specify sequence of behaviors for consecutive calls
        doThrow(new RuntimeException("fail once")).doNothing().when(runnable).run(monitor);

        // Run method under test
        boolean ok = DBExecUtils.tryExecuteRecover(monitor, ds, runnable);

        // Should return true since it eventually succeeded
        assertTrue(ok);

        // Verify runnable ran exactly 2 times with first failure and then success
        verify(runnable, times(2)).run(monitor);
    }

    @Test
    public void recoverableConnectionLost_retryExhausted_throws() throws Exception {
        // Recovery enabled with 2 retries
        DBPDataSource ds = makeDataSource(true, 2);

        // Stub error type to CONNECTION_LOST to trigger recovery logic
        stubErrorType(ds, DBPErrorAssistant.ErrorType.CONNECTION_LOST);

        // Operation that always fails to simulate recoverable error that never recovers
        @SuppressWarnings("unchecked")
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Simulate cancellation check to allow retry logic to proceed
        when(monitor.isCanceled()).thenReturn(true);

        // Force runnable to always throw exception when executed to use all retries
        doThrow(new RuntimeException("always fails")).when(runnable).run(monitor);

        // Run method under test and expect to throw DBException after exhausting retries
        assertThrows(DBException.class, () -> DBExecUtils.tryExecuteRecover(monitor, ds, runnable));
        
        // Verify runnable ran exactly 3 times
        verify(runnable, times(3)).run(monitor);
    }

    @Test
    public void nonRecoverableNormalError_doesNotRetry_throws() throws Exception {
        // Recovery enabled with 3 retries
        DBPDataSource ds = makeDataSource(true, 3);

        // Stub error type to NORMAL to indicate non-recoverable error
        stubErrorType(ds, DBPErrorAssistant.ErrorType.NORMAL);

        // Operation that throws a normal error which should not trigger retries
        @SuppressWarnings("unchecked")
        DBRRunnableParametrized<DBRProgressMonitor> runnable = mock(DBRRunnableParametrized.class);

        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);

        // Force runnable to throw RuntimeException with NORMAL error type
        doThrow(new RuntimeException("normal error")).when(runnable).run(monitor);

        // Run method under test and expect to throw DBException immediately without retrying
        assertThrows(DBException.class, () -> DBExecUtils.tryExecuteRecover(monitor, ds, runnable));
        
        // Verify runnable ran exactly 1 time
        verify(runnable, times(1)).run(monitor);
    }
}
