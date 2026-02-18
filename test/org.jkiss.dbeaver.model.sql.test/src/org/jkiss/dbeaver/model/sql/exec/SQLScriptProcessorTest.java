package org.jkiss.dbeaver.model.sql.exec;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScript;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("restriction")
public class SQLScriptProcessorTest {

    // Helper: call private method SQLScriptProcessor.fetchQueryData() via reflection
    private static boolean invokeFetchQueryData(
        SQLScriptProcessor processor,
        DBCSession session,
        DBCResultSet resultSet,
        DBDDataReceiver receiver
    ) throws Exception {
        Method m = SQLScriptProcessor.class.getDeclaredMethod(
            "fetchQueryData",
            DBCSession.class,
            DBCResultSet.class,
            DBDDataReceiver.class
        );
        m.setAccessible(true);
        return (boolean) m.invoke(processor, session, resultSet, receiver);
    }


    // Helper: call private method SQLScriptProcessor.executeSingleQuery() via reflection
    private static boolean invokeExecuteSingleQuery(
        SQLScriptProcessor processor,
        DBCSession session,
        SQLScriptElement element
    ) throws Exception {
        Method m = SQLScriptProcessor.class.getDeclaredMethod(
            "executeSingleQuery",
            DBCSession.class,
            SQLScriptElement.class
        );
        m.setAccessible(true);
        return (boolean) m.invoke(processor, session, element);
    }

    // Helper: call the private method SQLScriptProcessor.executeScript() via reflection
    private static void invokeExecuteScript(
        SQLScriptProcessor processor,
        DBCSession session,
        List<SQLScriptElement> script,
        boolean trackMonitor
    ) throws Exception {
        Method m = SQLScriptProcessor.class.getDeclaredMethod(
            "executeScript",
            DBCSession.class,
            List.class,
            boolean.class
        );
        m.setAccessible(true);
        m.invoke(processor, session, script, trackMonitor);
    }

    @Test
    public void fetchQueryData_nullReceiver_returnsFalse() throws Exception {
        // Create processor with no receiver (null)
        // fetchQueryData should treat this as nothing to fetch into and return false without trying to fetch any rows
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            null,
            log
        );

        DBCSession session = mock(DBCSession.class);
        DBCResultSet rs = mock(DBCResultSet.class);

        assertFalse(invokeFetchQueryData(processor, session, rs, null));
    }

    @Test
    public void fetchQueryData_nullResultSet_returnsFalse() throws Exception {
        // Receiver exists but result set is null
        // fetchQueryData should not try to fetch anything and should return false
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        DBCSession session = mock(DBCSession.class);

        assertFalse(invokeFetchQueryData(processor, session, null, receiver));
    }

    @Test
    public void fetchQueryData_twoRows_fetchesAndReturnsTrue() throws Exception {
        // Receiver and result set are non-null, and result set has rows
        // fetchQueryData should fetch all rows and return true
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        // Statistics is initialized in runScript(), but we are calling fetchQueryData via reflection
        // To avoid null, we initialize it by calling runScript in production… BUT that drags in txn/session.
        // So instead, we set the private field via reflection:
        {
            var f = SQLScriptProcessor.class.getDeclaredField("statistics");
            f.setAccessible(true);
            f.set(processor, new org.jkiss.dbeaver.model.exec.DBCStatistics());
        }

        // Mock session and monitor
        DBCSession session = mock(DBCSession.class);
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        when(session.getProgressMonitor()).thenReturn(monitor);

        DBCResultSet rs = mock(DBCResultSet.class);

        // startFetchWorkflow expects a non-null source statement from the result set so mock that as well
        DBCStatement stmt = mock(DBCStatement.class);
        when(rs.getSourceStatement()).thenReturn(stmt);
        doNothing().when(stmt).autoCloseDependant(any());

        // Simulate a result set with 2 rows.
        when(rs.nextRow()).thenReturn(true, true, false);

        assertTrue(invokeFetchQueryData(processor, session, rs, receiver));

        // Receiver should be asked to fetch exactly 2 rows
        verify(receiver, times(2)).fetchRow(session, rs);
        verify(monitor, atLeastOnce()).subTask(anyString());
    }

    // Unsupported element type branch
    @Test
    public void executeSingleQuery_unsupportedElement_returnsFalse() throws Exception {
        // If element is not SQLQuery, SQLScript, or SQLControlCommand, executeSingleQuery should log an error and return false
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        DBCSession session = mock(DBCSession.class);
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        when(session.getProgressMonitor()).thenReturn(monitor);

        // The element is of unknown type (not SQLQuery, SQLScript, or SQLControlCommand)
        SQLScriptElement unknown = mock(SQLScriptElement.class);

        boolean result = invokeExecuteSingleQuery(processor, session, unknown);
        assertFalse(result);

        verify(log).error(contains("Unsupported SQL element type:"));
    }

    // SQLScript branch in executeSingleQuery
    @Test
    public void executeSingleQuery_sqlScriptElement_delegatesAndReturnsTrue() throws Exception {
        // If the element is SQLScript, executeSingleQuery should delegate to executeScript and return true if it succeeds
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        DBCSession session = mock(DBCSession.class);
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        when(session.getProgressMonitor()).thenReturn(monitor);

        // Stub script with empty elements to avoid complexity of executeScript
        // Still drives the SQLScript branch without needing more setup
        SQLScript script = mock(SQLScript.class);
        when(script.getScriptElements()).thenReturn(Collections.emptyList());

        boolean result = invokeExecuteSingleQuery(processor, session, script);
        assertTrue(result);
    }

    // Canceled monitor early-break in executeScript
    @Test
    public void executeScript_monitorCanceled_breaksImmediately() throws Exception {
        // If user cancels the operation, executeScript should break out of loop immediately
        // Simulate this by having monitor return true for isCanceled() and verify no log messages are produced
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        DBCSession session = mock(DBCSession.class);
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        when(session.getProgressMonitor()).thenReturn(monitor);
        when(monitor.isCanceled()).thenReturn(true);

        SQLScriptElement element = mock(SQLScriptElement.class);

        // If canceled, loop breaks before trying to execute anything, so no log messages should be produced
        invokeExecuteScript(processor, session, List.of(element), true);

        // Verify no log messages were produced since it should break before doing anything
        verifyNoInteractions(log);
    }

    // Setters/getters
    @Test
    public void settersAndGetters_smoke() {
        // Just call all the setters and getters to verify they work without throwing exceptions
        DBCExecutionContext ctx = mock(DBCExecutionContext.class);
        SQLScriptContext scriptCtx = mock(SQLScriptContext.class);
        DBDDataReceiver receiver = mock(DBDDataReceiver.class);
        Log log = mock(Log.class);

        SQLScriptProcessor processor = new SQLScriptProcessor(
            ctx,
            Collections.<SQLScriptElement>emptyList(),
            scriptCtx,
            receiver,
            log
        );

        // Fetch settings getters/setters
        processor.setFetchSize(123);
        processor.setOffset(10);
        processor.setMaxRows(99);
        processor.setFetchFlags(7);

        // Ensure getters return the values we set
        assertNotNull(processor.getCommitType());
        processor.setCommitType(org.jkiss.dbeaver.model.sql.SQLScriptCommitType.NO_COMMIT);
        assertEquals(org.jkiss.dbeaver.model.sql.SQLScriptCommitType.NO_COMMIT, processor.getCommitType());

        // Error handling getters/setters
        assertNotNull(processor.getErrorHandling());
        processor.setErrorHandling(org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling.IGNORE);
        assertEquals(org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling.IGNORE, processor.getErrorHandling());

        // Statistics should be initialized in constructor, so just verify it's non-null
        assertNotNull(processor.getTotalStatistics());
    }
}
